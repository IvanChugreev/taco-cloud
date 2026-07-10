package tacos.dto.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TacoResponse(
        Long id,
        String name,
        Instant createdAt,
        BigDecimal price,
        List<IngredientResponse> ingredients) {
}
