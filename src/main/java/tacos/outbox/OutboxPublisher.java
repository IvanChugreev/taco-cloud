package tacos.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "tacos.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private final OutboxPublishingService publishingService;

    public OutboxPublisher(OutboxPublishingService publishingService) {
        this.publishingService = publishingService;
    }

    @Scheduled(fixedDelayString = "${tacos.outbox.publish-interval:1s}")
    public void publishPendingEvents() {
        publishingService.publishBatch();
    }
}
