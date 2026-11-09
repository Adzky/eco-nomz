package id.co.economz.eco_nomz.payment;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import id.co.economz.eco_nomz.order.CustomerOrder;
import id.co.economz.eco_nomz.order.CustomerOrderRepository;
import id.co.economz.eco_nomz.order.OrderConflictException;
import id.co.economz.eco_nomz.order.OrderStatus;
import id.co.economz.eco_nomz.user.User;
import id.co.economz.eco_nomz.user.UserRepository;
import id.co.economz.eco_nomz.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MidtransCheckoutService {
    private static final Duration CHECKOUT_WINDOW = Duration.ofMinutes(15);

    private final CustomerOrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final MidtransGateway midtransGateway;
    private final MidtransProperties properties;

    public MidtransCheckoutService(
            CustomerOrderRepository orderRepository,
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            MidtransGateway midtransGateway,
            MidtransProperties properties
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.midtransGateway = midtransGateway;
        this.properties = properties;
    }

    @Transactional
    public String createCheckout(String customerEmail, Long orderId) {
        User customer = userRepository.findByEmailIgnoreCase(customerEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (customer.getRole() != UserRole.CUSTOMER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        CustomerOrder order = orderRepository.findByIdAndCustomerEmailIgnoreCase(orderId, customer.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var payment = paymentRepository.lockByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("Reservation payment was not found."));
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT || payment.getStatus() != PaymentStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This reservation is no longer payable.");
        }
        if (payment.getCreatedAt() != null
                && !payment.getCreatedAt().plus(CHECKOUT_WINDOW).isAfter(Instant.now())) {
            throw OrderConflictException.localized(
                    "orders.conflict.paymentWindowExpired",
                    "The 15-minute payment window has expired."
            );
        }
        if (payment.hasCheckoutUrl()) {
            return payment.getCheckoutUrl();
        }

        MidtransSnapResponse response = midtransGateway.createTransaction(payment);
        validateRedirectUrl(response.redirectUrl());
        payment.completeCheckout(response.redirectUrl(), null);
        return response.redirectUrl();
    }

    private void validateRedirectUrl(String redirectUrl) {
        try {
            URI redirect = URI.create(redirectUrl);
            URI base = URI.create(properties.getBaseUrl());
            if (!"https".equalsIgnoreCase(redirect.getScheme())
                    || !base.getHost().equalsIgnoreCase(redirect.getHost())
                    || redirect.getUserInfo() != null) {
                throw new MidtransIntegrationException("Midtrans returned an unexpected checkout URL.");
            }
        } catch (IllegalArgumentException exception) {
            throw new MidtransIntegrationException("Midtrans returned an invalid checkout URL.", exception);
        }
    }
}
