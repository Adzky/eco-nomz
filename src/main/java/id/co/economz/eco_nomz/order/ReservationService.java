package id.co.economz.eco_nomz.order;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.UUID;
import id.co.economz.eco_nomz.offer.FoodOffer;
import id.co.economz.eco_nomz.offer.FoodOfferRepository;
import id.co.economz.eco_nomz.offer.OfferStatus;
import id.co.economz.eco_nomz.payment.Payment;
import id.co.economz.eco_nomz.payment.PaymentRepository;
import id.co.economz.eco_nomz.payment.PaymentStatus;
import id.co.economz.eco_nomz.user.User;
import id.co.economz.eco_nomz.user.UserRepository;
import id.co.economz.eco_nomz.user.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReservationService {
    public static final int PAGE_SIZE = 12;

    private static final ZoneId MARKETPLACE_ZONE = ZoneId.of("Asia/Jakarta");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final CustomerOrderRepository customerOrderRepository;
    private final FoodOfferRepository foodOfferRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public ReservationService(
            CustomerOrderRepository customerOrderRepository,
            FoodOfferRepository foodOfferRepository,
            PaymentRepository paymentRepository,
            UserRepository userRepository
    ) {
        this.customerOrderRepository = customerOrderRepository;
        this.foodOfferRepository = foodOfferRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CustomerOrder reserve(String customerEmail, Long offerId, int quantity) {
        if (quantity < 1 || quantity > 1_000_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reservation quantity is invalid.");
        }
        User customer = requireRole(customerEmail, UserRole.CUSTOMER);
        OffsetDateTime now = now();
        FoodOffer offer = foodOfferRepository.lockById(offerId)
                .orElseThrow(() -> OrderConflictException.localized(
                        "orders.conflict.offerUnavailable",
                        "This offer is no longer available."
                ));
        if (offer.getStatus() != OfferStatus.PUBLISHED
                || !now.isBefore(offer.getPickupEnd())
                || !offer.getRestaurant().isPublished()) {
            throw OrderConflictException.localized(
                    "orders.conflict.offerUnavailable",
                    "This offer is no longer available."
            );
        }
        if (quantity > offer.getQuantityAvailable()) {
            throw OrderConflictException.localized(
                    "orders.conflict.itemsRemaining",
                    "Only " + offer.getQuantityAvailable() + " items remain.",
                    offer.getQuantityAvailable()
            );
        }

        BigDecimal unitPrice = offer.getDiscountedPrice();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        offer.reserveQuantity(quantity);
        CustomerOrder order = new CustomerOrder(
                customer,
                offer,
                quantity,
                unitPrice,
                totalPrice,
                OrderStatus.PENDING_PAYMENT
        );
        order.assignPickupCode(generatePickupCode());
        order = customerOrderRepository.save(order);
        Payment payment = paymentRepository.save(new Payment(
                order,
                "ECO-" + UUID.randomUUID(),
                totalPrice,
                PaymentStatus.PENDING
        ));
        order.attachPayment(payment);
        return order;
    }

    @Transactional
    public Page<CustomerOrder> findCustomerOrders(String customerEmail, int page) {
        requireRole(customerEmail, UserRole.CUSTOMER);
        return customerOrderRepository.findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(
                customerEmail,
                pageRequest(page)
        );
    }

    @Transactional
    public Page<CustomerOrder> findOwnerOrders(String ownerEmail, int page) {
        requireRole(ownerEmail, UserRole.RESTAURANT_OWNER);
        return customerOrderRepository.findByFoodOfferRestaurantOwnerEmailIgnoreCaseOrderByCreatedAtDesc(
                ownerEmail,
                pageRequest(page)
        );
    }

    @Transactional
    public CustomerOrder cancel(String customerEmail, Long orderId) {
        User customer = requireRole(customerEmail, UserRole.CUSTOMER);
        CustomerOrder order = customerOrderRepository.findByIdAndCustomerEmailIgnoreCase(orderId, customer.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        OffsetDateTime now = now();
        if (!order.canBeCancelledBefore(now)) {
            throw OrderConflictException.localized(
                    "orders.conflict.cannotCancel",
                    "This order can no longer be cancelled."
            );
        }

        Payment payment = paymentRepository.lockByOrderId(orderId).orElse(null);
        if (payment == null && order.getPickupCode() != null) {
            throw new IllegalStateException("Reservation payment was not found.");
        }
        if (payment != null) {
            if (payment.getStatus() != PaymentStatus.PENDING) {
                throw OrderConflictException.localized(
                        "orders.conflict.onlyUnpaidCanCancel",
                        "Only unpaid reservations can be cancelled online."
                );
            }
            if (payment.hasCheckoutUrl()) {
                throw OrderConflictException.localized(
                        "orders.conflict.checkoutAlreadyOpened",
                        "This checkout has been opened. Cancel it with Midtrans or wait for it to expire."
                );
            }
            payment.cancel();
        }
        FoodOffer offer = foodOfferRepository.lockById(order.getFoodOffer().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        order.cancel();
        offer.restoreReservedQuantity(order.getQuantity(), now);
        return order;
    }

    private String generatePickupCode() {
        byte[] code = new byte[12];
        SECURE_RANDOM.nextBytes(code);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(code);
    }

    private Pageable pageRequest(int page) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must not be negative.");
        }
        return PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private User requireRole(String email, UserRole role) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (user.getRole() != role) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return user;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(MARKETPLACE_ZONE);
    }
}
