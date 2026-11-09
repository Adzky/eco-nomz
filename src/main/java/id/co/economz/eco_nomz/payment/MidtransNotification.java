package id.co.economz.eco_nomz.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MidtransNotification(
        @JsonProperty("order_id") String orderId,
        @JsonProperty("status_code") String statusCode,
        @JsonProperty("gross_amount") String grossAmount,
        @JsonProperty("signature_key") String signatureKey,
        @JsonProperty("transaction_status") String transactionStatus,
        @JsonProperty("fraud_status") String fraudStatus,
        @JsonProperty("transaction_id") String transactionId,
        @JsonProperty("currency") String currency
) {
}
