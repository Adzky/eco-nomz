package id.co.economz.eco_nomz.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import id.co.economz.eco_nomz.offer.FoodOffer;
import id.co.economz.eco_nomz.payment.Payment;
import id.co.economz.eco_nomz.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "customer_order")
public class CustomerOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_offer_id", nullable = false)
    private FoodOffer foodOffer;

    @Min(1)
    @Column(nullable = false)
    private int quantity;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Size(max = 128)
    @Column(name = "pickup_code", unique = true, length = 128)
    private String pickupCode;

    @OneToOne(mappedBy = "order", fetch = FetchType.LAZY)
    private Payment payment;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CustomerOrder() {
    }

    public CustomerOrder(
            User customer,
            FoodOffer foodOffer,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            OrderStatus status
    ) {
        this.customer = customer;
        this.foodOffer = foodOffer;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
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

    public User getCustomer() {
        return customer;
    }

    public FoodOffer getFoodOffer() {
        return foodOffer;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getPickupCode() {
        return pickupCode;
    }

    public Payment getPayment() {
        return payment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void cancel() {
        if (status != OrderStatus.CONFIRMED && status != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("This order can no longer be cancelled.");
        }
        status = OrderStatus.CANCELLED;
    }

    public boolean canBeCancelledBefore(OffsetDateTime now) {
        return status == OrderStatus.PENDING_PAYMENT && now.isBefore(foodOffer.getPickupStart());
    }

    public void assignPickupCode(String pickupCode) {
        if (this.pickupCode != null || status != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("A pickup code can only be assigned to a new pending order.");
        }
        this.pickupCode = pickupCode;
    }

    public void attachPayment(Payment payment) {
        this.payment = payment;
    }

    public void confirm() {
        if (status != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Only pending orders can be confirmed.");
        }
        status = OrderStatus.CONFIRMED;
    }

    public void expire() {
        if (status != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Only pending orders can expire.");
        }
        status = OrderStatus.EXPIRED;
    }

    public void completePickup() {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed orders can be completed.");
        }
        status = OrderStatus.COMPLETED;
    }
}
