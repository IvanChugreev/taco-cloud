package tacos.service;

import java.util.UUID;

public class OrderVersionConflictException extends RuntimeException {

    public OrderVersionConflictException(UUID orderUuid, Long expectedVersion, Long actualVersion) {
        super("Order " + orderUuid + " was updated concurrently: expected version "
                + expectedVersion + ", actual version " + actualVersion);
    }
}
