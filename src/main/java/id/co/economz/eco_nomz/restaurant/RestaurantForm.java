package id.co.economz.eco_nomz.restaurant;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RestaurantForm {
    @NotBlank(message = "{validation.required}")
    @Size(max = 160, message = "{validation.maxLength}")
    private String name;

    @Size(max = 10000, message = "{validation.maxLength}")
    private String description;

    @NotBlank(message = "{validation.required}")
    @Size(max = 500, message = "{validation.maxLength}")
    private String address;

    @DecimalMin(value = "-90.0", message = "{validation.decimalMin}")
    @DecimalMax(value = "90.0", message = "{validation.decimalMax}")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "{validation.decimalMin}")
    @DecimalMax(value = "180.0", message = "{validation.decimalMax}")
    private BigDecimal longitude;

    private boolean published;
    private String logoUrl;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    static RestaurantForm from(Restaurant restaurant) {
        RestaurantForm form = new RestaurantForm();
        form.setName(restaurant.getName());
        form.setDescription(restaurant.getDescription());
        form.setAddress(restaurant.getAddress());
        form.setLatitude(restaurant.getLatitude());
        form.setLongitude(restaurant.getLongitude());
        form.setPublished(restaurant.isPublished());
        form.setLogoUrl(restaurant.getLogoUrl());
        return form;
    }
}
