package iuh.fit.se.shared.event;

import java.util.List;
import lombok.Getter;

@Getter
public final class OrderConfirmedEvent extends DomainEvent {

    private final Long orderId;
    private final List<Long> orderItemIds;

    public OrderConfirmedEvent(Long orderId, List<Long> orderItemIds) {
        this.orderId = orderId;
        this.orderItemIds = List.copyOf(orderItemIds);
    }
}
