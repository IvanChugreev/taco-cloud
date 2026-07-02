package tacos.domain;

import jakarta.persistence.Column;
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
import jakarta.persistence.Table;
import lombok.Data;

import java.util.ArrayList;
import java.time.Instant;
import java.util.List;

@Data
@Entity
@Table(name = "orders")
public class TacoOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status = OrderStatus.PLACED;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "delivery_name", nullable = false, length = 128)
    private String deliveryName;

    @Column(name = "delivery_street", nullable = false)
    private String deliveryStreet;

    @Column(name = "delivery_city", nullable = false, length = 128)
    private String deliveryCity;

    @Column(name = "delivery_state", nullable = false, length = 64)
    private String deliveryState;

    @Column(name = "delivery_zip", nullable = false, length = 32)
    private String deliveryZip;

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
}
