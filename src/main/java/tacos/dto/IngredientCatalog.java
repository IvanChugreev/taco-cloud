package tacos.dto;

import lombok.Value;

import java.util.List;

@Value
public class IngredientCatalog {
    List<IngredientDto> wraps;
    List<IngredientDto> proteins;
    List<IngredientDto> cheeses;
    List<IngredientDto> veggies;
    List<IngredientDto> sauces;
}
