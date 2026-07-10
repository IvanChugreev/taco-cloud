package tacos.mapper;

import org.springframework.stereotype.Component;
import tacos.domain.DeliveryAddress;
import tacos.domain.Taco;
import tacos.domain.TacoOrder;
import tacos.domain.User;
import tacos.dto.OrderForm;
import tacos.dto.OrderUpdateCommand;

import java.math.BigDecimal;
import java.util.List;

@Component
public class OrderMapper {

    public TacoOrder toEntity(OrderForm form, User user, List<Taco> tacos, BigDecimal totalPrice) {
        return toEntity(
                user,
                toAddress(form),
                form.getComment(),
                form.getCcNumber(),
                form.getCcExpiration(),
                form.getCcCVV(),
                tacos,
                totalPrice);
    }

    public TacoOrder toEntity(
            User user,
            DeliveryAddress deliveryAddress,
            String comment,
            String ccNumber,
            String ccExpiration,
            String ccCvv,
            List<Taco> tacos,
            BigDecimal totalPrice) {
        return TacoOrder.create(
                user,
                deliveryAddress,
                comment,
                ccNumber,
                ccExpiration,
                ccCvv,
                tacos,
                totalPrice);
    }

    public DeliveryAddress toAddress(OrderForm form) {
        return new DeliveryAddress(
                form.getDeliveryName(),
                form.getDeliveryStreet(),
                form.getDeliveryCity(),
                form.getDeliveryState(),
                form.getDeliveryZip());
    }

    public DeliveryAddress toAddress(OrderUpdateCommand command) {
        return new DeliveryAddress(
                command.getDeliveryName(),
                command.getDeliveryStreet(),
                command.getDeliveryCity(),
                command.getDeliveryState(),
                command.getDeliveryZip());
    }
}
