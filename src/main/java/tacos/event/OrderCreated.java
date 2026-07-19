package tacos.event;

import tacos.domain.OrderStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreated(
        UUID orderId,
        String username,
        OrderStatus status,
        BigDecimal totalPrice) implements OrderEvent {
}
