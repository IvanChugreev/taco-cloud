package tacos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class DatabaseMigrationTest {

    private final JdbcTemplate jdbcTemplate;

    DatabaseMigrationTest(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void flywayAppliesSchemaAndDevelopmentDataMigrations() {
        Integer successfulMigrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success",
                Integer.class);
        Integer ingredientCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ingredients",
                Integer.class);

        assertEquals(5, successfulMigrations);
        assertEquals(10, ingredientCount);
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
        assertTrue(indexNames.contains("idx_orders_placed_at"));

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
}
