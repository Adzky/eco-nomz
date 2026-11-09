package id.co.economz.eco_nomz.restaurant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import id.co.economz.eco_nomz.user.User;
import id.co.economz.eco_nomz.user.UserRepository;
import id.co.economz.eco_nomz.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTests {
    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private UserRepository userRepository;

    private RestaurantService restaurantService;

    @BeforeEach
    void setUp() {
        restaurantService = new RestaurantService(restaurantRepository, userRepository);
    }

    @Test
    void createsProfileOwnedByAuthenticatedRestaurantOwner() {
        User owner = new User("Owner", "owner@example.com", "password-hash", UserRole.RESTAURANT_OWNER);
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
        when(restaurantRepository.save(any(Restaurant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        RestaurantForm form = new RestaurantForm();
        form.setName("  Green Cafe  ");
        form.setDescription("  Surplus meals  ");
        form.setAddress("  Jakarta  ");
        form.setLatitude(new BigDecimal("-6.200000"));
        form.setLongitude(new BigDecimal("106.816666"));
        form.setPublished(false);

        Restaurant restaurant = restaurantService.createRestaurant("owner@example.com", form);

        assertThat(restaurant.getOwner()).isSameAs(owner);
        assertThat(restaurant.getName()).isEqualTo("Green Cafe");
        assertThat(restaurant.getDescription()).isEqualTo("Surplus meals");
        assertThat(restaurant.getAddress()).isEqualTo("Jakarta");
        assertThat(restaurant.getLatitude()).isEqualByComparingTo("-6.200000");
        assertThat(restaurant.getLongitude()).isEqualByComparingTo("106.816666");
        assertThat(restaurant.isPublished()).isFalse();
        verify(restaurantRepository).save(restaurant);
    }

    @Test
    void doesNotPublishNewRestaurantBeforeItsLogoIsUploaded() {
        User owner = new User("Owner", "owner@example.com", "password-hash", UserRole.RESTAURANT_OWNER);
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
        RestaurantForm form = new RestaurantForm();
        form.setName("Cafe");
        form.setAddress("Jakarta");
        form.setPublished(true);

        assertThatThrownBy(() -> restaurantService.createRestaurant("owner@example.com", form))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    @Test
    void customerCannotManageRestaurantProfiles() {
        User customer = new User("Customer", "customer@example.com", "password-hash", UserRole.CUSTOMER);
        when(userRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> restaurantService.findOwnerRestaurants("customer@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void ownerCannotUpdateRestaurantBelongingToAnotherUser() {
        User owner = new User("Owner", "owner@example.com", "password-hash", UserRole.RESTAURANT_OWNER);
        Restaurant restaurant = new Restaurant(owner, "My Cafe", null, "Jakarta");
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
        when(restaurantRepository.findByIdAndOwnerEmailIgnoreCase(15L, "owner@example.com"))
                .thenReturn(Optional.empty());

        RestaurantForm form = new RestaurantForm();
        form.setName("Changed Cafe");
        form.setAddress("Jakarta");

        assertThatThrownBy(() -> restaurantService.updateRestaurant(15L, "owner@example.com", form))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
        assertThat(restaurant.getName()).isEqualTo("My Cafe");
    }

    @Test
    void doesNotPublishOwnedRestaurantWithoutLogo() {
        User owner = new User("Owner", "owner@example.com", "password-hash", UserRole.RESTAURANT_OWNER);
        Restaurant restaurant = new Restaurant(owner, "My Cafe", null, "Jakarta");
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
        when(restaurantRepository.findByIdAndOwnerEmailIgnoreCase(15L, "owner@example.com"))
                .thenReturn(Optional.of(restaurant));
        RestaurantForm form = new RestaurantForm();
        form.setName("My Cafe");
        form.setAddress("Jakarta");
        form.setPublished(true);

        assertThatThrownBy(() -> restaurantService.updateRestaurant(15L, "owner@example.com", form))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
        assertThat(restaurant.isPublished()).isFalse();
    }
}
