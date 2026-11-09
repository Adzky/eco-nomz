package id.co.economz.eco_nomz.payment;

import java.math.BigDecimal;
import java.time.Instant;
import id.co.economz.eco_nomz.order.CustomerOrder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "payment")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private CustomerOrder order;

    @Size(max = 255)
    @Column(name = "provider_reference", unique = true, length = 255)
    private String providerReference;

    @Size(max = 255)
    @Column(name = "provider_transaction_id", unique = true, length = 255)
    private String providerTransactionId;

    @Size(max = 255)
    @Column(name = "checkout_url", length = 255)
    private String checkoutUrl;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentStatus status;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Payment() {
    }

    public Payment(CustomerOrder order, String providerReference, BigDecimal amount, PaymentStatus status) {
        this.order = order;
        this.providerReference = providerReference;
        this.amount = amount;
        this.status = status;
    }

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public CustomerOrder getOrder() {
        return order;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public String getProviderTransactionId() {
        return providerTransactionId;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void succeed() {
        transitionFromPending(PaymentStatus.SUCCEEDED);
    }

    public void fail() {
        transitionFromPending(PaymentStatus.FAILED);
    }

    public void cancel() {
        if (status == PaymentStatus.PENDING) {
            status = PaymentStatus.CANCELLED;
        } else {
            throw new IllegalStateException("Only pending payments can be cancelled.");
        }
    }

    public void expire() {
        transitionFromPending(PaymentStatus.EXPIRED);
    }

    public void completeCheckout(String checkoutUrl, String providerTransactionId) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can start checkout.");
        }
        this.checkoutUrl = checkoutUrl;
        if (providerTransactionId != null && !providerTransactionId.isBlank()) {
            this.providerTransactionId = providerTransactionId;
        }
    }

    public boolean hasCheckoutUrl() {
        return checkoutUrl != null && !checkoutUrl.isBlank();
    }

    public void applyProviderTransactionId(String transactionId) {
        if (transactionId != null && !transactionId.isBlank()) {
            this.providerTransactionId = transactionId;
        }
    }

    public void requireManualReview() {
        if (status != PaymentStatus.PENDING && status != PaymentStatus.EXPIRED) {
            throw new IllegalStateException("Only pending or expired payments can require manual review.");
        }
        status = PaymentStatus.REVIEW_REQUIRED;
    }

    private void transitionFromPending(PaymentStatus nextStatus) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can change status.");
        }
        status = nextStatus;
    }
}
