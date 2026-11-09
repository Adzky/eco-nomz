package id.co.economz.eco_nomz.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MidtransSnapResponse(
        @JsonProperty("token") String token,
        @JsonProperty("redirect_url") String redirectUrl
) {
}
