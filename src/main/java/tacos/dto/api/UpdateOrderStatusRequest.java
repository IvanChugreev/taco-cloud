package tacos.dto.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import tacos.domain.OrderStatus;

public record UpdateOrderStatusRequest(
        @NotNull OrderStatus status,
        @NotNull @PositiveOrZero Long version) {
}
