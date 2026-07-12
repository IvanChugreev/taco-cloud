package tacos.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tacos.domain.OrderStatus;
import tacos.dto.api.OrderResponse;
import tacos.dto.api.PageResponse;
import tacos.service.OrderService;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
@Tag(name = "Admin orders", description = "Administrative order operations")
public class AdminOrderRestController {

    private final OrderService orderService;

    public AdminOrderRestController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get any order by UUID")
    public OrderResponse getOrder(@PathVariable UUID id) {
        return orderService.getAnyOrder(id);
    }

    @GetMapping
    @Operation(summary = "List all orders")
    public PageResponse<OrderResponse> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return orderService.getAllOrders(status, createdFrom, createdTo, pageable);
    }
}
