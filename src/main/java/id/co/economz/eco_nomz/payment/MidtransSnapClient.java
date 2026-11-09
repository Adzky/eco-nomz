package id.co.economz.eco_nomz.payment;

import java.time.OffsetDateTime;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class MidtransSnapClient implements MidtransGateway {
    private static final DateTimeFormatter MIDTRANS_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss xx");
    private static final Duration PAYMENT_WINDOW = Duration.ofMinutes(15);

    private final RestClient restClient;
    private final MidtransProperties properties;

    public MidtransSnapClient(MidtransProperties properties) {
        this.restClient = RestClient.builder().baseUrl(properties.getBaseUrl()).build();
        this.properties = properties;
    }

    @Override
    public MidtransSnapResponse createTransaction(Payment payment) {
        properties.requireServerKey();
        var order = payment.getOrder();
        var offer = order.getFoodOffer();
        int remainingMinutes = payment.getCreatedAt() == null
                ? 15
                : Math.max(1, (int) Math.ceil(Duration.between(
                        Instant.now(),
                        payment.getCreatedAt().plus(PAYMENT_WINDOW)
                ).toSeconds() / 60.0));
        Map<String, Object> request = Map.of(
                "transaction_details", Map.of(
                        "order_id", payment.getProviderReference(),
                        "gross_amount", payment.getAmount()
                ),
                "item_details", List.of(Map.of(
                        "id", offer.getId().toString(),
                        "price", order.getUnitPrice(),
                        "quantity", order.getQuantity(),
                        "name", offer.getTitle().substring(0, Math.min(offer.getTitle().length(), 50))
                )),
                "expiry", Map.of(
                        "start_time", OffsetDateTime.now(ZoneId.of("Asia/Jakarta")).format(MIDTRANS_TIME),
                        "unit", "minutes",
                        "duration", remainingMinutes
                ),
                "callbacks", Map.of(
                        "finish", properties.getPublicBaseUrl() + "/customer/orders"
                )
        );

        try {
            MidtransSnapResponse response = restClient.post()
                    .uri("/snap/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBasicAuth(properties.getServerKey(), ""))
                    .body(request)
                    .retrieve()
                    .body(MidtransSnapResponse.class);
            if (response == null || response.token() == null || response.redirectUrl() == null) {
                throw new MidtransIntegrationException("Midtrans returned an incomplete checkout response.");
            }
            return response;
        } catch (RestClientException exception) {
            throw new MidtransIntegrationException("Could not create a Midtrans Sandbox checkout.", exception);
        }
    }
}
