package id.co.economz.eco_nomz.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import id.co.economz.eco_nomz.offer.FoodOffer;
import id.co.economz.eco_nomz.offer.FoodOfferRepository;
import id.co.economz.eco_nomz.offer.OfferStatus;
import id.co.economz.eco_nomz.payment.Payment;
import id.co.economz.eco_nomz.payment.PaymentRepository;
import id.co.economz.eco_nomz.payment.PaymentStatus;
import id.co.economz.eco_nomz.restaurant.Restaurant;
import id.co.economz.eco_nomz.user.User;
import id.co.economz.eco_nomz.user.UserRepository;
import id.co.economz.eco_nomz.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTests {
    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Jakarta");
    private static final String CUSTOMER_EMAIL = "customer@example.com";
    private static final String OWNER_EMAIL = "owner@example.com";

    @Mock
    private CustomerOrderRepository orderRepository;

    @Mock
    private FoodOfferRepository offerRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(
                orderRepository, offerRepository, paymentRepository, userRepository
        );
    }

    @Test
    void reservesRequestedQuantityUsingServerPriceAndPersistsOrderSnapshot() {
        User customer = customer();
        FoodOffer offer = availableOffer(4);
        when(userRepository.findByEmailIgnoreCase(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(offerRepository.lockById(7L)).thenReturn(Optional.of(offer));
        when(orderRepository.save(any(CustomerOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomerOrder order = reservationService.reserve(CUSTOMER_EMAIL, 7L, 2);

        assertThat(offer.getQuantityAvailable()).isEqualTo(2);
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.PUBLISHED);
        assertThat(order.getUnitPrice()).isEqualByComparingTo("25000.00");
        assertThat(order.getTotalPrice()).isEqualByComparingTo("50000.00");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.getPickupCode()).isNotBlank();
        ArgumentCaptor<CustomerOrder> savedOrder = ArgumentCaptor.forClass(CustomerOrder.class);
        verify(orderRepository).save(savedOrder.capture());
        assertThat(savedOrder.getValue().getFoodOffer()).isSameAs(offer);
    }

    @Test
    void marksOfferSoldOutWhenLastItemIsReserved() {
        User customer = customer();
        FoodOffer offer = availableOffer(1);
        when(userRepository.findByEmailIgnoreCase(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(offerRepository.lockById(7L)).thenReturn(Optional.of(offer));
        when(orderRepository.save(any(CustomerOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        reservationService.reserve(CUSTOMER_EMAIL, 7L, 1);

        assertThat(offer.getQuantityAvailable()).isZero();
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.SOLD_OUT);
    }

    @Test
    void refusesToOversellWithoutCreatingOrder() {
        User customer = customer();
        FoodOffer offer = availableOffer(1);
        when(userRepository.findByEmailIgnoreCase(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(offerRepository.lockById(7L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> reservationService.reserve(CUSTOMER_EMAIL, 7L, 2))
                .isInstanceOf(OrderConflictException.class)
                .hasMessageContaining("Only 1 items remain");
        assertThat(offer.getQuantityAvailable()).isEqualTo(1);
        verify(orderRepository, org.mockito.Mockito.never()).save(any(CustomerOrder.class));
    }

    @Test
    void rejectsExpiredOrUnavailableOffer() {
        User customer = customer();
        FoodOffer offer = availableOffer(2);
        offer.reserveQuantity(2);
        when(userRepository.findByEmailIgnoreCase(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(offerRepository.lockById(7L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> reservationService.reserve(CUSTOMER_EMAIL, 7L, 1))
                .isInstanceOf(OrderConflictException.class)
                .hasMessageContaining("no longer available");
        verify(orderRepository, org.mockito.Mockito.never()).save(any(CustomerOrder.class));
    }

    @Test
    void pendingReservationCancellationRestoresInventoryAndCannotBeRepeated() {
        User customer = customer();
        FoodOffer offer = availableOffer(2);
        ReflectionTestUtils.setField(offer, "id", 7L);
        offer.reserveQuantity(1);
        CustomerOrder order = new CustomerOrder(
                customer, offer, 1, new BigDecimal("25000.00"),
                new BigDecimal("25000.00"), OrderStatus.PENDING_PAYMENT
        );
        order.assignPickupCode("pickup-test-code");
        Payment payment = new Payment(order, "ECO-test-order", new BigDecimal("25000.00"), PaymentStatus.PENDING);
        order.attachPayment(payment);
        when(userRepository.findByEmailIgnoreCase(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(orderRepository.findByIdAndCustomerEmailIgnoreCase(15L, CUSTOMER_EMAIL))
                .thenReturn(Optional.of(order));
        when(offerRepository.lockById(7L)).thenReturn(Optional.of(offer));
        when(paymentRepository.lockByOrderId(15L)).thenReturn(Optional.of(payment));

        reservationService.cancel(CUSTOMER_EMAIL, 15L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getPayment().getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(offer.getQuantityAvailable()).isEqualTo(2);
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.PUBLISHED);

        assertThatThrownBy(() -> reservationService.cancel(CUSTOMER_EMAIL, 15L))
                .isInstanceOf(OrderConflictException.class);
    }

    @Test
    void customerCannotCancelAnotherCustomersOrder() {
        User customer = customer();
        when(userRepository.findByEmailIgnoreCase(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(orderRepository.findByIdAndCustomerEmailIgnoreCase(15L, CUSTOMER_EMAIL))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.cancel(CUSTOMER_EMAIL, 15L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cancellationAfterPickupStartsIsRejectedWithoutRestocking() {
        User customer = customer();
        FoodOffer offer = availableOffer(2);
        offer.reserveQuantity(1);
        offer.updateOffer(
                offer.getRestaurant(),
                offer.getTitle(),
                offer.getDescription(),
                offer.getOriginalPrice(),
                offer.getDiscountedPrice(),
                offer.getQuantityAvailable(),
                OffsetDateTime.now(MARKET_ZONE).minusHours(1),
                OffsetDateTime.now(MARKET_ZONE).plusHours(1),
                true
        );
        CustomerOrder order = new CustomerOrder(
                customer, offer, 1, new BigDecimal("25000.00"),
                new BigDecimal("25000.00"), OrderStatus.CONFIRMED
        );
        Payment payment = new Payment(order, "ECO-test-order", new BigDecimal("25000.00"), PaymentStatus.SUCCEEDED);
        order.attachPayment(payment);
        when(userRepository.findByEmailIgnoreCase(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(orderRepository.findByIdAndCustomerEmailIgnoreCase(15L, CUSTOMER_EMAIL))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> reservationService.cancel(CUSTOMER_EMAIL, 15L))
                .isInstanceOf(OrderConflictException.class)
                .hasMessageContaining("no longer be cancelled");
        assertThat(offer.getQuantityAvailable()).isEqualTo(1);
    }

    @Test
    void rejectsInvalidReservationQuantity() {
        assertThatThrownBy(() -> reservationService.reserve(CUSTOMER_EMAIL, 7L, 0))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsNonCustomerReservation() {
        User owner = new User("Owner", OWNER_EMAIL, "hash", UserRole.RESTAURANT_OWNER);
        when(userRepository.findByEmailIgnoreCase(OWNER_EMAIL)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> reservationService.reserve(OWNER_EMAIL, 7L, 1))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private User customer() {
        return new User("Customer", CUSTOMER_EMAIL, "hash", UserRole.CUSTOMER);
    }

    private FoodOffer availableOffer(int quantity) {
        User owner = new User("Owner", OWNER_EMAIL, "hash", UserRole.RESTAURANT_OWNER);
        Restaurant restaurant = new Restaurant(owner, "Eco Cafe", null, "Jakarta", null, null, true);
        OffsetDateTime pickupStart = OffsetDateTime.now(MARKET_ZONE).plusHours(2);
        return new FoodOffer(
                restaurant,
                "Surprise food bag",
                null,
                new BigDecimal("50000.00"),
                new BigDecimal("25000.00"),
                quantity,
                pickupStart,
                pickupStart.plusHours(2),
                OfferStatus.PUBLISHED
        );
    }
}
