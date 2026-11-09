package id.co.economz.eco_nomz.restaurant;

public interface ImageStorage {
    StoredImage upload(Long restaurantId, LogoImage image);

    void delete(String publicId);
}
