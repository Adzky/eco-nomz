package id.co.economz.eco_nomz.offer;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface FoodOfferRepository extends JpaRepository<FoodOffer, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select offer from FoodOffer offer where offer.id = :id")
    Optional<FoodOffer> lockById(@Param("id") Long id);

    @EntityGraph(attributePaths = "restaurant")
    @Query("""
            select offer
            from FoodOffer offer
            join offer.restaurant restaurant
            where offer.status = :status
              and offer.quantityAvailable >= :minimumQuantity
              and offer.pickupEnd > :now
              and offer.moderated = false
              and restaurant.published = true
              and restaurant.moderated = false
              and restaurant.owner.active = true
              and (
                  :search = ''
                  or lower(offer.title) like concat('%', lower(:search), '%')
                  or lower(restaurant.name) like concat('%', lower(:search), '%')
              )
              and (:minimumPrice is null or offer.discountedPrice >= :minimumPrice)
              and (:maximumPrice is null or offer.discountedPrice <= :maximumPrice)
            """)
    Page<FoodOffer> searchAvailableOffers(
            @Param("status") OfferStatus status,
            @Param("minimumQuantity") int minimumQuantity,
            @Param("now") OffsetDateTime now,
            @Param("search") String search,
            @Param("minimumPrice") BigDecimal minimumPrice,
            @Param("maximumPrice") BigDecimal maximumPrice,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "restaurant")
    Page<FoodOffer> findByStatusAndQuantityAvailableGreaterThanAndPickupEndAfterAndModeratedFalseAndRestaurantPublishedTrueAndRestaurantModeratedFalseAndRestaurantOwnerActiveTrue(
            OfferStatus status,
            int quantityAvailable,
            OffsetDateTime now,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "restaurant")
    Optional<FoodOffer> findByIdAndStatusAndQuantityAvailableGreaterThanAndPickupEndAfterAndModeratedFalseAndRestaurantPublishedTrueAndRestaurantModeratedFalseAndRestaurantOwnerActiveTrue(
            Long id,
            OfferStatus status,
            int quantityAvailable,
            OffsetDateTime now
    );

    @EntityGraph(attributePaths = "restaurant")
    Page<FoodOffer> findByRestaurantOwnerEmailIgnoreCaseOrderByCreatedAtDesc(
            String ownerEmail,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "restaurant")
    Optional<FoodOffer> findByIdAndRestaurantOwnerEmailIgnoreCase(Long id, String ownerEmail);

    @EntityGraph(attributePaths = {"restaurant", "restaurant.owner"})
    Page<FoodOffer> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByModeratedTrue();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update FoodOffer offer
            set offer.status = :expired
            where offer.status = :published and offer.pickupEnd <= :now
            """)
    int expirePublishedOffers(
            @Param("published") OfferStatus published,
            @Param("expired") OfferStatus expired,
            @Param("now") OffsetDateTime now
    );
}
