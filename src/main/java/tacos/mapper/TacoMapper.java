package tacos.mapper;

import org.springframework.stereotype.Component;
import tacos.domain.Ingredient;
import tacos.domain.Taco;
import tacos.dto.IngredientDto;
import tacos.dto.TacoForm;
import tacos.dto.TacoSummary;

import java.util.List;

@Component
public class TacoMapper {

    public IngredientDto toDto(Ingredient ingredient) {
        return new IngredientDto(ingredient.getId(), ingredient.getName());
    }

    public Taco toEntity(TacoForm form, List<Ingredient> ingredients) {
        Taco taco = new Taco();
        taco.setName(form.getName());
        taco.setIngredients(ingredients);
        return taco;
    }

    public TacoSummary toSummary(Taco taco) {
        return new TacoSummary(taco.getId(), taco.getName());
    }
}
