package id.co.economz.eco_nomz.offer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import id.co.economz.eco_nomz.restaurant.Restaurant;
import id.co.economz.eco_nomz.restaurant.RestaurantRepository;
import id.co.economz.eco_nomz.user.User;
import id.co.economz.eco_nomz.user.UserRepository;
import id.co.economz.eco_nomz.user.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FoodOfferService {
    public static final int PAGE_SIZE = 12;

    private static final ZoneId MARKETPLACE_ZONE = ZoneId.of("Asia/Jakarta");
    private final FoodOfferRepository foodOfferRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    public FoodOfferService(
            FoodOfferRepository foodOfferRepository,
            RestaurantRepository restaurantRepository,
            UserRepository userRepository
    ) {
        this.foodOfferRepository = foodOfferRepository;
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Page<FoodOffer> findAvailableOffers(
            int page,
            OfferSort sort,
            String search,
            BigDecimal minimumPrice,
            BigDecimal maximumPrice,
            int minimumQuantity
    ) {
        OffsetDateTime now = now();
        expireOffers(now);
        return foodOfferRepository.searchAvailableOffers(
                OfferStatus.PUBLISHED,
                minimumQuantity,
                now,
                normalizeSearch(search),
                minimumPrice,
                maximumPrice,
                pageable(page, sort)
        );
    }

    @Transactional
    public FoodOffer getAvailableOffer(Long id) {
        OffsetDateTime now = now();
        expireOffers(now);
        return foodOfferRepository
                .findByIdAndStatusAndQuantityAvailableGreaterThanAndPickupEndAfterAndModeratedFalseAndRestaurantPublishedTrueAndRestaurantModeratedFalseAndRestaurantOwnerActiveTrue(
                        id,
                        OfferStatus.PUBLISHED,
                        0,
                        now
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Page<FoodOffer> findOwnerOffers(String ownerEmail, int page) {
        requireRestaurantOwner(ownerEmail);
        expireOffers(now());
        return foodOfferRepository.findByRestaurantOwnerEmailIgnoreCaseOrderByCreatedAtDesc(
                ownerEmail,
                PageRequest.of(normalizePage(page), PAGE_SIZE)
        );
    }

    @Transactional(readOnly = true)
    public List<Restaurant> findOwnerRestaurants(String ownerEmail) {
        requireRestaurantOwner(ownerEmail);
        return restaurantRepository.findByOwnerEmailIgnoreCaseOrderByNameAsc(ownerEmail);
    }

    @Transactional
    public FoodOfferForm getOwnerOfferForm(Long id, String ownerEmail) {
        requireRestaurantOwner(ownerEmail);
        if (id == null) {
            return new FoodOfferForm();
        }
        return FoodOfferForm.from(getOwnedOffer(id, ownerEmail));
    }

    @Transactional
    public FoodOffer createOffer(String ownerEmail, FoodOfferForm form) {
        User owner = requireRestaurantOwner(ownerEmail);
        Restaurant restaurant = getOwnedRestaurant(form.getRestaurantId(), ownerEmail, form.isPublished());
        validatePublication(form, restaurant);

        FoodOffer offer = new FoodOffer(
                restaurant,
                form.getTitle().trim(),
                normalizeDescription(form.getDescription()),
                form.getOriginalPrice(),
                form.getDiscountedPrice(),
                form.getQuantityAvailable(),
                form.pickupStartAtMarketZone(),
                form.pickupEndAtMarketZone(),
                form.isPublished() ? OfferStatus.PUBLISHED : OfferStatus.DRAFT
        );
        return foodOfferRepository.save(offer);
    }

    @Transactional
    public FoodOffer updateOffer(Long id, String ownerEmail, FoodOfferForm form) {
        FoodOffer offer = getOwnedOffer(id, ownerEmail);
        if (offer.getStatus() == OfferStatus.EXPIRED || offer.getStatus() == OfferStatus.SOLD_OUT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Expired or sold-out offers cannot be edited.");
        }
        Restaurant restaurant = getOwnedRestaurant(form.getRestaurantId(), ownerEmail, form.isPublished());
        validatePublication(form, restaurant);
        offer.updateOffer(
                restaurant,
                form.getTitle().trim(),
                normalizeDescription(form.getDescription()),
                form.getOriginalPrice(),
                form.getDiscountedPrice(),
                form.getQuantityAvailable(),
                form.pickupStartAtMarketZone(),
                form.pickupEndAtMarketZone(),
                form.isPublished()
        );
        return offer;
    }

    private void validatePublication(FoodOfferForm form, Restaurant restaurant) {
        if (!form.isPublished()) {
            return;
        }
        if (!restaurant.isPublished()) {
            throw OfferPublicationException.localized(
                    "offer.publication.restaurantNotPublished",
                    "Publish the restaurant profile before publishing its offers."
            );
        }
        OffsetDateTime now = now();
        if (!form.pickupStartAtMarketZone().isAfter(now)) {
            throw OfferPublicationException.localized(
                    "offer.publication.pickupStartPast",
                    "Pickup must start in the future to publish an offer."
            );
        }
        if (!form.pickupEndAtMarketZone().isAfter(now)) {
            throw OfferPublicationException.localized(
                    "offer.publication.pickupWindowPast",
                    "Pickup window has already ended."
            );
        }
        if (form.getQuantityAvailable() < 1) {
            throw OfferPublicationException.localized(
                    "offer.publication.quantityRequired",
                    "At least one item must be available to publish an offer."
            );
        }
    }

    private FoodOffer getOwnedOffer(Long id, String ownerEmail) {
        requireRestaurantOwner(ownerEmail);
        return foodOfferRepository.findByIdAndRestaurantOwnerEmailIgnoreCase(id, ownerEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private Restaurant getOwnedRestaurant(Long restaurantId, String ownerEmail, boolean forPublication) {
        return (forPublication
                ? restaurantRepository.findByIdAndOwnerEmailIgnoreCaseAndPublishedTrue(restaurantId, ownerEmail)
                : restaurantRepository.findByIdAndOwnerEmailIgnoreCase(restaurantId, ownerEmail))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        forPublication ? "Published restaurant not found." : "Restaurant not found."
                ));
    }

    private User requireRestaurantOwner(String ownerEmail) {
        User owner = userRepository.findByEmailIgnoreCase(ownerEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (owner.getRole() != UserRole.RESTAURANT_OWNER || !owner.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return owner;
    }

    private Pageable pageable(int page, OfferSort sort) {
        Sort ordering = switch (sort == null ? OfferSort.NEWEST : sort) {
            case PICKUP -> Sort.by(Sort.Direction.ASC, "pickupStart");
            case PRICE_LOW -> Sort.by(Sort.Direction.ASC, "discountedPrice");
            case PRICE_HIGH -> Sort.by(Sort.Direction.DESC, "discountedPrice");
            case NEWEST -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        return PageRequest.of(normalizePage(page), PAGE_SIZE, ordering);
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return "";
        }
        return search.trim();
    }

    private void expireOffers(OffsetDateTime now) {
        foodOfferRepository.expirePublishedOffers(OfferStatus.PUBLISHED, OfferStatus.EXPIRED, now);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(MARKETPLACE_ZONE);
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }
}
