package tacos.event;

import tacos.domain.OrderStatus;

import java.util.UUID;

public record OrderStatusChanged(
        UUID orderId,
        OrderStatus previousStatus,
        OrderStatus status) {
}
