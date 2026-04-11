package iuh.fit.se.shared.event;

import lombok.Getter;

@Getter
public final class OrderCancelledEvent extends DomainEvent {

    private final Long orderId;
    private final String reason;

    public OrderCancelledEvent(Long orderId, String reason) {
        this.orderId = orderId;
        this.reason = reason;
    }
}
