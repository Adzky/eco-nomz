package id.co.economz.eco_nomz.restaurant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class RestaurantLogoImageValidatorTests {
    private final RestaurantLogoImageValidator validator = new RestaurantLogoImageValidator();

    @Test
    void acceptsSquarePng() throws Exception {
        LogoImage result = validator.validate(image("logo.png", "image/png", 32, 32));

        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(result.extension()).isEqualTo("png");
        assertThat(result.content()).isNotEmpty();
    }

    @Test
    void rejectsNonSquareImage() throws Exception {
        assertThatThrownBy(() -> validator.validate(image("logo.png", "image/png", 48, 32)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void rejectsContentThatDoesNotMatchItsDeclaredImageType() {
        MockMultipartFile file = new MockMultipartFile(
                "logo",
                "logo.png",
                "image/png",
                "not a PNG".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void rejectsUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "logo",
                "logo.svg",
                "image/svg+xml",
                "<svg/>".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode().value()).isEqualTo(415));
    }

    private MockMultipartFile image(String name, String contentType, int width, int height) throws Exception {
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", output);
        return new MockMultipartFile("logo", name, contentType, output.toByteArray());
    }
}
