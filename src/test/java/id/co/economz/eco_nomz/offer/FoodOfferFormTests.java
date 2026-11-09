package id.co.economz.eco_nomz.offer;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FoodOfferFormTests {
    private static ValidatorFactory validatorFactory;
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
    void acceptsValidOffer() {
        FoodOfferForm form = validForm();

        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void rejectsInvalidPriceQuantityAndPickupWindow() {
        FoodOfferForm form = validForm();
        form.setOriginalPrice(new BigDecimal("10000"));
        form.setDiscountedPrice(new BigDecimal("10000"));
        form.setQuantityAvailable(0);
        form.setPickupEnd(form.getPickupStart());

        Set<?> violations = validator.validate(form);

        assertThat(violations).hasSize(3);
    }

    private FoodOfferForm validForm() {
        FoodOfferForm form = new FoodOfferForm();
        form.setRestaurantId(1L);
        form.setTitle("Surprise food bag");
        form.setOriginalPrice(new BigDecimal("50000"));
        form.setDiscountedPrice(new BigDecimal("25000"));
        form.setQuantityAvailable(4);
        form.setPickupStart(LocalDateTime.now().plusDays(1));
        form.setPickupEnd(LocalDateTime.now().plusDays(1).plusHours(2));
        return form;
    }
}
