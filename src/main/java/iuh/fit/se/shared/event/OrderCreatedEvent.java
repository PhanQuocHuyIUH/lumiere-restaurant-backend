package iuh.fit.se.shared.event;

import lombok.Getter;

@Getter
public final class OrderCreatedEvent extends DomainEvent {

    private final Long orderId;
    private final Long tableId;

    public OrderCreatedEvent(Long orderId, Long tableId) {
        this.orderId = orderId;
        this.tableId = tableId;
    }
}
