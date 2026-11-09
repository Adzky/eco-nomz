package id.co.economz.eco_nomz.offer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class FoodOfferForm {
    private static final ZoneId MARKETPLACE_ZONE = ZoneId.of("Asia/Jakarta");

    @NotNull(message = "{validation.required}")
    private Long restaurantId;

    @NotBlank(message = "{validation.required}")
    @Size(max = 160, message = "{validation.maxLength}")
    private String title;

    @Size(max = 10000, message = "{validation.maxLength}")
    private String description;

    @NotNull(message = "{validation.required}")
    @DecimalMin(value = "0.00", inclusive = false, message = "{validation.decimalMinExclusive}")
    @DecimalMax(value = "9999999999.99", message = "{validation.decimalMax}")
    private BigDecimal originalPrice;

    @NotNull(message = "{validation.required}")
    @DecimalMin(value = "0.00", inclusive = false, message = "{validation.decimalMinExclusive}")
    @DecimalMax(value = "9999999999.99", message = "{validation.decimalMax}")
    private BigDecimal discountedPrice;

    @Min(value = 1, message = "{validation.min}")
    private int quantityAvailable;

    @NotNull(message = "{validation.required}")
    private LocalDateTime pickupStart;

    @NotNull(message = "{validation.required}")
    private LocalDateTime pickupEnd;

    private boolean published;

    @AssertTrue(message = "{validation.discountMustBeLower}")
    public boolean isDiscountValid() {
        return originalPrice == null
                || discountedPrice == null
                || (discountedPrice.signum() > 0 && discountedPrice.compareTo(originalPrice) < 0);
    }

    @AssertTrue(message = "{validation.pickupEndAfterStart}")
    public boolean isPickupWindowValid() {
        return pickupStart == null || pickupEnd == null || pickupEnd.isAfter(pickupStart);
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public BigDecimal getDiscountedPrice() {
        return discountedPrice;
    }

    public void setDiscountedPrice(BigDecimal discountedPrice) {
        this.discountedPrice = discountedPrice;
    }

    public int getQuantityAvailable() {
        return quantityAvailable;
    }

    public void setQuantityAvailable(int quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }

    public LocalDateTime getPickupStart() {
        return pickupStart;
    }

    public void setPickupStart(LocalDateTime pickupStart) {
        this.pickupStart = pickupStart;
    }

    public LocalDateTime getPickupEnd() {
        return pickupEnd;
    }

    public void setPickupEnd(LocalDateTime pickupEnd) {
        this.pickupEnd = pickupEnd;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    OffsetDateTime pickupStartAtMarketZone() {
        return pickupStart.atZone(MARKETPLACE_ZONE).toOffsetDateTime();
    }

    OffsetDateTime pickupEndAtMarketZone() {
        return pickupEnd.atZone(MARKETPLACE_ZONE).toOffsetDateTime();
    }

    static FoodOfferForm from(FoodOffer offer) {
        FoodOfferForm form = new FoodOfferForm();
        form.setRestaurantId(offer.getRestaurant().getId());
        form.setTitle(offer.getTitle());
        form.setDescription(offer.getDescription());
        form.setOriginalPrice(offer.getOriginalPrice());
        form.setDiscountedPrice(offer.getDiscountedPrice());
        form.setQuantityAvailable(offer.getQuantityAvailable());
        form.setPickupStart(offer.getPickupStart().atZoneSameInstant(MARKETPLACE_ZONE).toLocalDateTime());
        form.setPickupEnd(offer.getPickupEnd().atZoneSameInstant(MARKETPLACE_ZONE).toLocalDateTime());
        form.setPublished(offer.getStatus() == OfferStatus.PUBLISHED);
        return form;
    }
}
