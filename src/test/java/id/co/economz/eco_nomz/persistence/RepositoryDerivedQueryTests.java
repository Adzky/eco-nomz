package id.co.economz.eco_nomz.persistence;

import static org.assertj.core.api.Assertions.assertThatCode;

import id.co.economz.eco_nomz.offer.FoodOffer;
import id.co.economz.eco_nomz.restaurant.Restaurant;
import id.co.economz.eco_nomz.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.query.parser.PartTree;

class RepositoryDerivedQueryTests {
    @Test
    void parsesPublicRestaurantVisibilityQueries() {
        assertThatCode(() -> new PartTree(
                "findByIdAndPublishedTrueAndModeratedFalseAndOwnerActiveTrue",
                Restaurant.class
        )).doesNotThrowAnyException();
        assertThatCode(() -> new PartTree(
                "findByPublishedTrueAndModeratedFalseAndOwnerActiveTrueOrderByNameAsc",
                Restaurant.class
        )).doesNotThrowAnyException();
    }

    @Test
    void parsesPublicOfferVisibilityQueries() {
        assertThatCode(() -> new PartTree(
                "findByIdAndStatusAndQuantityAvailableGreaterThanAndPickupEndAfter"
                        + "AndModeratedFalseAndRestaurantPublishedTrueAndRestaurantModeratedFalse"
                        + "AndRestaurantOwnerActiveTrue",
                FoodOffer.class
        )).doesNotThrowAnyException();
        assertThatCode(() -> new PartTree(
                "findByStatusAndQuantityAvailableGreaterThanAndPickupEndAfterAndModeratedFalse"
                        + "AndRestaurantPublishedTrueAndRestaurantModeratedFalseAndRestaurantOwnerActiveTrue",
                FoodOffer.class
        )).doesNotThrowAnyException();
    }

    @Test
    void parsesAdminPaginationQueries() {
        assertThatCode(() -> new PartTree("findAllByOrderByCreatedAtDesc", User.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> new PartTree("findAllByOrderByCreatedAtDesc", Restaurant.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> new PartTree("findAllByOrderByCreatedAtDesc", FoodOffer.class))
                .doesNotThrowAnyException();
    }
}
