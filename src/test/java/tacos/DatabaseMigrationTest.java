package tacos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;
import tacos.outbox.OutboxEventRepository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class DatabaseMigrationTest {

    private final JdbcTemplate jdbcTemplate;
    private final OutboxEventRepository outboxEventRepository;

    DatabaseMigrationTest(
            JdbcTemplate jdbcTemplate,
            OutboxEventRepository outboxEventRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Test
    void flywayAppliesSchemaAndDevelopmentDataMigrations() {
        Integer successfulMigrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success",
                Integer.class);
        Integer ingredientCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ingredients",
                Integer.class);
        BigDecimal catalogPrice = jdbcTemplate.queryForObject(
                "SELECT SUM(price) FROM ingredients",
                BigDecimal.class);

        assertEquals(8, successfulMigrations);
        assertEquals(10, ingredientCount);
        assertEquals(new BigDecimal("12.50"), catalogPrice);
    }

    @Test
    @Transactional
    void databaseEnforcesUniqueUsernameAndCreatesOrderIndexes() {
        List<String> indexNames = jdbcTemplate.queryForList(
                """
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = current_schema()
                  AND tablename = 'orders'
                """,
                String.class);
        assertTrue(indexNames.contains("idx_orders_user_id"));
        assertTrue(indexNames.contains("idx_orders_status"));
        assertTrue(indexNames.contains("idx_orders_created_at"));

        List<String> outboxColumns = jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name = 'outbox_events'
                """,
                String.class);
        assertTrue(outboxColumns.containsAll(List.of(
                "event_id",
                "aggregate_id",
                "event_type",
                "event_version",
                "payload",
                "created_at",
                "published_at",
                "attempts")));

        String insertUser = """
                INSERT INTO users (
                    username, password, fullname, street, city, state, zip, phone_number
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        Object[] user = {
                "migration-user", "encoded", "Migration User", "1 Test Street",
                "Test City", "TS", "12345", "5551234567"
        };
        jdbcTemplate.update(insertUser, user);

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(insertUser, user));
    }

    @Test
    @Transactional
    void cleanupDeletesOnlyPublishedEventsOlderThanRetention() {
        Instant now = Instant.now();
        UUID oldEventId = UUID.randomUUID();
        UUID recentEventId = UUID.randomUUID();
        String insertEvent = """
                INSERT INTO outbox_events (
                    event_id, aggregate_id, event_type, event_version,
                    payload, created_at, published_at, attempts
                ) VALUES (?, ?, 'OrderCreated', 1, '{}'::jsonb, ?, ?, 1)
                """;
        jdbcTemplate.update(
                insertEvent,
                oldEventId,
                UUID.randomUUID(),
                Timestamp.from(now.minus(Duration.ofDays(10))),
                Timestamp.from(now.minus(Duration.ofDays(9))));
        jdbcTemplate.update(
                insertEvent,
                recentEventId,
                UUID.randomUUID(),
                Timestamp.from(now.minus(Duration.ofDays(2))),
                Timestamp.from(now.minus(Duration.ofDays(1))));

        int deleted = outboxEventRepository.deletePublishedBefore(now.minus(Duration.ofDays(7)));

        assertEquals(1, deleted);
        assertEquals(0, countOutboxEvent(oldEventId));
        assertEquals(1, countOutboxEvent(recentEventId));
    }

    private int countOutboxEvent(UUID eventId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE event_id = ?",
                Integer.class,
                eventId);
    }
}
