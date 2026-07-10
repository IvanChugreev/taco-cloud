package tacos.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import tacos.domain.OrderStatus;
import tacos.dto.api.CancelOrderRequest;
import tacos.dto.api.CreateOrderRequest;
import tacos.dto.api.OrderResponse;
import tacos.dto.api.PageResponse;
import tacos.service.OrderService;

import java.net.URI;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Operations with the authenticated user's orders")
public class OrderRestController {

    private final OrderService orderService;

    public OrderRestController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Create an order")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Principal principal,
            UriComponentsBuilder uriBuilder) {
        OrderResponse order = orderService.createOrder(request, principal.getName());
        URI location = uriBuilder.path("/api/v1/orders/{id}")
                .buildAndExpand(order.id())
                .toUri();
        return ResponseEntity.created(location).body(order);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an order by UUID")
    public OrderResponse getOrder(@PathVariable UUID id, Principal principal) {
        return orderService.getOrder(id, principal.getName());
    }

    @GetMapping
    @Operation(summary = "List and filter orders")
    public PageResponse<OrderResponse> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            Principal principal) {
        return orderService.getOrders(
                principal.getName(), status, createdFrom, createdTo, pageable);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order")
    public OrderResponse cancelOrder(
            @PathVariable UUID id,
            @Valid @RequestBody CancelOrderRequest request,
            Principal principal) {
        return orderService.cancelOrder(id, request.version(), principal.getName());
    }
}
