package tacos.dto.api;

import tacos.domain.Ingredient;

import java.math.BigDecimal;

public record IngredientResponse(
        String id,
        String name,
        Ingredient.Type type,
        BigDecimal price) {
}
