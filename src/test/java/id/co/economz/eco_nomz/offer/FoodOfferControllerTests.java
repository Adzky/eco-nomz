package id.co.economz.eco_nomz.offer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

class FoodOfferControllerTests {
    private FoodOfferService foodOfferService;
    private FoodOfferController controller;

    @BeforeEach
    void setUp() {
        foodOfferService = org.mockito.Mockito.mock(FoodOfferService.class);
        controller = new FoodOfferController(foodOfferService);
    }

    @Test
    void rejectsPriceRangeWhereMinimumExceedsMaximum() {
        assertThatThrownBy(() -> controller.publicOffers(
                0,
                OfferSort.NEWEST,
                null,
                new BigDecimal("50000"),
                new BigDecimal("10000"),
                1,
                new ExtendedModelMap()
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");

        verifyNoInteractions(foodOfferService);
    }

    @Test
    void rejectsInvalidAvailabilityFilter() {
        assertThatThrownBy(() -> controller.publicOffers(
                0,
                OfferSort.NEWEST,
                null,
                null,
                null,
                0,
                new ExtendedModelMap()
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");

        verifyNoInteractions(foodOfferService);
    }

    @Test
    void acceptsValidFiltersAndReturnsMarketplaceView() {
        var model = new ExtendedModelMap();

        String view = controller.publicOffers(
                1,
                OfferSort.PRICE_LOW,
                "  nasi  ",
                new BigDecimal("10000"),
                new BigDecimal("50000"),
                2,
                model
        );

        assertThat(view).isEqualTo("offers/list");
        verify(foodOfferService).findAvailableOffers(
                1,
                OfferSort.PRICE_LOW,
                "  nasi  ",
                new BigDecimal("10000"),
                new BigDecimal("50000"),
                2
        );
        assertThat(model.getAttribute("q")).isEqualTo("  nasi  ");
        assertThat(model.getAttribute("minQuantity")).isEqualTo(2);
    }
}
