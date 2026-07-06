package tacos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode
@Embeddable
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryAddress {

    @Column(name = "delivery_name", nullable = false, length = 128)
    private String recipientName;

    @Column(name = "delivery_street", nullable = false)
    private String street;

    @Column(name = "delivery_city", nullable = false, length = 128)
    private String city;

    @Column(name = "delivery_state", nullable = false, length = 64)
    private String state;

    @Column(name = "delivery_zip", nullable = false, length = 32)
    private String zip;
}
