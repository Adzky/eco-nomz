package id.co.economz.eco_nomz.restaurant;

import java.math.BigDecimal;
import java.time.Instant;
import id.co.economz.eco_nomz.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "restaurant")
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @NotBlank
    @Size(max = 160)
    @Column(nullable = false, length = 160)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String address;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(nullable = false)
    private boolean published;

    @Size(max = 1000)
    @Column(name = "logo_url", length = 1000)
    private String logoUrl;

    @Size(max = 255)
    @Column(name = "logo_public_id", length = 255)
    private String logoPublicId;

    @NotNull
    @Column(nullable = false)
    private boolean moderated;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Restaurant() {
    }

    public Restaurant(User owner, String name, String description, String address) {
        this.owner = owner;
        this.name = name;
        this.description = description;
        this.address = address;
    }

    public Restaurant(
            User owner,
            String name,
            String description,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            boolean published
    ) {
        this(owner, name, description, address);
        this.latitude = latitude;
        this.longitude = longitude;
        this.published = published;
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

    public User getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAddress() {
        return address;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public boolean isPublished() {
        return published;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getLogoPublicId() {
        return logoPublicId;
    }

    public boolean isModerated() {
        return moderated;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void updateProfile(
            String name,
            String description,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            boolean published
    ) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.published = published;
    }

    public void setModerated(boolean moderated) {
        this.moderated = moderated;
    }

    public void updateLogo(String logoUrl, String logoPublicId) {
        this.logoUrl = logoUrl;
        this.logoPublicId = logoPublicId;
    }
}
