package tacos.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tacos.config.OrderEventsProperties;

import java.time.Instant;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "tacos.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrderEventKafkaPublisher {

    public static final int CONTRACT_VERSION = 1;

    private static final Logger log = LoggerFactory.getLogger(OrderEventKafkaPublisher.class);

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final OrderEventsProperties properties;

    public OrderEventKafkaPublisher(
            KafkaTemplate<Object, Object> kafkaTemplate,
            OrderEventsProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishOrderCreated(OrderCreated event) {
        publish("OrderCreated", event.orderId(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishOrderCancelled(OrderCancelled event) {
        publish("OrderCancelled", event.orderId(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishOrderStatusChanged(OrderStatusChanged event) {
        publish("OrderStatusChanged", event.orderId(), event);
    }

    private void publish(String eventType, UUID orderId, Object payload) {
        UUID eventId = UUID.randomUUID();
        EventEnvelope<Object> envelope = new EventEnvelope<>(
                eventId,
                eventType,
                CONTRACT_VERSION,
                Instant.now(),
                eventId,
                orderId,
                payload);
        kafkaTemplate.send(properties.getOrdersEventsTopic(), orderId.toString(), envelope)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to publish {} for order {}", eventType, orderId, error);
                    }
                });
    }
}
