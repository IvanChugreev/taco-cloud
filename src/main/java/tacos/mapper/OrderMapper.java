package tacos.mapper;

import org.springframework.stereotype.Component;
import tacos.domain.Taco;
import tacos.domain.TacoOrder;
import tacos.domain.User;
import tacos.dto.OrderForm;

import java.util.List;

@Component
public class OrderMapper {

    public TacoOrder toEntity(OrderForm form, User user, List<Taco> tacos) {
        TacoOrder order = new TacoOrder();
        order.setUser(user);
        order.setDeliveryName(form.getDeliveryName());
        order.setDeliveryStreet(form.getDeliveryStreet());
        order.setDeliveryCity(form.getDeliveryCity());
        order.setDeliveryState(form.getDeliveryState());
        order.setDeliveryZip(form.getDeliveryZip());
        order.setCcNumber(form.getCcNumber());
        order.setCcExpiration(form.getCcExpiration());
        order.setCcCVV(form.getCcCVV());
        order.setTacos(tacos);
        return order;
    }
}
