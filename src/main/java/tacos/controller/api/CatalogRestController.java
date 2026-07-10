package tacos.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tacos.dto.api.IngredientResponse;
import tacos.dto.api.TacoResponse;
import tacos.service.CatalogApiService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Catalog", description = "Ingredients and previously designed tacos")
public class CatalogRestController {

    private final CatalogApiService catalogApiService;

    public CatalogRestController(CatalogApiService catalogApiService) {
        this.catalogApiService = catalogApiService;
    }

    @GetMapping("/ingredients")
    @Operation(summary = "List ingredients")
    public List<IngredientResponse> getIngredients() {
        return catalogApiService.getIngredients();
    }

    @GetMapping("/tacos")
    @Operation(summary = "List available tacos")
    public List<TacoResponse> getTacos() {
        return catalogApiService.getTacos();
    }
}
