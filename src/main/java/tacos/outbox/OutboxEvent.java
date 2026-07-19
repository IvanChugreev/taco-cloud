package tacos.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "outbox_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 128, updatable = false)
    private String eventType;

    @Column(name = "event_version", nullable = false, updatable = false)
    private int eventVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb", updatable = false)
    private JsonNode payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    public static OutboxEvent create(
            UUID aggregateId,
            String eventType,
            int eventVersion,
            JsonNode payload,
            Instant createdAt) {
        OutboxEvent event = new OutboxEvent();
        event.eventId = UUID.randomUUID();
        event.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId is required");
        event.eventType = Objects.requireNonNull(eventType, "eventType is required");
        event.eventVersion = eventVersion;
        event.payload = Objects.requireNonNull(payload, "payload is required");
        event.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        event.attempts = 0;
        return event;
    }

    public void recordAttempt() {
        attempts++;
    }

    public void markPublished(Instant publishedAt) {
        this.publishedAt = Objects.requireNonNull(publishedAt, "publishedAt is required");
    }
}
