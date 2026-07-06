package tacos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TacoOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_uuid", nullable = false, unique = true, updatable = false)
    private UUID orderUuid;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Embedded
    private DeliveryAddress deliveryAddress;

    @Column(length = 500)
    private String comment;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "cc_number", nullable = false, length = 32)
    private String ccNumber;

    @Column(name = "cc_expiration", nullable = false, length = 5)
    private String ccExpiration;

    @Column(name = "cc_cvv", nullable = false, length = 4)
    private String ccCVV;

    @OneToMany
    @JoinTable(
            name = "order_items",
            joinColumns = @JoinColumn(name = "order_id"),
            inverseJoinColumns = @JoinColumn(name = "taco_id"))
    private List<Taco> tacos = new ArrayList<>();

    public static TacoOrder create(
            User user,
            DeliveryAddress deliveryAddress,
            String comment,
            String ccNumber,
            String ccExpiration,
            String ccCVV,
            List<Taco> tacos,
            BigDecimal totalPrice) {
        TacoOrder order = new TacoOrder();
        order.orderUuid = UUID.randomUUID();
        order.status = OrderStatus.CREATED;
        order.user = Objects.requireNonNull(user, "user is required");
        order.deliveryAddress = Objects.requireNonNull(deliveryAddress, "deliveryAddress is required");
        order.comment = comment;
        order.ccNumber = Objects.requireNonNull(ccNumber, "ccNumber is required");
        order.ccExpiration = Objects.requireNonNull(ccExpiration, "ccExpiration is required");
        order.ccCVV = Objects.requireNonNull(ccCVV, "ccCVV is required");
        order.tacos = new ArrayList<>(Objects.requireNonNull(tacos, "tacos are required"));
        order.totalPrice = Objects.requireNonNull(totalPrice, "totalPrice is required")
                .setScale(2, RoundingMode.HALF_UP);
        return order;
    }

    public void transitionTo(OrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidOrderStatusTransitionException(status, target);
        }
        status = target;
    }

    public void updateDeliveryDetails(DeliveryAddress deliveryAddress, String comment) {
        if (status.isTerminal()) {
            throw new OrderNotEditableException(status);
        }
        this.deliveryAddress = Objects.requireNonNull(deliveryAddress, "deliveryAddress is required");
        this.comment = comment;
    }

    @PrePersist
    void initializeOrder() {
        Instant now = Instant.now();
        if (orderUuid == null) {
            orderUuid = UUID.randomUUID();
        }
        if (status == null) {
            status = OrderStatus.CREATED;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void markUpdated() {
        updatedAt = Instant.now();
    }
}
