package tacos.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tacos.config.OrderEventsProperties;
import tacos.config.OutboxProperties;
import tacos.event.EventEnvelope;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@ConditionalOnProperty(prefix = "tacos.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublishingService {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublishingService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final OrderEventsProperties orderEventsProperties;
    private final OutboxProperties outboxProperties;

    public OutboxPublishingService(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<Object, Object> kafkaTemplate,
            OrderEventsProperties orderEventsProperties,
            OutboxProperties outboxProperties) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.orderEventsProperties = orderEventsProperties;
        this.outboxProperties = outboxProperties;
    }

    @Transactional
    public int publishBatch() {
        List<OutboxEvent> events = outboxEventRepository.lockUnpublishedBatch(outboxProperties.getBatchSize());
        int published = 0;
        for (OutboxEvent event : events) {
            event.recordAttempt();
            try {
                publish(event);
                event.markPublished(Instant.now());
                published++;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while publishing outbox event {}", event.getEventId());
                log.debug("Interrupted Kafka publication", exception);
                break;
            } catch (ExecutionException | TimeoutException exception) {
                log.warn(
                        "Kafka is unavailable; outbox event {} will be retried: {}",
                        event.getEventId(),
                        exception.getMessage());
                log.debug("Failed Kafka publication", exception);
                break;
            } catch (RuntimeException exception) {
                log.warn(
                        "Kafka rejected outbox event {}; it will be retried: {}",
                        event.getEventId(),
                        exception.getMessage());
                log.debug("Synchronous Kafka publication failure", exception);
                break;
            }
        }
        return published;
    }

    private void publish(OutboxEvent event)
            throws InterruptedException, ExecutionException, TimeoutException {
        EventEnvelope<JsonNode> envelope = new EventEnvelope<>(
                event.getEventId(),
                event.getEventType(),
                event.getEventVersion(),
                event.getCreatedAt(),
                event.getEventId(),
                event.getAggregateId(),
                event.getPayload());
        kafkaTemplate.send(
                        orderEventsProperties.getOrdersEventsTopic(),
                        event.getAggregateId().toString(),
                        envelope)
                .get(outboxProperties.getSendTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }
}
