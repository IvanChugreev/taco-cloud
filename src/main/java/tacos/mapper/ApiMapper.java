package tacos.mapper;

import org.springframework.stereotype.Component;
import tacos.domain.DeliveryAddress;
import tacos.domain.Ingredient;
import tacos.domain.Taco;
import tacos.domain.TacoOrder;
import tacos.dto.api.DeliveryAddressRequest;
import tacos.dto.api.DeliveryAddressResponse;
import tacos.dto.api.IngredientResponse;
import tacos.dto.api.OrderResponse;
import tacos.dto.api.TacoResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ApiMapper {

    public DeliveryAddress toAddress(DeliveryAddressRequest request) {
        return new DeliveryAddress(
                request.recipientName(),
                request.street(),
                request.city(),
                request.state(),
                request.zip());
    }

    public IngredientResponse toIngredientResponse(Ingredient ingredient) {
        return new IngredientResponse(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getType(),
                ingredient.getPrice());
    }

    public TacoResponse toTacoResponse(Taco taco) {
        BigDecimal price = taco.getIngredients().stream()
                .map(Ingredient::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        return new TacoResponse(
                taco.getId(),
                taco.getName(),
                taco.getCreatedAt(),
                price,
                taco.getIngredients().stream().map(this::toIngredientResponse).toList());
    }

    public OrderResponse toOrderResponse(TacoOrder order) {
        DeliveryAddress address = order.getDeliveryAddress();
        return new OrderResponse(
                order.getOrderUuid(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getTotalPrice(),
                new DeliveryAddressResponse(
                        address.getRecipientName(),
                        address.getStreet(),
                        address.getCity(),
                        address.getState(),
                        address.getZip()),
                order.getComment(),
                order.getVersion(),
                order.getTacos().stream().map(this::toTacoResponse).toList());
    }
}
