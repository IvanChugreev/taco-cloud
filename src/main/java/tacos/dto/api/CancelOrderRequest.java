package tacos.dto.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CancelOrderRequest(
        @NotNull @PositiveOrZero Long version) {
}
