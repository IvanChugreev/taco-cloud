package tacos.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TacoOrderTest {

    @Test
    void followsTheHappyPathToCompletion() {
        TacoOrder order = order();

        order.transitionTo(OrderStatus.ACCEPTED);
        order.transitionTo(OrderStatus.PREPARING);
        order.transitionTo(OrderStatus.READY);
        order.transitionTo(OrderStatus.COMPLETED);

        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }

    @Test
    void rejectsTransitionFromCompletedToPreparing() {
        TacoOrder order = completedOrder();

        assertThrows(
                InvalidOrderStatusTransitionException.class,
                () -> order.transitionTo(OrderStatus.PREPARING));
    }

    @Test
    void allowsRejectionAndCancellationOnlyFromConfiguredStates() {
        TacoOrder rejected = order();
        rejected.transitionTo(OrderStatus.REJECTED);

        TacoOrder cancelledBeforeAcceptance = order();
        cancelledBeforeAcceptance.transitionTo(OrderStatus.CANCELLED);

        TacoOrder cancelledAfterAcceptance = order();
        cancelledAfterAcceptance.transitionTo(OrderStatus.ACCEPTED);
        cancelledAfterAcceptance.transitionTo(OrderStatus.CANCELLED);

        assertEquals(OrderStatus.REJECTED, rejected.getStatus());
        assertEquals(OrderStatus.CANCELLED, cancelledBeforeAcceptance.getStatus());
        assertEquals(OrderStatus.CANCELLED, cancelledAfterAcceptance.getStatus());
    }

    @Test
    void rejectsEditingCompletedOrder() {
        TacoOrder order = completedOrder();

        assertThrows(
                OrderNotEditableException.class,
                () -> order.updateDeliveryDetails(address(), "Changed"));
    }

    private TacoOrder completedOrder() {
        TacoOrder order = order();
        order.transitionTo(OrderStatus.ACCEPTED);
        order.transitionTo(OrderStatus.PREPARING);
        order.transitionTo(OrderStatus.READY);
        order.transitionTo(OrderStatus.COMPLETED);
        return order;
    }

    private TacoOrder order() {
        User user = new User(
                "user", "encoded", "Test User", "1 Test Street",
                "Test City", "TS", "12345", "5551234567");
        return TacoOrder.create(
                user,
                address(),
                null,
                "4111111111111111",
                "12/30",
                "123",
                List.of(),
                new BigDecimal("0.00"));
    }

    private DeliveryAddress address() {
        return new DeliveryAddress("Test User", "1 Test Street", "Test City", "TS", "12345");
    }
}
