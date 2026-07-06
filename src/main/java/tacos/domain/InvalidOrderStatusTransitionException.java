package tacos.domain;

public class InvalidOrderStatusTransitionException extends RuntimeException {

    public InvalidOrderStatusTransitionException(OrderStatus current, OrderStatus target) {
        super("Order status cannot change from " + current + " to " + target);
    }
}
