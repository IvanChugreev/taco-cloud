package tacos.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tacos.domain.OrderStatus;
import tacos.event.EventEnvelope;
import tacos.event.OrderCreated;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventOutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Test
    void storesVersionedEventPayload() {
        UUID orderId = UUID.randomUUID();
        OrderEventOutboxService service = new OrderEventOutboxService(
                outboxEventRepository,
                new ObjectMapper().findAndRegisterModules());

        service.append(new OrderCreated(
                orderId,
                "user",
                OrderStatus.CREATED,
                new BigDecimal("12.50")));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        OutboxEvent event = captor.getValue();
        assertNotNull(event.getEventId());
        assertEquals(orderId, event.getAggregateId());
        assertEquals("OrderCreated", event.getEventType());
        assertEquals(EventEnvelope.CURRENT_VERSION, event.getEventVersion());
        assertEquals(orderId.toString(), event.getPayload().required("orderId").asText());
        assertEquals(0, event.getAttempts());
        assertNull(event.getPublishedAt());
    }
}
