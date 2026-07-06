package tacos.domain;

public enum OrderStatus {
    CREATED,
    ACCEPTED,
    PREPARING,
    READY,
    COMPLETED,
    REJECTED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case CREATED -> target == ACCEPTED || target == REJECTED || target == CANCELLED;
            case ACCEPTED -> target == PREPARING || target == CANCELLED;
            case PREPARING -> target == READY;
            case READY -> target == COMPLETED;
            case COMPLETED, REJECTED, CANCELLED -> false;
        };
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == REJECTED || this == CANCELLED;
    }
}
