package id.co.economz.eco_nomz.restaurant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class CloudinaryImageStorageTests {
    @Test
    void uploadsWithServerCredentialsAndReturnsTheSecureCloudinaryAsset() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.cloudinary.com/v1_1/demo-cloud/image/upload"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"secure_url\":\"https://res.cloudinary.com/demo-cloud/image/upload/logo.png\","
                                + "\"public_id\":\"eco-nomz/restaurant-logos/8/logo\"}",
                        MediaType.APPLICATION_JSON
                ));
        CloudinaryImageStorage storage = new CloudinaryImageStorage(
                builder.build(),
                Clock.fixed(Instant.parse("2026-11-05T00:00:00Z"), ZoneOffset.UTC),
                "demo-cloud",
                "api-key",
                "api-secret"
        );

        StoredImage result = storage.upload(8L, new LogoImage(new byte[] {1}, "image/png", "png"));

        assertThat(result.secureUrl()).startsWith("https://res.cloudinary.com/");
        assertThat(result.publicId()).isEqualTo("eco-nomz/restaurant-logos/8/logo");
        server.verify();
    }

    @Test
    void failsClearlyWhenCloudinaryCredentialsAreMissing() {
        CloudinaryImageStorage storage = new CloudinaryImageStorage(
                RestClient.create(),
                Clock.systemUTC(),
                "",
                "",
                ""
        );

        assertThatThrownBy(() -> storage.upload(8L, new LogoImage(new byte[] {1}, "image/png", "png")))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode().value()).isEqualTo(503));
    }
}
