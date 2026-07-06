package tacos.domain;

public class OrderNotEditableException extends RuntimeException {

    public OrderNotEditableException(OrderStatus status) {
        super("Order in status " + status + " cannot be edited");
    }
}
