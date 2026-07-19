package tacos.event;

import java.util.UUID;

public interface OrderEvent {

    UUID orderId();

    default String eventType() {
        return getClass().getSimpleName();
    }
}
