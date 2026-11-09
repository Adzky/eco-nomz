package id.co.economz.eco_nomz.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Optional;
import id.co.economz.eco_nomz.offer.FoodOffer;
import id.co.economz.eco_nomz.offer.FoodOfferRepository;
import id.co.economz.eco_nomz.offer.OfferStatus;
import id.co.economz.eco_nomz.order.CustomerOrder;
import id.co.economz.eco_nomz.order.CustomerOrderRepository;
import id.co.economz.eco_nomz.order.OrderStatus;
import id.co.economz.eco_nomz.restaurant.Restaurant;
import id.co.economz.eco_nomz.user.User;
import id.co.economz.eco_nomz.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MidtransNotificationServiceTests {
    private static final String SERVER_KEY = "sandbox-test-server-key";
    private static final String MERCHANT_ORDER_ID = "ECO-test-order";

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CustomerOrderRepository orderRepository;

    @Mock
    private FoodOfferRepository offerRepository;

    private MidtransProperties properties;
    private MidtransNotificationService service;
    private CustomerOrder order;
    private Payment payment;

    @BeforeEach
    void setUp() {
        properties = new MidtransProperties();
        properties.setServerKey(SERVER_KEY);
        service = new MidtransNotificationService(properties, paymentRepository, orderRepository, offerRepository);

        User customer = new User("Customer", "customer@example.com", "hash", UserRole.CUSTOMER);
        User owner = new User("Owner", "owner@example.com", "hash", UserRole.RESTAURANT_OWNER);
        OffsetDateTime pickupStart = OffsetDateTime.now(ZoneId.of("Asia/Jakarta")).plusHours(2);
        FoodOffer offer = new FoodOffer(
                new Restaurant(owner, "Cafe", null, "Jakarta", null, null, true),
                "Food bag", null, new BigDecimal("50000"), new BigDecimal("25000"), 2,
                pickupStart, pickupStart.plusHours(2), OfferStatus.PUBLISHED
        );
        offer.reserveQuantity(1);
        ReflectionTestUtils.setField(offer, "id", 7L);
        order = new CustomerOrder(
                customer, offer, 1, new BigDecimal("25000.00"),
                new BigDecimal("25000.00"), OrderStatus.PENDING_PAYMENT
        );
        ReflectionTestUtils.setField(order, "id", 15L);
        order.assignPickupCode("pickup-test-code");
        payment = new Payment(order, MERCHANT_ORDER_ID, new BigDecimal("25000.00"), PaymentStatus.PENDING);

        lenient().when(paymentRepository.findByProviderReferenceForOrderLookup(MERCHANT_ORDER_ID))
                .thenReturn(Optional.of(payment));
        lenient().when(orderRepository.findById(15L)).thenReturn(Optional.of(order));
        lenient().when(paymentRepository.lockByOrderId(15L)).thenReturn(Optional.of(payment));
    }

    @Test
    void verifiedSettlementConfirmsOrderAndDuplicateNotificationIsIdempotent() {
        MidtransNotification notification = notification("settlement", "accept", "200", "25000.00");

        service.process(notification);
        service.process(notification);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(payment.getProviderTransactionId()).isEqualTo("transaction-123");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void invalidSignatureIsRejectedBeforeDatabaseLookup() {
        MidtransNotification notification = new MidtransNotification(
                MERCHANT_ORDER_ID, "200", "25000.00", "bad-signature",
                "settlement", "accept", "transaction-123", "IDR"
        );

        assertThatThrownBy(() -> service.process(notification))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void wrongAmountIsRejected() {
        MidtransNotification notification = notification("settlement", "accept", "200", "25001.00");

        assertThatThrownBy(() -> service.process(notification))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void verifiedExpiryMarksOrderExpiredAndRestoresInventory() {
        MidtransNotification notification = notification("expire", null, "407", "25000.00");
        when(offerRepository.lockById(7L)).thenReturn(Optional.of(order.getFoodOffer()));

        service.process(notification);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(order.getFoodOffer().getQuantityAvailable()).isEqualTo(2);
    }

    @Test
    void paidNotificationForAlreadyExpiredReservationRequiresManualReview() {
        order.expire();
        payment.expire();

        service.process(notification("settlement", "accept", "200", "25000.00"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REVIEW_REQUIRED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXPIRED);
    }

    private MidtransNotification notification(
            String transactionStatus,
            String fraudStatus,
            String statusCode,
            String grossAmount
    ) {
        String signature = sha512(MERCHANT_ORDER_ID + statusCode + grossAmount + SERVER_KEY);
        return new MidtransNotification(
                MERCHANT_ORDER_ID,
                statusCode,
                grossAmount,
                signature,
                transactionStatus,
                fraudStatus,
                "transaction-123",
                "IDR"
        );
    }

    private String sha512(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-512")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
