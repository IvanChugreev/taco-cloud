package tacos.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tacos.domain.Ingredient;
import tacos.domain.Ingredient.Type;
import tacos.domain.Taco;
import tacos.dto.IngredientCatalog;
import tacos.dto.IngredientDto;
import tacos.dto.TacoForm;
import tacos.dto.TacoSummary;
import tacos.mapper.TacoMapper;
import tacos.repository.IngredientRepository;
import tacos.repository.TacoRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class TacoService {

    private final IngredientRepository ingredientRepository;
    private final TacoRepository tacoRepository;
    private final TacoMapper tacoMapper;

    public TacoService(
            IngredientRepository ingredientRepository,
            TacoRepository tacoRepository,
            TacoMapper tacoMapper) {
        this.ingredientRepository = ingredientRepository;
        this.tacoRepository = tacoRepository;
        this.tacoMapper = tacoMapper;
    }

    @Transactional(readOnly = true)
    public IngredientCatalog getIngredientCatalog() {
        List<Ingredient> ingredients = toList(ingredientRepository.findAll());
        return new IngredientCatalog(
                filterByType(ingredients, Type.WRAP),
                filterByType(ingredients, Type.PROTEIN),
                filterByType(ingredients, Type.CHEESE),
                filterByType(ingredients, Type.VEGGIES),
                filterByType(ingredients, Type.SAUCE));
    }

    @Transactional
    public TacoSummary createTaco(TacoForm form) {
        List<String> ingredientIds = form.getIngredientIds().stream().distinct().toList();
        List<Ingredient> ingredients = toList(ingredientRepository.findAllById(ingredientIds));
        if (ingredients.size() != ingredientIds.size()) {
            throw new ResourceNotFoundException("One or more ingredients were not found");
        }
        Taco taco = tacoMapper.toEntity(form, ingredients);
        return tacoMapper.toSummary(tacoRepository.save(taco));
    }

    private List<IngredientDto> filterByType(List<Ingredient> ingredients, Type type) {
        return ingredients.stream()
                .filter(ingredient -> ingredient.getType() == type)
                .map(tacoMapper::toDto)
                .toList();
    }

    private static <T> List<T> toList(Iterable<T> values) {
        List<T> result = new ArrayList<>();
        values.forEach(result::add);
        return result;
    }
}
