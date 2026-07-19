package tacos.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tacos.config.OutboxProperties;

import java.time.Instant;

@Component
@ConditionalOnProperty(prefix = "tacos.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxCleanupScheduler.class);

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxProperties outboxProperties;

    public OutboxCleanupScheduler(
            OutboxEventRepository outboxEventRepository,
            OutboxProperties outboxProperties) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxProperties = outboxProperties;
    }

    @Scheduled(cron = "${tacos.outbox.cleanup-cron:0 0 3 * * *}")
    public void deletePublishedEvents() {
        Instant cutoff = Instant.now().minus(outboxProperties.getRetention());
        int deleted = outboxEventRepository.deletePublishedBefore(cutoff);
        if (deleted > 0) {
            log.info("Deleted {} published outbox events", deleted);
        }
    }
}
