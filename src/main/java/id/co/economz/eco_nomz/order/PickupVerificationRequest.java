package id.co.economz.eco_nomz.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PickupVerificationRequest {
    @NotBlank(message = "{validation.required}")
    @Size(max = 128, message = "{validation.maxLength}")
    private String pickupCode;

    public String getPickupCode() {
        return pickupCode;
    }

    public void setPickupCode(String pickupCode) {
        this.pickupCode = pickupCode;
    }
}
