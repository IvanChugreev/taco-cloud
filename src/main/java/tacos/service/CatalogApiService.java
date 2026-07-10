package tacos.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tacos.domain.Ingredient;
import tacos.domain.Taco;
import tacos.dto.api.IngredientResponse;
import tacos.dto.api.TacoResponse;
import tacos.mapper.ApiMapper;
import tacos.repository.IngredientRepository;
import tacos.repository.TacoRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CatalogApiService {

    private final IngredientRepository ingredientRepository;
    private final TacoRepository tacoRepository;
    private final ApiMapper apiMapper;

    public CatalogApiService(
            IngredientRepository ingredientRepository,
            TacoRepository tacoRepository,
            ApiMapper apiMapper) {
        this.ingredientRepository = ingredientRepository;
        this.tacoRepository = tacoRepository;
        this.apiMapper = apiMapper;
    }

    @Transactional(readOnly = true)
    public List<IngredientResponse> getIngredients() {
        List<Ingredient> ingredients = toList(ingredientRepository.findAll());
        return ingredients.stream()
                .sorted(Comparator.comparing(Ingredient::getId))
                .map(apiMapper::toIngredientResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TacoResponse> getTacos() {
        return tacoRepository.findAvailableForOrdering().stream()
                .sorted(Comparator.comparing(Taco::getId))
                .map(apiMapper::toTacoResponse)
                .toList();
    }

    private static <T> List<T> toList(Iterable<T> values) {
        List<T> result = new ArrayList<>();
        values.forEach(result::add);
        return result;
    }
}
