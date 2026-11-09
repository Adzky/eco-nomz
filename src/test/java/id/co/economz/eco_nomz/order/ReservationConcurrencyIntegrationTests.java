package id.co.economz.eco_nomz.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import id.co.economz.eco_nomz.offer.FoodOffer;
import id.co.economz.eco_nomz.offer.FoodOfferRepository;
import id.co.economz.eco_nomz.offer.OfferStatus;
import id.co.economz.eco_nomz.restaurant.Restaurant;
import id.co.economz.eco_nomz.restaurant.RestaurantRepository;
import id.co.economz.eco_nomz.user.User;
import id.co.economz.eco_nomz.user.UserRepository;
import id.co.economz.eco_nomz.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ReservationConcurrencyIntegrationTests {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.auth.jwt.secret", () -> "integration-test-secret-with-at-least-32-bytes");
    }

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private FoodOfferRepository foodOfferRepository;

    @Autowired
    private CustomerOrderRepository orderRepository;

    @Test
    void simultaneousReservationsCannotSellMoreThanAvailableStock() throws Exception {
        String testId = UUID.randomUUID().toString();
        User owner = userRepository.saveAndFlush(new User(
                "Concurrency Owner", "owner-" + testId + "@example.com", "hashed-password",
                UserRole.RESTAURANT_OWNER
        ));
        User firstCustomer = userRepository.saveAndFlush(new User(
                "First Customer", "first-" + testId + "@example.com", "hashed-password", UserRole.CUSTOMER
        ));
        User secondCustomer = userRepository.saveAndFlush(new User(
                "Second Customer", "second-" + testId + "@example.com", "hashed-password", UserRole.CUSTOMER
        ));
        Restaurant restaurant = restaurantRepository.saveAndFlush(
                new Restaurant(owner, "Concurrency Cafe", null, "Jakarta", null, null, true)
        );
        OffsetDateTime pickupStart = OffsetDateTime.now(ZoneId.of("Asia/Jakarta")).plusHours(2);
        FoodOffer offer = foodOfferRepository.saveAndFlush(new FoodOffer(
                restaurant,
                "Single item",
                null,
                new BigDecimal("50000"),
                new BigDecimal("25000"),
                1,
                pickupStart,
                pickupStart.plusHours(2),
                OfferStatus.PUBLISHED
        ));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> tryReserve(firstCustomer.getEmail(), offer.getId(), ready, start));
            var second = executor.submit(() -> tryReserve(secondCustomer.getEmail(), offer.getId(), ready, start));
            ready.await();
            start.countDown();

            int successfulReservations = (first.get() ? 1 : 0) + (second.get() ? 1 : 0);

            assertThat(successfulReservations).isEqualTo(1);
            assertThat(foodOfferRepository.findById(offer.getId()).orElseThrow().getQuantityAvailable()).isZero();
            assertThat(orderRepository.findByFoodOfferRestaurantOwnerId(owner.getId())).hasSize(1);
        }
    }

    private boolean tryReserve(
            String customerEmail,
            Long offerId,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            reservationService.reserve(customerEmail, offerId, 1);
            return true;
        } catch (OrderConflictException exception) {
            return false;
        }
    }
}
