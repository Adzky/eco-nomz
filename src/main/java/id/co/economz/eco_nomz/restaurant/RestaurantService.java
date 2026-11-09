package id.co.economz.eco_nomz.restaurant;

import java.util.List;
import id.co.economz.eco_nomz.user.User;
import id.co.economz.eco_nomz.user.UserRepository;
import id.co.economz.eco_nomz.user.UserRole;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RestaurantService {
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    public RestaurantService(RestaurantRepository restaurantRepository, UserRepository userRepository) {
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Restaurant> findPublishedRestaurants() {
        return restaurantRepository.findByPublishedTrueAndModeratedFalseAndOwnerActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Restaurant getPublishedRestaurant(Long id) {
        return restaurantRepository.findByIdAndPublishedTrueAndModeratedFalseAndOwnerActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<Restaurant> findOwnerRestaurants(String ownerEmail) {
        requireRestaurantOwner(ownerEmail);
        return restaurantRepository.findByOwnerEmailIgnoreCaseOrderByNameAsc(ownerEmail);
    }

    @Transactional(readOnly = true)
    public RestaurantForm getOwnerRestaurantForm(Long id, String ownerEmail) {
        requireRestaurantOwner(ownerEmail);
        if (id == null) {
            return new RestaurantForm();
        }
        return RestaurantForm.from(getOwnedRestaurant(id, ownerEmail));
    }

    @Transactional(readOnly = true)
    public Restaurant getOwnedRestaurant(Long id, String ownerEmail) {
        requireRestaurantOwner(ownerEmail);
        return restaurantRepository.findByIdAndOwnerEmailIgnoreCase(id, ownerEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Restaurant createRestaurant(String ownerEmail, RestaurantForm form) {
        User owner = requireRestaurantOwner(ownerEmail);
        requireUnpublishedUntilLogo(form.isPublished());
        Restaurant restaurant = new Restaurant(
                owner,
                form.getName().trim(),
                normalizeDescription(form.getDescription()),
                form.getAddress().trim(),
                form.getLatitude(),
                form.getLongitude(),
                form.isPublished()
        );
        return restaurantRepository.save(restaurant);
    }

    @Transactional
    public Restaurant updateRestaurant(Long id, String ownerEmail, RestaurantForm form) {
        Restaurant restaurant = getOwnedRestaurant(id, ownerEmail);
        requireLogoBeforePublishing(form.isPublished(), restaurant.getLogoUrl());
        restaurant.updateProfile(
                form.getName().trim(),
                normalizeDescription(form.getDescription()),
                form.getAddress().trim(),
                form.getLatitude(),
                form.getLongitude(),
                form.isPublished()
        );
        return restaurant;
    }

    @Transactional
    public void deleteRestaurant(Long id, String ownerEmail) {
        Restaurant restaurant = getOwnedRestaurant(id, ownerEmail);
        restaurantRepository.delete(restaurant);
        try {
            restaurantRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This restaurant cannot be deleted while it has associated records.",
                    exception
            );
        }
    }

    private User requireRestaurantOwner(String ownerEmail) {
        User owner = userRepository.findByEmailIgnoreCase(ownerEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (owner.getRole() != UserRole.RESTAURANT_OWNER || !owner.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return owner;
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }

    private void requireUnpublishedUntilLogo(boolean published) {
        if (published) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Save the restaurant details and upload a logo before publishing."
            );
        }
    }

    private void requireLogoBeforePublishing(boolean published, String logoUrl) {
        if (published && (logoUrl == null || logoUrl.isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Upload a restaurant logo before publishing."
            );
        }
    }
}
