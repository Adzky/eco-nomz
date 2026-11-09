package id.co.economz.eco_nomz.order;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByCustomerId(Long customerId);

    List<CustomerOrder> findByFoodOfferRestaurantOwnerId(Long ownerId);

    @EntityGraph(attributePaths = {"foodOffer", "foodOffer.restaurant", "payment"})
    Page<CustomerOrder> findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(
            String customerEmail,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"customer", "foodOffer", "foodOffer.restaurant", "payment"})
    Page<CustomerOrder> findByFoodOfferRestaurantOwnerEmailIgnoreCaseOrderByCreatedAtDesc(
            String ownerEmail,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CustomerOrder> findByIdAndCustomerEmailIgnoreCase(Long id, String customerEmail);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CustomerOrder> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CustomerOrder> findByPickupCode(String pickupCode);
}
