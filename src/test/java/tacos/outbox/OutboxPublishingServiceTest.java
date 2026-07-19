package tacos.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import tacos.config.OrderEventsProperties;
import tacos.config.OutboxProperties;
import tacos.domain.OrderStatus;
import tacos.event.EventEnvelope;
import tacos.event.OrderCreated;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublishingServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<Object, Object> kafkaTemplate;

    private OutboxPublishingService publishingService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        OrderEventsProperties orderEventsProperties = new OrderEventsProperties();
        orderEventsProperties.setOrdersEventsTopic("orders.events");
        OutboxProperties outboxProperties = new OutboxProperties();
        outboxProperties.setBatchSize(10);
        outboxProperties.setSendTimeout(Duration.ofSeconds(1));
        objectMapper = new ObjectMapper().findAndRegisterModules();
        publishingService = new OutboxPublishingService(
                outboxEventRepository,
                kafkaTemplate,
                orderEventsProperties,
                outboxProperties);
    }

    @Test
    void marksEventPublishedOnlyAfterKafkaAcknowledgement() {
        OutboxEvent event = event();
        when(outboxEventRepository.lockUnpublishedBatch(10)).thenReturn(List.of(event));
        when(kafkaTemplate.send(eq("orders.events"), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        int published = publishingService.publishBatch();

        assertEquals(1, published);
        assertEquals(1, event.getAttempts());
        assertNotNull(event.getPublishedAt());
        ArgumentCaptor<Object> keyCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> envelopeCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("orders.events"), keyCaptor.capture(), envelopeCaptor.capture());
        assertEquals(event.getAggregateId().toString(), keyCaptor.getValue());
        EventEnvelope<?> envelope = (EventEnvelope<?>) envelopeCaptor.getValue();
        assertEquals(event.getEventId(), envelope.eventId());
        assertSame(event.getPayload(), envelope.payload());
    }

    @Test
    void retriesPendingEventAfterKafkaRecovers() {
        OutboxEvent event = event();
        when(outboxEventRepository.lockUnpublishedBatch(10)).thenReturn(List.of(event));
        when(kafkaTemplate.send(eq("orders.events"), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Kafka unavailable")))
                .thenReturn(CompletableFuture.completedFuture(null));

        int firstAttempt = publishingService.publishBatch();

        assertEquals(0, firstAttempt);
        assertEquals(1, event.getAttempts());
        assertNull(event.getPublishedAt());

        int secondAttempt = publishingService.publishBatch();

        assertEquals(1, secondAttempt);
        assertEquals(2, event.getAttempts());
        assertNotNull(event.getPublishedAt());
        verify(kafkaTemplate, times(2)).send(eq("orders.events"), any(), any());
    }

    private OutboxEvent event() {
        UUID orderId = UUID.randomUUID();
        OrderCreated payload = new OrderCreated(
                orderId,
                "user",
                OrderStatus.CREATED,
                new BigDecimal("12.50"));
        return OutboxEvent.create(
                orderId,
                "OrderCreated",
                EventEnvelope.CURRENT_VERSION,
                objectMapper.valueToTree(payload),
                Instant.parse("2026-07-16T00:00:00Z"));
    }
}
