package id.co.economz.eco_nomz.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import id.co.economz.eco_nomz.offer.FoodOffer;
import id.co.economz.eco_nomz.offer.FoodOfferRepository;
import id.co.economz.eco_nomz.offer.OfferStatus;
import id.co.economz.eco_nomz.restaurant.Restaurant;
import id.co.economz.eco_nomz.restaurant.RestaurantRepository;
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
class AdminServiceTests {
    @Mock
    private UserRepository userRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private FoodOfferRepository foodOfferRepository;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(userRepository, restaurantRepository, foodOfferRepository);
    }

    @Test
    void nonAdministratorCannotOpenAdministration() {
        User customer = new User("Customer", "customer@example.com", "hash", UserRole.CUSTOMER);
        when(userRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> adminService.dashboard("customer@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void administratorCanSuspendAndReactivateCustomer() {
        User administrator = user("Admin", "admin@example.com", UserRole.ADMIN, 1L);
        User customer = user("Customer", "customer@example.com", UserRole.CUSTOMER, 2L);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(administrator));
        when(userRepository.findById(2L)).thenReturn(Optional.of(customer));

        adminService.setUserActive("admin@example.com", 2L, false);
        assertThat(customer.isActive()).isFalse();

        adminService.setUserActive("admin@example.com", 2L, true);
        assertThat(customer.isActive()).isTrue();
    }

    @Test
    void administratorCannotDisableSelfOrAnotherAdministrator() {
        User administrator = user("Admin", "admin@example.com", UserRole.ADMIN, 1L);
        User anotherAdministrator = user("Other Admin", "other@example.com", UserRole.ADMIN, 2L);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(administrator));
        when(userRepository.findById(1L)).thenReturn(Optional.of(administrator));
        when(userRepository.findById(2L)).thenReturn(Optional.of(anotherAdministrator));

        assertThatThrownBy(() -> adminService.setUserActive("admin@example.com", 1L, false))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        assertThatThrownBy(() -> adminService.setUserActive("admin@example.com", 2L, false))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        assertThat(administrator.isActive()).isTrue();
        assertThat(anotherAdministrator.isActive()).isTrue();
    }

    @Test
    void administratorCanModerateRestaurantAndOfferWithoutChangingPublicationStatus() {
        User administrator = user("Admin", "admin@example.com", UserRole.ADMIN, 1L);
        Restaurant restaurant = new Restaurant(
                user("Owner", "owner@example.com", UserRole.RESTAURANT_OWNER, 2L),
                "Cafe",
                null,
                "Jakarta",
                null,
                null,
                true
        );
        FoodOffer offer = new FoodOffer(
                restaurant,
                "Meal",
                null,
                new java.math.BigDecimal("100"),
                new java.math.BigDecimal("50"),
                3,
                java.time.OffsetDateTime.now().plusHours(1),
                java.time.OffsetDateTime.now().plusHours(2),
                OfferStatus.PUBLISHED
        );
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(administrator));
        when(restaurantRepository.findById(3L)).thenReturn(Optional.of(restaurant));
        when(foodOfferRepository.findById(4L)).thenReturn(Optional.of(offer));

        adminService.setRestaurantModerated("admin@example.com", 3L, true);
        adminService.setOfferModerated("admin@example.com", 4L, true, "  Needs review  ");

        assertThat(restaurant.isModerated()).isTrue();
        assertThat(restaurant.isPublished()).isTrue();
        assertThat(offer.isModerated()).isTrue();
        assertThat(offer.getModerationNote()).isEqualTo("Needs review");
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.PUBLISHED);

        adminService.setOfferModerated("admin@example.com", 4L, false, null);
        assertThat(offer.isModerated()).isFalse();
        assertThat(offer.getModerationNote()).isNull();
    }

    private User user(String name, String email, UserRole role, long id) {
        User user = new User(name, email, "hash", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
