package id.co.economz.eco_nomz.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import jakarta.servlet.http.Cookie;
import id.co.economz.eco_nomz.auth.JwtService;
import id.co.economz.eco_nomz.offer.FoodOffer;
import id.co.economz.eco_nomz.offer.FoodOfferRepository;
import id.co.economz.eco_nomz.offer.OfferStatus;
import id.co.economz.eco_nomz.order.CustomerOrder;
import id.co.economz.eco_nomz.order.CustomerOrderRepository;
import id.co.economz.eco_nomz.order.OrderStatus;
import id.co.economz.eco_nomz.payment.Payment;
import id.co.economz.eco_nomz.payment.PaymentRepository;
import id.co.economz.eco_nomz.payment.PaymentStatus;
import id.co.economz.eco_nomz.restaurant.Restaurant;
import id.co.economz.eco_nomz.restaurant.RestaurantRepository;
import id.co.economz.eco_nomz.user.User;
import id.co.economz.eco_nomz.user.UserRepository;
import id.co.economz.eco_nomz.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class PersistenceIntegrationTests {
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
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private FoodOfferRepository foodOfferRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void registrationLoginRoleAuthorizationAndLogoutWorkWithJwtCookie() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "Test Customer")
                        .param("email", "Test.Customer@example.com")
                        .param("password", "correct-horse-battery"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));

        User registeredUser = userRepository.findByEmailIgnoreCase("test.customer@example.com").orElseThrow();
        assertThat(registeredUser.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(registeredUser.getPasswordHash()).isNotEqualTo("correct-horse-battery");

        var loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", "TEST.CUSTOMER@example.com")
                        .param("password", "correct-horse-battery"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customer"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")))
                .andReturn();

        String setCookie = loginResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull();
        String token = setCookie.substring("ECO_NOMZ_AUTH=".length(), setCookie.indexOf(';'));
        Cookie authCookie = new Cookie("ECO_NOMZ_AUTH", token);

        mockMvc.perform(get("/customer").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(view().name("account/dashboard"));
        mockMvc.perform(get("/owner").cookie(authCookie))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/logout").cookie(authCookie).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    @Test
    void restaurantOwnerCanOnboardWithoutManualRolePromotion() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("accountType", "RESTAURANT_OWNER")
                        .param("name", "New Restaurant Owner")
                        .param("email", "new-owner@example.com")
                        .param("password", "correct-horse-battery"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));

        User owner = userRepository.findByEmailIgnoreCase("new-owner@example.com").orElseThrow();
        assertThat(owner.getRole()).isEqualTo(UserRole.RESTAURANT_OWNER);

        var loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", "new-owner@example.com")
                        .param("password", "correct-horse-battery"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner"))
                .andReturn();

        String setCookie = loginResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String token = setCookie.substring("ECO_NOMZ_AUTH=".length(), setCookie.indexOf(';'));
        Cookie ownerCookie = new Cookie("ECO_NOMZ_AUTH", token);
        mockMvc.perform(get("/owner/restaurant").cookie(ownerCookie))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/restaurants"));
        mockMvc.perform(get("/customer/orders").cookie(ownerCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void csrfIsRequiredAndInvalidJwtCannotAuthenticate() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "Test Customer")
                        .param("email", "csrf@example.com")
                        .param("password", "correct-horse-battery"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/customer").cookie(new Cookie("ECO_NOMZ_AUTH", "not-a-valid-token")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void restaurantOwnerCanManageOwnProfileAndPublicCanOnlyViewPublishedProfiles() throws Exception {
        User owner = userRepository.saveAndFlush(
                new User("Restaurant Owner", "restaurant-owner@example.com", "hashed-password", UserRole.RESTAURANT_OWNER)
        );
        User otherOwner = userRepository.saveAndFlush(
                new User("Other Owner", "other-owner@example.com", "hashed-password", UserRole.RESTAURANT_OWNER)
        );
        Cookie ownerCookie = new Cookie(
                "ECO_NOMZ_AUTH",
                jwtService.generateToken(owner.getEmail())
        );
        Cookie otherOwnerCookie = new Cookie(
                "ECO_NOMZ_AUTH",
                jwtService.generateToken(otherOwner.getEmail())
        );

        mockMvc.perform(get("/owner/restaurant").cookie(ownerCookie))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/restaurants"));

        mockMvc.perform(post("/owner/restaurant")
                        .cookie(ownerCookie)
                        .with(csrf())
                        .param("name", "Owner Cafe")
                        .param("description", "A neighborhood cafe")
                        .param("address", "Jakarta")
                        .param("latitude", "-6.200000")
                        .param("longitude", "106.816666")
                        .param("published", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/owner/restaurant/*/logo/crop"));

        Restaurant restaurant = restaurantRepository.findByOwnerEmailIgnoreCaseOrderByNameAsc(owner.getEmail())
                .getFirst();
        assertThat(restaurant.isPublished()).isFalse();
        assertThat(restaurant.getLatitude()).isEqualByComparingTo("-6.200000");

        mockMvc.perform(get("/restaurants"))
                .andExpect(status().isOk())
                .andExpect(view().name("restaurants/list"));
        mockMvc.perform(get("/restaurants/{id}", restaurant.getId()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/owner/restaurant/edit")
                        .param("id", restaurant.getId().toString())
                        .cookie(otherOwnerCookie))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/owner/restaurant/update")
                        .cookie(otherOwnerCookie)
                        .with(csrf())
                        .param("id", restaurant.getId().toString())
                        .param("name", "Hijacked Cafe")
                        .param("address", "Jakarta"))
                .andExpect(status().isNotFound());
        assertThat(restaurantRepository.findById(restaurant.getId()).orElseThrow().getName())
                .isEqualTo("Owner Cafe");

        mockMvc.perform(post("/owner/restaurant/{id}/delete", restaurant.getId())
                        .cookie(otherOwnerCookie)
                        .with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(restaurantRepository.existsById(restaurant.getId())).isTrue();

        restaurant.updateLogo(
                "https://res.cloudinary.com/demo/image/upload/logo.png",
                "eco-nomz/restaurant-logos/test-logo"
        );
        restaurantRepository.saveAndFlush(restaurant);

        mockMvc.perform(post("/owner/restaurant/update")
                        .cookie(ownerCookie)
                        .with(csrf())
                        .param("id", restaurant.getId().toString())
                        .param("name", "Updated Cafe")
                        .param("address", "Central Jakarta")
                        .param("published", "true"))
                .andExpect(status().is3xxRedirection());
        assertThat(restaurantRepository.findById(restaurant.getId()).orElseThrow().isPublished()).isTrue();
        mockMvc.perform(get("/restaurants/{id}", restaurant.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("restaurants/detail"));

        mockMvc.perform(post("/owner/restaurant/update")
                        .cookie(ownerCookie)
                        .with(csrf())
                        .param("id", restaurant.getId().toString())
                        .param("name", "Updated Cafe")
                        .param("address", "Central Jakarta")
                        .param("published", "false"))
                .andExpect(status().is3xxRedirection());
        assertThat(restaurantRepository.findById(restaurant.getId()).orElseThrow().isPublished()).isFalse();
        mockMvc.perform(get("/restaurants/{id}", restaurant.getId()))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/owner/restaurant/{id}/delete", restaurant.getId())
                        .cookie(ownerCookie)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(restaurantRepository.existsById(restaurant.getId())).isFalse();
    }

    @Test
    void customerCannotAccessRestaurantOwnerManagementAndInvalidProfileIsRejected() throws Exception {
        User customer = userRepository.saveAndFlush(
                new User("Customer", "market-customer@example.com", "hashed-password", UserRole.CUSTOMER)
        );
        Cookie customerCookie = new Cookie(
                "ECO_NOMZ_AUTH",
                jwtService.generateToken(customer.getEmail())
        );
        mockMvc.perform(get("/owner/restaurant").cookie(customerCookie))
                .andExpect(status().isForbidden());

        User owner = userRepository.saveAndFlush(
                new User("Restaurant Owner", "validation-owner@example.com", "hashed-password",
                        UserRole.RESTAURANT_OWNER)
        );
        Cookie ownerCookie = new Cookie(
                "ECO_NOMZ_AUTH",
                jwtService.generateToken(owner.getEmail())
        );
        mockMvc.perform(post("/owner/restaurant")
                        .cookie(ownerCookie)
                        .with(csrf())
                        .param("name", "")
                        .param("address", "Jakarta")
                        .param("latitude", "91"))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/restaurant-form"))
                .andExpect(content().string(containsString("must be less than or equal to 90")));
        assertThat(restaurantRepository.findByOwnerEmailIgnoreCaseOrderByNameAsc(owner.getEmail())).isEmpty();
    }

    @Test
    void persistsMarketplaceAndOrderRelationships() {
        User owner = userRepository.saveAndFlush(
                new User("Restaurant Owner", "owner@example.com", "hashed-password", UserRole.RESTAURANT_OWNER)
        );
        User customer = userRepository.saveAndFlush(
                new User("Customer", "customer@example.com", "hashed-password", UserRole.CUSTOMER)
        );
        Restaurant restaurant = restaurantRepository.saveAndFlush(
                new Restaurant(owner, "Eco Kitchen", "Surplus meals", "Jakarta")
        );
        FoodOffer offer = foodOfferRepository.saveAndFlush(new FoodOffer(
                restaurant,
                "Surprise meal",
                "End-of-day meal",
                new BigDecimal("50000.00"),
                new BigDecimal("25000.00"),
                4,
                OffsetDateTime.parse("2026-10-29T10:00:00+07:00"),
                OffsetDateTime.parse("2026-10-29T12:00:00+07:00"),
                OfferStatus.PUBLISHED
        ));
        CustomerOrder order = customerOrderRepository.saveAndFlush(new CustomerOrder(
                customer,
                offer,
                2,
                new BigDecimal("25000.00"),
                new BigDecimal("50000.00"),
                OrderStatus.CONFIRMED
        ));
        Payment payment = paymentRepository.saveAndFlush(new Payment(
                order,
                "provider-transaction-123",
                new BigDecimal("50000.00"),
                PaymentStatus.SUCCEEDED
        ));

        assertThat(userRepository.findByEmailIgnoreCase("OWNER@example.com")).contains(owner);
        assertThat(restaurantRepository.findByOwnerEmailIgnoreCaseOrderByNameAsc(owner.getEmail()))
                .contains(restaurant);
        assertThat(foodOfferRepository.findById(offer.getId())).contains(offer);
        assertThat(customerOrderRepository.findByCustomerId(customer.getId())).contains(order);
        assertThat(customerOrderRepository.findByFoodOfferRestaurantOwnerId(owner.getId())).contains(order);
        assertThat(paymentRepository.findByOrderId(order.getId())).contains(payment);
        assertThat(paymentRepository.findByProviderReference("provider-transaction-123")).contains(payment);
    }

    @Test
    void marketplaceSearchFiltersPublishedAvailableOffersByNamePriceAndQuantity() {
        User owner = userRepository.saveAndFlush(
                new User("Restaurant Owner", "search-owner@example.com", "hashed-password",
                        UserRole.RESTAURANT_OWNER)
        );
        Restaurant restaurant = restaurantRepository.saveAndFlush(
                new Restaurant(owner, "Nasi Pagi", "Breakfast cafe", "Jakarta", null, null, true)
        );
        OffsetDateTime pickupStart = OffsetDateTime.now(ZoneId.of("Asia/Jakarta")).plusHours(2);
        OffsetDateTime pickupEnd = pickupStart.plusHours(2);
        FoodOffer matchingOffer = foodOfferRepository.saveAndFlush(new FoodOffer(
                restaurant, "Nasi surplus bag", null, new BigDecimal("50000"),
                new BigDecimal("25000"), 4, pickupStart, pickupEnd, OfferStatus.PUBLISHED
        ));
        foodOfferRepository.saveAndFlush(new FoodOffer(
                restaurant, "Nasi small bag", null, new BigDecimal("50000"),
                new BigDecimal("15000"), 1, pickupStart, pickupEnd, OfferStatus.PUBLISHED
        ));
        foodOfferRepository.saveAndFlush(new FoodOffer(
                restaurant, "Nasi ended bag", null, new BigDecimal("50000"),
                new BigDecimal("25000"), 4, pickupStart.minusDays(1), pickupStart, OfferStatus.PUBLISHED
        ));

        var result = foodOfferRepository.searchAvailableOffers(
                OfferStatus.PUBLISHED,
                2,
                OffsetDateTime.now(ZoneId.of("Asia/Jakarta")),
                "nasi",
                new BigDecimal("20000"),
                new BigDecimal("30000"),
                PageRequest.of(0, 10)
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).containsExactly(matchingOffer);
        assertThat(result.getContent().getFirst().getRestaurant().getAddress()).isEqualTo("Jakarta");
    }

    @Test
    void customerReservationConsumesStockAndCancellationRestoresIt() throws Exception {
        User owner = userRepository.saveAndFlush(
                new User("Reservation Owner", "reservation-owner@example.com", "hashed-password",
                        UserRole.RESTAURANT_OWNER)
        );
        User customer = userRepository.saveAndFlush(
                new User("Reservation Customer", "reservation-customer@example.com", "hashed-password",
                        UserRole.CUSTOMER)
        );
        User otherCustomer = userRepository.saveAndFlush(
                new User("Other Customer", "other-reservation-customer@example.com", "hashed-password",
                        UserRole.CUSTOMER)
        );
        Restaurant restaurant = restaurantRepository.saveAndFlush(
                new Restaurant(owner, "Reservation Cafe", null, "Jakarta", null, null, true)
        );
        OffsetDateTime pickupStart = OffsetDateTime.now(ZoneId.of("Asia/Jakarta")).plusHours(2);
        FoodOffer offer = foodOfferRepository.saveAndFlush(new FoodOffer(
                restaurant,
                "Reservation bag",
                null,
                new BigDecimal("50000"),
                new BigDecimal("25000"),
                3,
                pickupStart,
                pickupStart.plusHours(2),
                OfferStatus.PUBLISHED
        ));
        Cookie customerCookie = new Cookie("ECO_NOMZ_AUTH", jwtService.generateToken(customer.getEmail()));
        Cookie otherCustomerCookie = new Cookie(
                "ECO_NOMZ_AUTH",
                jwtService.generateToken(otherCustomer.getEmail())
        );

        mockMvc.perform(post("/customer/orders")
                        .cookie(customerCookie)
                        .with(csrf())
                        .param("offerId", offer.getId().toString())
                        .param("quantity", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customer/orders"));

        assertThat(offer.getQuantityAvailable()).isEqualTo(1);
        var savedOrder = customerOrderRepository.findByCustomerId(customer.getId()).getFirst();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(savedOrder.getUnitPrice()).isEqualByComparingTo("25000.00");
        assertThat(savedOrder.getTotalPrice()).isEqualByComparingTo("50000.00");
        assertThat(savedOrder.getPickupCode()).isNotBlank();
        assertThat(paymentRepository.findByOrderId(savedOrder.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.PENDING);

        mockMvc.perform(get("/customer/orders").cookie(customerCookie))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/customer-list"));
        mockMvc.perform(get("/owner/orders").cookie(new Cookie(
                        "ECO_NOMZ_AUTH",
                        jwtService.generateToken(owner.getEmail())
                )))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/owner-list"));

        mockMvc.perform(post("/customer/orders/{id}/cancel", savedOrder.getId())
                        .cookie(otherCustomerCookie)
                        .with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);

        mockMvc.perform(post("/customer/orders/{id}/cancel", savedOrder.getId())
                        .cookie(customerCookie)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(paymentRepository.findByOrderId(savedOrder.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.CANCELLED);
        assertThat(offer.getQuantityAvailable()).isEqualTo(3);
    }

    @Test
    void reservationRequiresCustomerRoleAndCsrf() throws Exception {
        User owner = userRepository.saveAndFlush(
                new User("Role Owner", "role-owner@example.com", "hashed-password", UserRole.RESTAURANT_OWNER)
        );
        Cookie ownerCookie = new Cookie("ECO_NOMZ_AUTH", jwtService.generateToken(owner.getEmail()));

        mockMvc.perform(post("/customer/orders")
                        .cookie(ownerCookie)
                        .with(csrf())
                        .param("offerId", "1")
                        .param("quantity", "1"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/customer/orders")
                        .param("offerId", "1")
                        .param("quantity", "1"))
                .andExpect(status().isForbidden());
    }
}
