package iuh.fit.se.shared.event;

import java.util.List;
import lombok.Getter;

@Getter
public final class OrderItemCancelledEvent extends DomainEvent {

    private final Long orderId;
    private final List<Long> cancelledOrderItemIds;

    public OrderItemCancelledEvent(Long orderId, List<Long> cancelledOrderItemIds) {
        this.orderId = orderId;
        this.cancelledOrderItemIds = List.copyOf(cancelledOrderItemIds);
    }
}
