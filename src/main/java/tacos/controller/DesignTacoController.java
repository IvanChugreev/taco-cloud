package tacos.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import tacos.dto.IngredientCatalog;
import tacos.dto.OrderForm;
import tacos.dto.TacoForm;
import tacos.service.TacoService;

@Controller
@RequestMapping("/design")
@SessionAttributes("tacoOrder")
public class DesignTacoController {

    private final TacoService tacoService;

    public DesignTacoController(TacoService tacoService) {
        this.tacoService = tacoService;
    }

    @ModelAttribute
    public void addIngredientsToModel(Model model) {
        IngredientCatalog catalog = tacoService.getIngredientCatalog();
        model.addAttribute("wrap", catalog.getWraps());
        model.addAttribute("protein", catalog.getProteins());
        model.addAttribute("cheese", catalog.getCheeses());
        model.addAttribute("veggies", catalog.getVeggies());
        model.addAttribute("sauce", catalog.getSauces());
    }

    @ModelAttribute(name = "tacoOrder")
    public OrderForm order() {
        return new OrderForm();
    }

    @ModelAttribute(name = "taco")
    public TacoForm taco() {
        return new TacoForm();
    }

    @GetMapping
    public String showDesignForm() {
        return "design";
    }

    @PostMapping
    public String processTaco(
            @Valid @ModelAttribute("taco") TacoForm taco,
            BindingResult bindingResult,
            @ModelAttribute("tacoOrder") OrderForm tacoOrder) {
        if (bindingResult.hasErrors()) {
            return "design";
        }
        tacoOrder.addTaco(tacoService.createTaco(taco));
        return "redirect:/orders/current";
    }
}
