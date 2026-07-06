package tacos.dto;

import lombok.Value;

import java.math.BigDecimal;

@Value
public class IngredientDto {
    String id;
    String name;
    BigDecimal price;
}
