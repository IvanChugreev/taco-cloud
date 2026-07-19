package tacos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderApiIntegrationTest {

    private static final String PASSWORD = "ApiPass!2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String username;
    private String otherUsername;
    private String kitchenUsername;
    private String adminUsername;
    private Long userId;
    private Long otherUserId;
    private Long kitchenUserId;
    private Long adminUserId;
    private Long tacoId;

    @BeforeEach
    void createFixtures() {
        String marker = UUID.randomUUID().toString();
        username = "api-" + marker;
        otherUsername = "api-other-" + marker;
        kitchenUsername = "api-kitchen-" + marker;
        adminUsername = "api-admin-" + marker;
        userId = insertUser(username, "CUSTOMER");
        otherUserId = insertUser(otherUsername, "CUSTOMER");
        kitchenUserId = insertUser(kitchenUsername, "KITCHEN");
        adminUserId = insertUser(adminUsername, "ADMIN");
        tacoId = jdbcTemplate.queryForObject(
                "INSERT INTO tacos (name) VALUES ('API Test Taco') RETURNING id",
                Long.class);
        jdbcTemplate.update(
                "INSERT INTO taco_ingredients (taco_id, ingredient_id) VALUES (?, 'FLTO'), (?, 'GRBF')",
                tacoId,
                tacoId);
    }

    @AfterEach
    void removeFixtures() {
        jdbcTemplate.update(
                "DELETE FROM outbox_events WHERE aggregate_id IN "
                        + "(SELECT order_uuid FROM orders WHERE user_id IN (?, ?))",
                userId,
                otherUserId);
        jdbcTemplate.update(
                "DELETE FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE user_id IN (?, ?))",
                userId,
                otherUserId);
        jdbcTemplate.update("DELETE FROM orders WHERE user_id IN (?, ?)", userId, otherUserId);
        jdbcTemplate.update("DELETE FROM taco_ingredients WHERE taco_id = ?", tacoId);
        jdbcTemplate.update("DELETE FROM tacos WHERE id = ?", tacoId);
        jdbcTemplate.update(
                "DELETE FROM users WHERE id IN (?, ?, ?, ?)",
                userId,
                otherUserId,
                kitchenUserId,
                adminUserId);
    }

    @Test
    void supportsCompleteCreateReadListAndCancelScenario() throws Exception {
        String responseBody = mockMvc.perform(post("/api/v1/orders")
                        .with(httpBasic(username, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOrderRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        matchesPattern(".*/api/v1/orders/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalPrice").value(3.50))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.ccNumber").doesNotExist())
                .andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdOrder = objectMapper.readTree(responseBody);
        String orderId = createdOrder.required("id").asText();
        Integer pendingCreatedEvents = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM outbox_events
                WHERE aggregate_id = ?::uuid
                  AND event_type = 'OrderCreated'
                  AND published_at IS NULL
                """,
                Integer.class,
                orderId);
        assertEquals(1, pendingCreatedEvents);

        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .with(httpBasic(username, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.tacos[0].id").value(tacoId));

        mockMvc.perform(get("/api/v1/orders")
                        .param("status", "CREATED")
                        .param("page", "0")
                        .param("size", "10")
                        .with(httpBasic(username, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(orderId)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .with(httpBasic(otherUsername, PASSWORD)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(post("/api/v1/orders")
                        .with(httpBasic(username, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOrderRequest()))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                        .with(httpBasic(username, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                        .with(httpBasic(username, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void exposesCatalogWithoutLeakingEntities() throws Exception {
        mockMvc.perform(get("/api/v1/ingredients")
                        .with(httpBasic(username, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'FLTO')].price").value(hasItem(1.00)));

        mockMvc.perform(get("/api/v1/tacos")
                        .with(httpBasic(username, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + tacoId + ")].price").value(hasItem(3.50)))
                .andExpect(jsonPath("$[?(@.id == " + tacoId + ")].orders").doesNotExist());
    }

    @Test
    void returnsProblemDetailsForAuthenticationValidationAndInvalidFilters() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(post("/api/v1/orders")
                        .with(httpBasic(username, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.violations.tacoIds").isArray());

        mockMvc.perform(get("/api/v1/orders")
                        .param("createdFrom", "2026-07-08T00:00:00Z")
                        .param("createdTo", "2026-07-07T00:00:00Z")
                        .with(httpBasic(username, PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void enforcesCustomerKitchenAndAdminAccessRules() throws Exception {
        String responseBody = mockMvc.perform(post("/api/v1/orders")
                        .with(httpBasic(username, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOrderRequest()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdOrder = objectMapper.readTree(responseBody);
        String orderId = createdOrder.required("id").asText();

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .with(httpBasic(username, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId));

        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .with(httpBasic(otherUsername, PASSWORD)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .with(httpBasic(adminUsername, PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/v1/orders")
                        .with(httpBasic(adminUsername, PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/v1/admin/orders/{id}", orderId)
                        .with(httpBasic(username, PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/v1/admin/orders/{id}", orderId)
                        .with(httpBasic(adminUsername, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId));

        mockMvc.perform(post("/api/v1/kitchen/orders/{id}/status", orderId)
                        .with(httpBasic(username, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\",\"version\":0}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(post("/api/v1/kitchen/orders/{id}/status", orderId)
                        .with(httpBasic(kitchenUsername, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\",\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void publishesOpenApiDescriptionAndSwaggerUi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/orders']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.basicAuth").exists());

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    private Long insertUser(String login, String role) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO users (
                    username, password, fullname, street, city, state, zip, phone_number, role
                )
                VALUES (?, ?, 'API User', '1 Test Street', 'Test City', 'TS', '12345', '5551234567', ?)
                RETURNING id
                """,
                Long.class,
                login,
                passwordEncoder.encode(PASSWORD),
                role);
    }

    private String validOrderRequest() {
        return """
                {
                  "tacoIds": [%d],
                  "deliveryAddress": {
                    "recipientName": "API User",
                    "street": "1 Test Street",
                    "city": "Test City",
                    "state": "TS",
                    "zip": "12345"
                  },
                  "comment": "Created through API",
                  "ccNumber": "4111111111111111",
                  "ccExpiration": "12/30",
                  "ccCvv": "123"
                }
                """.formatted(tacoId);
    }
}
