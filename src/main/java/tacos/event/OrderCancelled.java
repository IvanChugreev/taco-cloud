package tacos.event;

import tacos.domain.OrderStatus;

import java.util.UUID;

public record OrderCancelled(
        UUID orderId,
        String username,
        OrderStatus previousStatus,
        OrderStatus status) implements OrderEvent {
}
