package id.co.economz.eco_nomz.restaurant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import id.co.economz.eco_nomz.user.User;
import id.co.economz.eco_nomz.user.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class RestaurantLogoServiceTests {
    @Mock
    private RestaurantService restaurantService;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private RestaurantLogoImageValidator imageValidator;

    @Mock
    private ImageStorage imageStorage;

    private RestaurantLogoService restaurantLogoService;

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void storesTheLogoAndRemovesThePreviousCloudinaryAssetAfterCommit() {
        restaurantLogoService = new RestaurantLogoService(
                restaurantService,
                restaurantRepository,
                imageValidator,
                imageStorage
        );
        User owner = new User("Owner", "owner@example.com", "password-hash", UserRole.RESTAURANT_OWNER);
        Restaurant restaurant = new Restaurant(owner, "Green Cafe", null, "Jakarta");
        restaurant.updateLogo("https://res.cloudinary.com/demo/old.png", "old-public-id");
        LogoImage validatedImage = new LogoImage(new byte[] {1}, "image/png", "png");
        StoredImage uploadedImage = new StoredImage(
                "https://res.cloudinary.com/demo/new.png",
                "new-public-id"
        );
        when(restaurantService.getOwnedRestaurant(8L, owner.getEmail())).thenReturn(restaurant);
        when(imageValidator.validate(org.mockito.ArgumentMatchers.any())).thenReturn(validatedImage);
        when(imageStorage.upload(8L, validatedImage)).thenReturn(uploadedImage);
        TransactionSynchronizationManager.initSynchronization();

        StoredImage result = restaurantLogoService.uploadLogo(
                8L,
                owner.getEmail(),
                new MockMultipartFile("logo", "logo.png", "image/png", new byte[] {1})
        );

        assertThat(result).isEqualTo(uploadedImage);
        assertThat(restaurant.getLogoUrl()).isEqualTo(uploadedImage.secureUrl());
        assertThat(restaurant.getLogoPublicId()).isEqualTo(uploadedImage.publicId());
        verify(restaurantRepository).save(restaurant);
        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(synchronization ->
                synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        verify(imageStorage).delete("old-public-id");
    }
}
