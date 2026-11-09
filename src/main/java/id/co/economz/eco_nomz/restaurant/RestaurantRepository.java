package id.co.economz.eco_nomz.restaurant;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    List<Restaurant> findByOwnerEmailIgnoreCaseOrderByNameAsc(String ownerEmail);

    List<Restaurant> findByOwnerEmailIgnoreCaseAndPublishedTrueOrderByNameAsc(String ownerEmail);

    Optional<Restaurant> findByIdAndOwnerEmailIgnoreCase(Long id, String ownerEmail);

    Optional<Restaurant> findByIdAndOwnerEmailIgnoreCaseAndPublishedTrue(Long id, String ownerEmail);

    Optional<Restaurant> findByIdAndPublishedTrueAndModeratedFalseAndOwnerActiveTrue(Long id);

    List<Restaurant> findByPublishedTrueAndModeratedFalseAndOwnerActiveTrueOrderByNameAsc();

    @EntityGraph(attributePaths = "owner")
    Page<Restaurant> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByModeratedTrue();
}
