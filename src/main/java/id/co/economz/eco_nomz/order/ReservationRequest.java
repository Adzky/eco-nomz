package id.co.economz.eco_nomz.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ReservationRequest {
    @NotNull(message = "{validation.required}")
    private Long offerId;

    @Min(value = 1, message = "{validation.min}")
    @Max(value = 1000000, message = "{validation.max}")
    private int quantity;

    public Long getOfferId() {
        return offerId;
    }

    public void setOfferId(Long offerId) {
        this.offerId = offerId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
