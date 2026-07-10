package tacos.dto.api;

import tacos.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt,
        BigDecimal totalPrice,
        DeliveryAddressResponse deliveryAddress,
        String comment,
        Long version,
        List<TacoResponse> tacos) {
}
