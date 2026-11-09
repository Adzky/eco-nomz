package id.co.economz.eco_nomz.payment;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import id.co.economz.eco_nomz.offer.FoodOffer;
import id.co.economz.eco_nomz.offer.FoodOfferRepository;
import id.co.economz.eco_nomz.order.CustomerOrder;
import id.co.economz.eco_nomz.order.CustomerOrderRepository;
import id.co.economz.eco_nomz.order.OrderStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MidtransNotificationService {
    private final MidtransProperties properties;
    private final PaymentRepository paymentRepository;
    private final CustomerOrderRepository orderRepository;
    private final FoodOfferRepository offerRepository;

    public MidtransNotificationService(
            MidtransProperties properties,
            PaymentRepository paymentRepository,
            CustomerOrderRepository orderRepository,
            FoodOfferRepository offerRepository
    ) {
        this.properties = properties;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.offerRepository = offerRepository;
    }

    @Transactional
    public void process(MidtransNotification notification) {
        if (!isAuthentic(notification)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid payment notification signature.");
        }
        if (!"IDR".equalsIgnoreCase(notification.currency())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported payment currency.");
        }
        BigDecimal grossAmount;
        try {
            grossAmount = new BigDecimal(notification.grossAmount());
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment amount.", exception);
        }
        Payment payment = paymentRepository.findByProviderReferenceForOrderLookup(notification.orderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        CustomerOrder order = orderRepository.findById(payment.getOrder().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        payment = paymentRepository.lockByOrderId(order.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!notification.orderId().equals(payment.getProviderReference())
                || grossAmount.compareTo(payment.getAmount()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment notification does not match the order.");
        }
        payment.applyProviderTransactionId(notification.transactionId());
        applyStatus(order, payment, notification);
    }

    boolean isAuthentic(MidtransNotification notification) {
        if (notification == null
                || isBlank(notification.orderId())
                || isBlank(notification.statusCode())
                || isBlank(notification.grossAmount())
                || isBlank(notification.signatureKey())) {
            return false;
        }
        properties.requireServerKey();
        String message = notification.orderId()
                + notification.statusCode()
                + notification.grossAmount()
                + properties.getServerKey();
        try {
            byte[] expected = HexFormat.of().parseHex(sha512(message));
            byte[] supplied = HexFormat.of().parseHex(notification.signatureKey());
            return MessageDigest.isEqual(expected, supplied);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private void applyStatus(CustomerOrder order, Payment payment, MidtransNotification notification) {
        if (notification.transactionStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment status is missing.");
        }
        String status = notification.transactionStatus().toLowerCase(Locale.ROOT);
        if ("settlement".equals(status)
                || ("capture".equals(status) && "accept".equalsIgnoreCase(notification.fraudStatus()))) {
            if (payment.getStatus() == PaymentStatus.SUCCEEDED
                    && order.getStatus() == OrderStatus.CONFIRMED) {
                return;
            }
            if (!"200".equals(notification.statusCode())) {
                return;
            }
            if (payment.getStatus() == PaymentStatus.SUCCEEDED
                    && order.getStatus() == OrderStatus.CONFIRMED) {
                return;
            }
            if (order.getStatus() != OrderStatus.PENDING_PAYMENT
                    || payment.getStatus() == PaymentStatus.EXPIRED) {
                if (payment.getStatus() == PaymentStatus.PENDING
                        || payment.getStatus() == PaymentStatus.EXPIRED) {
                    payment.requireManualReview();
                }
                return;
            }
            if (payment.getStatus() != PaymentStatus.PENDING) {
                return;
            }
            payment.succeed();
            order.confirm();
            return;
        }

        if ("expire".equals(status) || "cancel".equals(status) || "deny".equals(status)
                || "failure".equals(status)) {
            if (payment.getStatus() != PaymentStatus.PENDING
                    || order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                return;
            }
            if ("failure".equals(status) || "deny".equals(status)) {
                payment.fail();
            } else {
                payment.expire();
            }
            order.expire();
            FoodOffer offer = offerRepository.lockById(order.getFoodOffer().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            offer.restoreReservedQuantity(order.getQuantity(), java.time.OffsetDateTime.now(
                    java.time.ZoneId.of("Asia/Jakarta")
            ));
        }
    }

    private String sha512(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-512")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-512 is unavailable.", exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
