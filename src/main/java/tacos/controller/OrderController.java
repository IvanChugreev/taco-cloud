package tacos.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import tacos.dto.OrderForm;
import tacos.service.OrderService;

import java.security.Principal;

@Controller
@RequestMapping("/orders")
@SessionAttributes("tacoOrder")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/current")
    public String orderForm(
            Principal principal,
            @ModelAttribute("tacoOrder") OrderForm tacoOrder) {
        orderService.prepareOrder(tacoOrder, principal.getName());
        return "orderForm";
    }

    @PostMapping
    public String processOrder(
            @Valid @ModelAttribute("tacoOrder") OrderForm order,
            BindingResult bindingResult,
            SessionStatus sessionStatus,
            Principal principal) {
        if (bindingResult.hasErrors()) {
            return "orderForm";
        }
        orderService.placeOrder(order, principal.getName());
        sessionStatus.setComplete();
        return "redirect:/";
    }
}
