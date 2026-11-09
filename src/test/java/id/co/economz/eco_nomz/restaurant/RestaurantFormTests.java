package id.co.economz.eco_nomz.restaurant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RestaurantFormTests {
    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void acceptsValidProfileAndOptionalCoordinates() {
        RestaurantForm form = new RestaurantForm();
        form.setName("Eco Cafe");
        form.setAddress("Jakarta");

        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void rejectsBlankNameAndOutOfRangeCoordinates() {
        RestaurantForm form = new RestaurantForm();
        form.setName(" ");
        form.setAddress("Jakarta");
        form.setLatitude(new java.math.BigDecimal("90.000001"));
        form.setLongitude(new java.math.BigDecimal("-180.000001"));

        Set<?> violations = validator.validate(form);

        assertThat(violations).hasSize(3);
    }
}
