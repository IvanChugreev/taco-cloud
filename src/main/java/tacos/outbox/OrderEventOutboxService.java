package tacos.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tacos.event.EventEnvelope;
import tacos.event.OrderEvent;

import java.time.Instant;

@Service
public class OrderEventOutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OrderEventOutboxService(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(OrderEvent event) {
        outboxEventRepository.save(OutboxEvent.create(
                event.orderId(),
                event.eventType(),
                EventEnvelope.CURRENT_VERSION,
                objectMapper.valueToTree(event),
                Instant.now()));
    }
}
