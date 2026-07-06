package tacos.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tacos.domain.Ingredient;
import tacos.domain.Ingredient.Type;
import tacos.domain.Taco;
import tacos.dto.IngredientCatalog;
import tacos.dto.TacoForm;
import tacos.dto.TacoSummary;
import tacos.mapper.TacoMapper;
import tacos.repository.IngredientRepository;
import tacos.repository.TacoRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TacoServiceTest {

    @Mock
    private IngredientRepository ingredientRepository;

    @Mock
    private TacoRepository tacoRepository;

    private TacoService tacoService;

    @BeforeEach
    void setUp() {
        tacoService = new TacoService(ingredientRepository, tacoRepository, new TacoMapper());
    }

    @Test
    void createTacoLoadsIngredientsAndReturnsSummary() {
        TacoForm form = new TacoForm();
        form.setName("Test Taco");
        form.setIngredientIds(List.of("FLTO", "GRBF"));
        List<Ingredient> ingredients = List.of(
                ingredient("FLTO", "Flour Tortilla", Type.WRAP),
                ingredient("GRBF", "Ground Beef", Type.PROTEIN));
        when(ingredientRepository.findAllById(List.of("FLTO", "GRBF"))).thenReturn(ingredients);
        when(tacoRepository.save(any(Taco.class))).thenAnswer(invocation -> {
            Taco taco = invocation.getArgument(0);
            taco.setId(42L);
            return taco;
        });

        TacoSummary result = tacoService.createTaco(form);

        assertEquals(42L, result.getId());
        assertEquals("Test Taco", result.getName());
        verify(tacoRepository).save(any(Taco.class));
    }

    @Test
    void createTacoRejectsUnknownIngredient() {
        TacoForm form = new TacoForm();
        form.setName("Test Taco");
        form.setIngredientIds(List.of("FLTO", "UNKNOWN"));
        when(ingredientRepository.findAllById(List.of("FLTO", "UNKNOWN")))
                .thenReturn(List.of(ingredient("FLTO", "Flour Tortilla", Type.WRAP)));

        assertThrows(ResourceNotFoundException.class, () -> tacoService.createTaco(form));

        verify(tacoRepository, never()).save(any());
    }

    @Test
    void getIngredientCatalogGroupsIngredientsByType() {
        when(ingredientRepository.findAll()).thenReturn(List.of(
                ingredient("FLTO", "Flour Tortilla", Type.WRAP),
                ingredient("GRBF", "Ground Beef", Type.PROTEIN),
                ingredient("CHED", "Cheddar", Type.CHEESE),
                ingredient("TMTO", "Diced Tomatoes", Type.VEGGIES),
                ingredient("SLSA", "Salsa", Type.SAUCE)));

        IngredientCatalog catalog = tacoService.getIngredientCatalog();

        assertEquals("FLTO", catalog.getWraps().get(0).getId());
        assertEquals("GRBF", catalog.getProteins().get(0).getId());
        assertEquals("CHED", catalog.getCheeses().get(0).getId());
        assertEquals("TMTO", catalog.getVeggies().get(0).getId());
        assertEquals("SLSA", catalog.getSauces().get(0).getId());
        assertEquals(BigDecimal.ONE, catalog.getWraps().get(0).getPrice());
    }

    private Ingredient ingredient(String id, String name, Type type) {
        return new Ingredient(id, name, type, BigDecimal.ONE);
    }
}
