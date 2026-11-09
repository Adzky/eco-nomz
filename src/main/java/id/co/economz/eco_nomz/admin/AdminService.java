package id.co.economz.eco_nomz.admin;

import id.co.economz.eco_nomz.offer.FoodOffer;
import id.co.economz.eco_nomz.offer.FoodOfferRepository;
import id.co.economz.eco_nomz.restaurant.Restaurant;
import id.co.economz.eco_nomz.restaurant.RestaurantRepository;
import id.co.economz.eco_nomz.user.User;
import id.co.economz.eco_nomz.user.UserRepository;
import id.co.economz.eco_nomz.user.UserRole;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminService {
    public static final int PAGE_SIZE = 25;

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminService.class);
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final FoodOfferRepository foodOfferRepository;

    public AdminService(
            UserRepository userRepository,
            RestaurantRepository restaurantRepository,
            FoodOfferRepository foodOfferRepository
    ) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.foodOfferRepository = foodOfferRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardSummary dashboard(String adminEmail) {
        requireAdministrator(adminEmail);
        return new AdminDashboardSummary(
                userRepository.count(),
                restaurantRepository.count(),
                foodOfferRepository.count(),
                restaurantRepository.countByModeratedTrue(),
                foodOfferRepository.countByModeratedTrue()
        );
    }

    @Transactional(readOnly = true)
    public Page<AdminUserSummary> users(String adminEmail, int page) {
        requireAdministrator(adminEmail);
        return userRepository.findAllByOrderByCreatedAtDesc(pageRequest(page))
                .map(user -> new AdminUserSummary(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole(),
                        user.isActive(),
                        user.getCreatedAt()
                ));
    }

    @Transactional(readOnly = true)
    public Page<AdminRestaurantSummary> restaurants(String adminEmail, int page) {
        requireAdministrator(adminEmail);
        return restaurantRepository.findAllByOrderByCreatedAtDesc(pageRequest(page))
                .map(restaurant -> new AdminRestaurantSummary(
                        restaurant.getId(),
                        restaurant.getName(),
                        restaurant.getOwner().getName(),
                        restaurant.getOwner().getEmail(),
                        restaurant.isPublished(),
                        restaurant.isModerated(),
                        restaurant.getCreatedAt()
                ));
    }

    @Transactional(readOnly = true)
    public Page<AdminOfferSummary> offers(String adminEmail, int page) {
        requireAdministrator(adminEmail);
        return foodOfferRepository.findAllByOrderByCreatedAtDesc(pageRequest(page))
                .map(offer -> new AdminOfferSummary(
                        offer.getId(),
                        offer.getTitle(),
                        offer.getRestaurant().getName(),
                        offer.getRestaurant().getOwner().getEmail(),
                        offer.getStatus(),
                        offer.getQuantityAvailable(),
                        offer.getPickupEnd(),
                        offer.isModerated(),
                        offer.getModerationNote(),
                        offer.getCreatedAt()
                ));
    }

    @Transactional
    public void setUserActive(String adminEmail, Long userId, boolean active) {
        User administrator = requireAdministrator(adminEmail);
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (target.getRole() == UserRole.ADMIN || Objects.equals(target.getId(), administrator.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Administrator accounts cannot be changed here."
            );
        }
        target.setActive(active);
        LOGGER.info(
                "Administrator user-status change actorId={} targetUserId={} active={}",
                administrator.getId(),
                target.getId(),
                active
        );
    }

    @Transactional
    public void setRestaurantModerated(String adminEmail, Long restaurantId, boolean moderated) {
        User administrator = requireAdministrator(adminEmail);
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        restaurant.setModerated(moderated);
        LOGGER.info(
                "Administrator restaurant-moderation change actorId={} restaurantId={} moderated={}",
                administrator.getId(),
                restaurantId,
                moderated
        );
    }

    @Transactional
    public void setOfferModerated(String adminEmail, Long offerId, boolean moderated, String note) {
        User administrator = requireAdministrator(adminEmail);
        FoodOffer offer = foodOfferRepository.findById(offerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String normalizedNote = note == null || note.isBlank() ? null : note.trim();
        if (normalizedNote != null && normalizedNote.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Moderation notes must be 500 characters or fewer.");
        }
        offer.moderate(moderated, normalizedNote);
        LOGGER.info(
                "Administrator offer-moderation change actorId={} offerId={} moderated={}",
                administrator.getId(),
                offerId,
                moderated
        );
    }

    private User requireAdministrator(String adminEmail) {
        User administrator = userRepository.findByEmailIgnoreCase(adminEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (administrator.getRole() != UserRole.ADMIN || !administrator.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return administrator;
    }

    private PageRequest pageRequest(int page) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must not be negative.");
        }
        return PageRequest.of(page, PAGE_SIZE);
    }

    public record AdminDashboardSummary(
            long users,
            long restaurants,
            long offers,
            long moderatedRestaurants,
            long moderatedOffers
    ) {
    }
}
