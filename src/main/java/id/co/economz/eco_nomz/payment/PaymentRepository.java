package id.co.economz.eco_nomz.payment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByProviderReference(String providerReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment where payment.order.id = :orderId")
    Optional<Payment> lockByOrderId(@Param("orderId") Long orderId);

    @Query("select payment from Payment payment where payment.providerReference = :providerReference")
    Optional<Payment> findByProviderReferenceForOrderLookup(
            @Param("providerReference") String providerReference
    );

    @Query("""
            select payment.order.id
            from Payment payment
            where payment.status = :status and payment.createdAt < :cutoff
            """)
    List<Long> findOrderIdsByStatusAndCreatedAtBefore(
            @Param("status") PaymentStatus status,
            @Param("cutoff") Instant cutoff
    );
}
