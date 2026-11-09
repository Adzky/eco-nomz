package id.co.economz.eco_nomz.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import id.co.economz.eco_nomz.offer.FoodOffer;
import id.co.economz.eco_nomz.offer.OfferStatus;
import id.co.economz.eco_nomz.order.CustomerOrder;
import id.co.economz.eco_nomz.order.CustomerOrderRepository;
import id.co.economz.eco_nomz.order.OrderStatus;
import id.co.economz.eco_nomz.restaurant.Restaurant;
import id.co.economz.eco_nomz.user.User;
import id.co.economz.eco_nomz.user.UserRepository;
import id.co.economz.eco_nomz.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MidtransCheckoutServiceTests {
    private static final String CUSTOMER_EMAIL = "customer@example.com";
    private static final String REDIRECT_URL = "https://app.sandbox.midtrans.com/snap/v2/vtweb/test-token";

    @Mock
    private CustomerOrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MidtransGateway gateway;

    private MidtransCheckoutService checkoutService;
    private CustomerOrder order;
    private Payment payment;

    @BeforeEach
    void setUp() {
        MidtransProperties properties = new MidtransProperties();
        checkoutService = new MidtransCheckoutService(
                orderRepository, paymentRepository, userRepository, gateway, properties
        );
        User customer = new User("Customer", CUSTOMER_EMAIL, "hash", UserRole.CUSTOMER);
        User owner = new User("Owner", "owner@example.com", "hash", UserRole.RESTAURANT_OWNER);
        OffsetDateTime pickupStart = OffsetDateTime.now(ZoneId.of("Asia/Jakarta")).plusHours(2);
        FoodOffer offer = new FoodOffer(
                new Restaurant(owner, "Cafe", null, "Jakarta", null, null, true),
                "Food bag", null, new BigDecimal("50000"), new BigDecimal("25000"), 2,
                pickupStart, pickupStart.plusHours(2), OfferStatus.PUBLISHED
        );
        ReflectionTestUtils.setField(offer, "id", 7L);
        order = new CustomerOrder(
                customer, offer, 1, new BigDecimal("25000.00"),
                new BigDecimal("25000.00"), OrderStatus.PENDING_PAYMENT
        );
        ReflectionTestUtils.setField(order, "id", 15L);
        payment = new Payment(order, "ECO-test-order", new BigDecimal("25000.00"), PaymentStatus.PENDING);

        when(userRepository.findByEmailIgnoreCase(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(orderRepository.findByIdAndCustomerEmailIgnoreCase(15L, CUSTOMER_EMAIL))
                .thenReturn(Optional.of(order));
        when(paymentRepository.lockByOrderId(15L)).thenReturn(Optional.of(payment));
    }

    @Test
    void createsAndPersistsSandboxCheckoutUrl() {
        when(gateway.createTransaction(payment)).thenReturn(new MidtransSnapResponse("token", REDIRECT_URL));

        String checkoutUrl = checkoutService.createCheckout(CUSTOMER_EMAIL, 15L);

        assertThat(checkoutUrl).isEqualTo(REDIRECT_URL);
        assertThat(payment.getCheckoutUrl()).isEqualTo(REDIRECT_URL);
        verify(gateway).createTransaction(payment);
    }

    @Test
    void reusesExistingCheckoutInsteadOfCreatingAnotherMidtransTransaction() {
        payment.completeCheckout(REDIRECT_URL, null);

        assertThat(checkoutService.createCheckout(CUSTOMER_EMAIL, 15L)).isEqualTo(REDIRECT_URL);
    }

    @Test
    void rejectsUntrustedCheckoutRedirectUrl() {
        when(gateway.createTransaction(payment))
                .thenReturn(new MidtransSnapResponse("token", "https://attacker.example/checkout"));

        assertThatThrownBy(() -> checkoutService.createCheckout(CUSTOMER_EMAIL, 15L))
                .isInstanceOf(MidtransIntegrationException.class);
    }

    @Test
    void expiredReservationCannotCreateNewCheckout() {
        ReflectionTestUtils.setField(payment, "createdAt", Instant.now().minusSeconds(16 * 60));

        assertThatThrownBy(() -> checkoutService.createCheckout(CUSTOMER_EMAIL, 15L))
                .isInstanceOf(id.co.economz.eco_nomz.order.OrderConflictException.class)
                .hasMessageContaining("payment window has expired");
    }
}
