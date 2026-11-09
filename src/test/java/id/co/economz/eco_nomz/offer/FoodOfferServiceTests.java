package id.co.economz.eco_nomz.offer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class FoodOfferServiceTests {
    private static final String OWNER_EMAIL = "owner@example.com";
    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Jakarta");

    @Mock
    private FoodOfferRepository foodOfferRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private UserRepository userRepository;

    private FoodOfferService foodOfferService;

    @BeforeEach
    void setUp() {
        foodOfferService = new FoodOfferService(foodOfferRepository, restaurantRepository, userRepository);
    }

    @Test
    void createsPublishedOfferForOwnersPublishedRestaurant() {
        User owner = restaurantOwner();
        Restaurant restaurant = new Restaurant(owner, "Eco Cafe", null, "Jakarta", null, null, true);
        when(userRepository.findByEmailIgnoreCase(OWNER_EMAIL)).thenReturn(Optional.of(owner));
        when(restaurantRepository.findByIdAndOwnerEmailIgnoreCaseAndPublishedTrue(1L, OWNER_EMAIL))
                .thenReturn(Optional.of(restaurant));
        when(foodOfferRepository.save(any(FoodOffer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        FoodOfferForm form = validForm();
        form.setPublished(true);

        FoodOffer offer = foodOfferService.createOffer(OWNER_EMAIL, form);

        assertThat(offer.getRestaurant()).isSameAs(restaurant);
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.PUBLISHED);
        assertThat(offer.getDiscountedPrice()).isEqualByComparingTo("25000.00");
        assertThat(offer.getPickupStart().getOffset()).isEqualTo(MARKET_ZONE.getRules()
                .getOffset(form.getPickupStart().atZone(MARKET_ZONE).toInstant()));
        verify(foodOfferRepository).save(offer);
    }

    @Test
    void refusesToPublishOfferForUnpublishedRestaurant() {
        User owner = restaurantOwner();
        when(userRepository.findByEmailIgnoreCase(OWNER_EMAIL)).thenReturn(Optional.of(owner));
        when(restaurantRepository.findByIdAndOwnerEmailIgnoreCaseAndPublishedTrue(1L, OWNER_EMAIL))
                .thenReturn(Optional.empty());
        FoodOfferForm form = validForm();
        form.setPublished(true);

        assertThatThrownBy(() -> foodOfferService.createOffer(OWNER_EMAIL, form))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void refusesToPublishOfferWithPastPickupWindow() {
        User owner = restaurantOwner();
        Restaurant restaurant = new Restaurant(owner, "Eco Cafe", null, "Jakarta", null, null, true);
        when(userRepository.findByEmailIgnoreCase(OWNER_EMAIL)).thenReturn(Optional.of(owner));
        when(restaurantRepository.findByIdAndOwnerEmailIgnoreCaseAndPublishedTrue(1L, OWNER_EMAIL))
                .thenReturn(Optional.of(restaurant));
        FoodOfferForm form = validForm();
        form.setPublished(true);
        form.setPickupStart(LocalDateTime.now(MARKET_ZONE).minusHours(2));
        form.setPickupEnd(LocalDateTime.now(MARKET_ZONE).minusHours(1));

        assertThatThrownBy(() -> foodOfferService.createOffer(OWNER_EMAIL, form))
                .isInstanceOf(OfferPublicationException.class)
                .hasMessageContaining("future");
    }

    @Test
    void deniesCustomerAccessToOwnerOffers() {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(Optional.of(new User("Customer", "customer@example.com", "hash", UserRole.CUSTOMER)));

        assertThatThrownBy(() -> foodOfferService.findOwnerOffers("customer@example.com", 0))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void expiresOffersBeforeReturningMarketplacePageAndUsesAllowlistedSort() {
        when(foodOfferRepository.searchAvailableOffers(
                eq(OfferStatus.PUBLISHED),
                eq(1),
                any(OffsetDateTime.class),
                eq(""),
                eq(null),
                eq(null),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(java.util.List.of()));

        var offers = foodOfferService.findAvailableOffers(0, OfferSort.PRICE_LOW, null, null, null, 1);

        assertThat(offers).isEmpty();
        verify(foodOfferRepository).expirePublishedOffers(
                eq(OfferStatus.PUBLISHED),
                eq(OfferStatus.EXPIRED),
                any(OffsetDateTime.class)
        );
        verify(foodOfferRepository).searchAvailableOffers(
                eq(OfferStatus.PUBLISHED),
                eq(1),
                any(OffsetDateTime.class),
                eq(""),
                eq(null),
                eq(null),
                any(Pageable.class)
        );
    }

    @Test
    void marketplaceFiltersAndSortArePassedToRepository() {
        when(foodOfferRepository.searchAvailableOffers(
                eq(OfferStatus.PUBLISHED),
                eq(3),
                any(OffsetDateTime.class),
                eq("nasi"),
                eq(new BigDecimal("10000")),
                eq(new BigDecimal("50000")),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(java.util.List.of()));

        var offers = foodOfferService.findAvailableOffers(
                2,
                OfferSort.PRICE_HIGH,
                "  nasi  ",
                new BigDecimal("10000"),
                new BigDecimal("50000"),
                3
        );

        assertThat(offers).isEmpty();
        verify(foodOfferRepository).searchAvailableOffers(
                eq(OfferStatus.PUBLISHED),
                eq(3),
                any(OffsetDateTime.class),
                eq("nasi"),
                eq(new BigDecimal("10000")),
                eq(new BigDecimal("50000")),
                eq(PageRequest.of(2, FoodOfferService.PAGE_SIZE, Sort.by(Sort.Direction.DESC, "discountedPrice")))
        );
    }

    private User restaurantOwner() {
        return new User("Restaurant Owner", OWNER_EMAIL, "hashed-password", UserRole.RESTAURANT_OWNER);
    }

    private FoodOfferForm validForm() {
        FoodOfferForm form = new FoodOfferForm();
        form.setRestaurantId(1L);
        form.setTitle("Surprise food bag");
        form.setDescription("End-of-day surplus");
        form.setOriginalPrice(new BigDecimal("50000.00"));
        form.setDiscountedPrice(new BigDecimal("25000.00"));
        form.setQuantityAvailable(4);
        form.setPickupStart(LocalDateTime.now(MARKET_ZONE).plusDays(1));
        form.setPickupEnd(LocalDateTime.now(MARKET_ZONE).plusDays(1).plusHours(2));
        return form;
    }
}
