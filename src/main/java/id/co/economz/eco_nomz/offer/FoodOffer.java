package id.co.economz.eco_nomz.offer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import id.co.economz.eco_nomz.restaurant.Restaurant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "food_offer")
public class FoodOffer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @NotBlank
    @Size(max = 160)
    @Column(nullable = false, length = 160)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Column(name = "original_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal originalPrice;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Column(name = "discounted_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountedPrice;

    @Min(0)
    @Column(name = "quantity_available", nullable = false)
    private int quantityAvailable;

    @NotNull
    @Column(name = "pickup_start", nullable = false)
    private OffsetDateTime pickupStart;

    @NotNull
    @Column(name = "pickup_end", nullable = false)
    private OffsetDateTime pickupEnd;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OfferStatus status;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @NotNull
    @Column(nullable = false)
    private boolean moderated;

    @Size(max = 500)
    @Column(name = "moderation_note", length = 500)
    private String moderationNote;

    protected FoodOffer() {
    }

    public FoodOffer(
            Restaurant restaurant,
            String title,
            String description,
            BigDecimal originalPrice,
            BigDecimal discountedPrice,
            int quantityAvailable,
            OffsetDateTime pickupStart,
            OffsetDateTime pickupEnd,
            OfferStatus status
    ) {
        this.restaurant = restaurant;
        this.title = title;
        this.description = description;
        this.originalPrice = originalPrice;
        this.discountedPrice = discountedPrice;
        this.quantityAvailable = quantityAvailable;
        this.pickupStart = pickupStart;
        this.pickupEnd = pickupEnd;
        this.status = status;
    }

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public BigDecimal getDiscountedPrice() {
        return discountedPrice;
    }

    public int getQuantityAvailable() {
        return quantityAvailable;
    }

    public OffsetDateTime getPickupStart() {
        return pickupStart;
    }

    public OffsetDateTime getPickupEnd() {
        return pickupEnd;
    }

    public OfferStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isModerated() {
        return moderated;
    }

    public String getModerationNote() {
        return moderationNote;
    }

    public void updateOffer(
            Restaurant restaurant,
            String title,
            String description,
            BigDecimal originalPrice,
            BigDecimal discountedPrice,
            int quantityAvailable,
            OffsetDateTime pickupStart,
            OffsetDateTime pickupEnd,
            boolean publish
    ) {
        this.restaurant = restaurant;
        this.title = title;
        this.description = description;
        this.originalPrice = originalPrice;
        this.discountedPrice = discountedPrice;
        this.quantityAvailable = quantityAvailable;
        this.pickupStart = pickupStart;
        this.pickupEnd = pickupEnd;
        if (status != OfferStatus.EXPIRED && status != OfferStatus.SOLD_OUT) {
            this.status = publish ? OfferStatus.PUBLISHED : OfferStatus.DRAFT;
        }
    }

    public void reserveQuantity(int quantity) {
        if (quantity < 1 || quantity > quantityAvailable || status != OfferStatus.PUBLISHED) {
            throw new IllegalStateException("Reservation quantity is not available.");
        }
        quantityAvailable -= quantity;
        if (quantityAvailable == 0) {
            status = OfferStatus.SOLD_OUT;
        }
    }

    public void restoreReservedQuantity(int quantity, OffsetDateTime now) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Restored quantity must be positive.");
        }
        quantityAvailable = Math.addExact(quantityAvailable, quantity);
        if (!pickupEnd.isAfter(now)) {
            status = OfferStatus.EXPIRED;
        } else {
            status = restaurant.isPublished() ? OfferStatus.PUBLISHED : OfferStatus.DRAFT;
        }
    }

    public void moderate(boolean moderated, String note) {
        this.moderated = moderated;
        this.moderationNote = moderated ? note : null;
    }
}
