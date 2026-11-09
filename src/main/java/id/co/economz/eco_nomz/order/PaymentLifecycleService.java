package id.co.economz.eco_nomz.order;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import id.co.economz.eco_nomz.offer.FoodOffer;
import id.co.economz.eco_nomz.offer.FoodOfferRepository;
import id.co.economz.eco_nomz.payment.Payment;
import id.co.economz.eco_nomz.payment.PaymentRepository;
import id.co.economz.eco_nomz.payment.PaymentStatus;
import id.co.economz.eco_nomz.user.User;
import id.co.economz.eco_nomz.user.UserRepository;
import id.co.economz.eco_nomz.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentLifecycleService {
    public static final Duration PAYMENT_WINDOW = Duration.ofMinutes(17);

    private static final ZoneId MARKETPLACE_ZONE = ZoneId.of("Asia/Jakarta");
    private final CustomerOrderRepository orderRepository;
    private final FoodOfferRepository offerRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public PaymentLifecycleService(
            CustomerOrderRepository orderRepository,
            FoodOfferRepository offerRepository,
            PaymentRepository paymentRepository,
            UserRepository userRepository
    ) {
        this.orderRepository = orderRepository;
        this.offerRepository = offerRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CustomerOrder verifyPickup(String ownerEmail, String pickupCode) {
        requireRole(ownerEmail, UserRole.RESTAURANT_OWNER);
        CustomerOrder order = orderRepository.findByPickupCode(pickupCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!order.getFoodOffer().getRestaurant().getOwner().getEmail().equalsIgnoreCase(ownerEmail)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Payment payment = paymentRepository.lockByOrderId(order.getId())
                .orElseThrow(() -> new IllegalStateException("Reservation payment was not found."));
        if (order.getStatus() != OrderStatus.CONFIRMED || payment.getStatus() != PaymentStatus.SUCCEEDED) {
            throw OrderConflictException.localized(
                    "orders.conflict.notReadyForPickup",
                    "This reservation is not paid and ready for pickup."
            );
        }
        OffsetDateTime now = OffsetDateTime.now(MARKETPLACE_ZONE);
        if (now.isBefore(order.getFoodOffer().getPickupStart())
                || !now.isBefore(order.getFoodOffer().getPickupEnd())) {
            throw OrderConflictException.localized(
                    "orders.conflict.outsidePickupWindow",
                    "Pickup can only be verified during the pickup window."
            );
        }
        order.completePickup();
        return order;
    }

    @Scheduled(fixedDelayString = "${app.payment.expiry-scan-ms:60000}")
    @Transactional
    public void expirePendingPayments() {
        Instant cutoff = Instant.now().minus(PAYMENT_WINDOW);
        for (Long orderId : paymentRepository.findOrderIdsByStatusAndCreatedAtBefore(PaymentStatus.PENDING, cutoff)) {
            orderRepository.findById(orderId).ifPresent(order -> {
                Payment payment = paymentRepository.lockByOrderId(orderId).orElse(null);
                if (payment != null
                        && payment.getStatus() == PaymentStatus.PENDING
                        && order.getStatus() == OrderStatus.PENDING_PAYMENT
                        && isExpired(payment)) {
                    expire(order, payment);
                }
            });
        }
    }

    private void expire(CustomerOrder order, Payment payment) {
        payment.expire();
        order.expire();
        releaseInventory(order);
    }

    private void releaseInventory(CustomerOrder order) {
        FoodOffer offer = offerRepository.lockById(order.getFoodOffer().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        offer.restoreReservedQuantity(order.getQuantity(), OffsetDateTime.now(MARKETPLACE_ZONE));
    }

    private boolean isExpired(Payment payment) {
        return payment.getCreatedAt().isBefore(Instant.now().minus(PAYMENT_WINDOW));
    }

    private User requireRole(String email, UserRole role) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (user.getRole() != role) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return user;
    }
}
