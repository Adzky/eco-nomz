package id.co.economz.eco_nomz.restaurant;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
public class RestaurantLogoImageValidator {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_DIMENSION = 4096;

    public LogoImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an image to upload.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "The image exceeds 10 MB.");
        }

        String contentType = file.getContentType();
        if (!"image/png".equalsIgnoreCase(contentType) && !"image/jpeg".equalsIgnoreCase(contentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only PNG and JPEG images are supported.");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The image could not be read.", exception);
        }

        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (input == null) {
                throw invalidImage();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalidImage();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName();
                boolean png = "png".equalsIgnoreCase(format);
                boolean jpeg = "jpeg".equalsIgnoreCase(format) || "jpg".equalsIgnoreCase(format);
                if ((!png && !jpeg)
                        || (png && !"image/png".equalsIgnoreCase(contentType))
                        || (jpeg && !"image/jpeg".equalsIgnoreCase(contentType))) {
                    throw new ResponseStatusException(
                            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                            "The image content does not match its PNG or JPEG type."
                    );
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The image dimensions are not supported.");
                }
                if (width != height) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The logo image must be square.");
                }
                return new LogoImage(content, png ? "image/png" : "image/jpeg", png ? "png" : "jpg");
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The image is invalid or damaged.", exception);
        }
    }

    private ResponseStatusException invalidImage() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file is not a valid image.");
    }
}
