package id.co.economz.eco_nomz.restaurant;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CloudinaryImageStorage implements ImageStorage {
    private static final Logger logger = LoggerFactory.getLogger(CloudinaryImageStorage.class);
    private static final String API_BASE = "https://api.cloudinary.com/v1_1/";
    private static final String LOGO_FOLDER = "eco-nomz/restaurant-logos";

    private final RestClient restClient;
    private final Clock clock;
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    CloudinaryImageStorage(
            RestClient restClient,
            Clock clock,
            String cloudName,
            String apiKey,
            String apiSecret
    ) {
        this.restClient = restClient;
        this.clock = clock;
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    @Autowired
    public CloudinaryImageStorage(
            @Value("${app.cloudinary.cloud-name:}") String cloudName,
            @Value("${app.cloudinary.api-key:}") String apiKey,
            @Value("${app.cloudinary.api-secret:}") String apiSecret
    ) {
        this(RestClient.create(), Clock.systemUTC(), cloudName, apiKey, apiSecret);
    }

    @Override
    public StoredImage upload(Long restaurantId, LogoImage image) {
        requireConfiguration();
        String folder = LOGO_FOLDER + "/" + restaurantId;
        String requestedPublicId = UUID.randomUUID().toString();
        long timestamp = clock.instant().getEpochSecond();
        Map<String, String> parameters = Map.of(
                "folder", folder,
                "public_id", requestedPublicId,
                "timestamp", Long.toString(timestamp)
        );
        String signature = sign(parameters);

        ByteArrayResource fileResource = new ByteArrayResource(image.content()) {
            @Override
            public String getFilename() {
                return "restaurant-logo." + image.extension();
            }
        };
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.parseMediaType(image.contentType()));
        fileHeaders.setContentDispositionFormData("file", fileResource.getFilename());

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new HttpEntity<>(fileResource, fileHeaders));
        form.add("api_key", apiKey);
        form.add("timestamp", Long.toString(timestamp));
        form.add("folder", folder);
        form.add("public_id", requestedPublicId);
        form.add("signature", signature);

        CloudinaryUploadResponse response;
        try {
            response = restClient.post()
                    .uri(API_BASE + cloudName + "/image/upload")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve()
                    .body(CloudinaryUploadResponse.class);
        } catch (RestClientException exception) {
            logger.error("Cloudinary logo upload failed for restaurant {}", restaurantId, exception);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "The logo could not be stored by the image provider.",
                    exception
            );
        }
        if (response == null || response.secureUrl() == null || response.publicId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "The image provider returned an incomplete logo response."
            );
        }
        if (!response.secureUrl().startsWith("https://res.cloudinary.com/")) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The image provider returned an invalid logo URL.");
        }
        return new StoredImage(response.secureUrl(), response.publicId());
    }

    @Override
    public void delete(String publicId) {
        requireConfiguration();
        long timestamp = clock.instant().getEpochSecond();
        Map<String, String> parameters = Map.of(
                "invalidate", "true",
                "public_id", publicId,
                "timestamp", Long.toString(timestamp)
        );
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("api_key", apiKey);
        form.add("timestamp", Long.toString(timestamp));
        form.add("public_id", publicId);
        form.add("invalidate", "true");
        form.add("signature", sign(parameters));

        try {
            ResponseEntity<Map> response = restClient.post()
                    .uri(API_BASE + cloudName + "/image/destroy")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toEntity(Map.class);
            if (response.getBody() == null || !"ok".equals(response.getBody().get("result"))) {
                throw new IllegalStateException("Cloudinary did not confirm logo deletion.");
            }
        } catch (RestClientException exception) {
            logger.error("Cloudinary logo deletion failed for asset {}", publicId, exception);
            throw new IllegalStateException("Cloudinary logo deletion failed.", exception);
        }
    }

    private String sign(Map<String, String> parameters) {
        TreeMap<String, String> sortedParameters = new TreeMap<>(parameters);
        String canonical = sortedParameters.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(java.util.stream.Collectors.joining("&"));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1")
                    .digest((canonical + apiSecret).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 is unavailable for Cloudinary request signing.", exception);
        }
    }

    private void requireConfiguration() {
        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Cloudinary image storage is not configured."
            );
        }
        if (!cloudName.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalStateException("The configured Cloudinary cloud name is invalid.");
        }
    }

    private record CloudinaryUploadResponse(
            @JsonProperty("secure_url") String secureUrl,
            @JsonProperty("public_id") String publicId
    ) {
    }
}
