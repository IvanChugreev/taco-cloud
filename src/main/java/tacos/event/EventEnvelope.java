package tacos.event;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        UUID correlationId,
        UUID aggregateId,
        T payload) {

    public static final int CURRENT_VERSION = 1;
}
