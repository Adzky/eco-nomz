package id.co.economz.eco_nomz.restaurant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RestaurantLogoService {
    private static final Logger logger = LoggerFactory.getLogger(RestaurantLogoService.class);

    private final RestaurantService restaurantService;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantLogoImageValidator imageValidator;
    private final ImageStorage imageStorage;

    public RestaurantLogoService(
            RestaurantService restaurantService,
            RestaurantRepository restaurantRepository,
            RestaurantLogoImageValidator imageValidator,
            ImageStorage imageStorage
    ) {
        this.restaurantService = restaurantService;
        this.restaurantRepository = restaurantRepository;
        this.imageValidator = imageValidator;
        this.imageStorage = imageStorage;
    }

    @Transactional
    public StoredImage uploadLogo(Long restaurantId, String ownerEmail, MultipartFile file) {
        Restaurant restaurant = restaurantService.getOwnedRestaurant(restaurantId, ownerEmail);
        LogoImage logo = imageValidator.validate(file);
        StoredImage nextImage = imageStorage.upload(restaurantId, logo);
        String previousPublicId = restaurant.getLogoPublicId();

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            imageStorage.delete(nextImage.publicId());
            throw new IllegalStateException("Logo uploads require an active database transaction.");
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    if (previousPublicId != null && !previousPublicId.equals(nextImage.publicId())) {
                        deleteAfterTransaction(previousPublicId, restaurantId);
                    }
                } else {
                    deleteAfterTransaction(nextImage.publicId(), restaurantId);
                }
            }
        });

        restaurant.updateLogo(nextImage.secureUrl(), nextImage.publicId());
        restaurantRepository.save(restaurant);
        return nextImage;
    }

    public void deleteStoredLogo(Long restaurantId, String publicId) {
        deleteAfterTransaction(publicId, restaurantId);
    }

    private void deleteAfterTransaction(String publicId, Long restaurantId) {
        try {
            imageStorage.delete(publicId);
        } catch (RuntimeException exception) {
            logger.error("Could not clean up Cloudinary logo {} for restaurant {}", publicId, restaurantId, exception);
        }
    }
}
