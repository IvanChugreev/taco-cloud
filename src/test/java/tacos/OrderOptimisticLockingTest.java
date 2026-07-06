package tacos;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.RollbackException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import tacos.domain.OrderStatus;
import tacos.domain.TacoOrder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class OrderOptimisticLockingTest {

    private final JdbcTemplate jdbcTemplate;
    private final EntityManagerFactory entityManagerFactory;

    OrderOptimisticLockingTest(
            JdbcTemplate jdbcTemplate,
            EntityManagerFactory entityManagerFactory) {
        this.jdbcTemplate = jdbcTemplate;
        this.entityManagerFactory = entityManagerFactory;
    }

    @Test
    void rejectsConcurrentOrderUpdate() {
        UUID marker = UUID.randomUUID();
        Long userId = insertUser(marker);
        Long orderId = insertOrder(marker, userId);
        EntityManager firstManager = entityManagerFactory.createEntityManager();
        EntityManager secondManager = entityManagerFactory.createEntityManager();
        try {
            firstManager.getTransaction().begin();
            secondManager.getTransaction().begin();
            TacoOrder firstCopy = firstManager.find(TacoOrder.class, orderId);
            TacoOrder secondCopy = secondManager.find(TacoOrder.class, orderId);

            firstCopy.transitionTo(OrderStatus.ACCEPTED);
            firstManager.getTransaction().commit();

            secondCopy.transitionTo(OrderStatus.REJECTED);
            assertThrows(RollbackException.class, () -> secondManager.getTransaction().commit());

            assertEquals("ACCEPTED", jdbcTemplate.queryForObject(
                    "SELECT status FROM orders WHERE id = ?", String.class, orderId));
            assertEquals(1L, jdbcTemplate.queryForObject(
                    "SELECT version FROM orders WHERE id = ?", Long.class, orderId));
            assertTrue(Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                    "SELECT updated_at > created_at FROM orders WHERE id = ?", Boolean.class, orderId)));
        } finally {
            if (firstManager.getTransaction().isActive()) {
                firstManager.getTransaction().rollback();
            }
            if (secondManager.getTransaction().isActive()) {
                secondManager.getTransaction().rollback();
            }
            firstManager.close();
            secondManager.close();
            jdbcTemplate.update("DELETE FROM orders WHERE id = ?", orderId);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
        }
    }

    private Long insertUser(UUID marker) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO users (username, password, fullname, street, city, state, zip, phone_number)
                VALUES (?, 'encoded', 'Lock Test', '1 Test Street', 'Test City', 'TS', '12345', '5551234567')
                RETURNING id
                """,
                Long.class,
                "lock-" + marker);
    }

    private Long insertOrder(UUID orderUuid, Long userId) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO orders (
                    order_uuid, user_id, status, created_at, updated_at, total_price,
                    delivery_name, delivery_street, delivery_city, delivery_state, delivery_zip,
                    cc_number, cc_expiration, cc_cvv, version
                ) VALUES (?, ?, 'CREATED', CURRENT_TIMESTAMP - INTERVAL '1 day',
                    CURRENT_TIMESTAMP - INTERVAL '1 day', 0.00,
                    'Lock Test', '1 Test Street', 'Test City', 'TS', '12345',
                    '4111111111111111', '12/30', '123', 0)
                RETURNING id
                """,
                Long.class,
                orderUuid,
                userId);
    }
}
