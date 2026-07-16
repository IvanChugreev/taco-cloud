package tacos.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import tacos.config.OrderEventsProperties;
import tacos.domain.OrderStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventKafkaPublisherTest {

    @Mock
    private KafkaTemplate<Object, Object> kafkaTemplate;

    private OrderEventKafkaPublisher publisher;

    @BeforeEach
    void setUp() {
        OrderEventsProperties properties = new OrderEventsProperties();
        properties.setOrdersEventsTopic("orders.events");
        publisher = new OrderEventKafkaPublisher(kafkaTemplate, properties);
        when(kafkaTemplate.send(anyString(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void publishesVersionedJsonEventsWithOrderIdAsKey() {
        UUID orderId = UUID.randomUUID();

        publisher.publishOrderCreated(new OrderCreated(
                orderId,
                "user",
                OrderStatus.CREATED,
                new BigDecimal("12.50")));
        publisher.publishOrderCancelled(new OrderCancelled(
                orderId,
                "user",
                OrderStatus.CREATED,
                OrderStatus.CANCELLED));
        publisher.publishOrderStatusChanged(new OrderStatusChanged(
                orderId,
                OrderStatus.ACCEPTED,
                OrderStatus.PREPARING));

        ArgumentCaptor<Object> keyCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> envelopeCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate, times(3)).send(
                org.mockito.ArgumentMatchers.eq("orders.events"),
                keyCaptor.capture(),
                envelopeCaptor.capture());

        assertEquals(List.of(orderId.toString(), orderId.toString(), orderId.toString()),
                keyCaptor.getAllValues());

        List<Object> values = envelopeCaptor.getAllValues();
        assertEnvelope(values.get(0), "OrderCreated", orderId, OrderCreated.class);
        assertEnvelope(values.get(1), "OrderCancelled", orderId, OrderCancelled.class);
        assertEnvelope(values.get(2), "OrderStatusChanged", orderId, OrderStatusChanged.class);
    }

    private void assertEnvelope(
            Object value,
            String eventType,
            UUID orderId,
            Class<?> payloadType) {
        EventEnvelope<?> envelope = assertInstanceOf(EventEnvelope.class, value);
        assertNotNull(envelope.eventId());
        assertEquals(eventType, envelope.eventType());
        assertEquals(OrderEventKafkaPublisher.CONTRACT_VERSION, envelope.eventVersion());
        assertNotNull(envelope.occurredAt());
        assertEquals(envelope.eventId(), envelope.correlationId());
        assertEquals(orderId, envelope.aggregateId());
        assertInstanceOf(payloadType, envelope.payload());
    }
}
