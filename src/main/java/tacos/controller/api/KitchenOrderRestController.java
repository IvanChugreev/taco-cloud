package tacos.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tacos.dto.api.OrderResponse;
import tacos.dto.api.UpdateOrderStatusRequest;
import tacos.service.OrderService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kitchen/orders")
@Tag(name = "Kitchen orders", description = "Kitchen order workflow operations")
public class KitchenOrderRestController {

    private final OrderService orderService;

    public KitchenOrderRestController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "Move an order through the kitchen workflow")
    public OrderResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.transitionOrder(id, request.status(), request.version());
    }
}
