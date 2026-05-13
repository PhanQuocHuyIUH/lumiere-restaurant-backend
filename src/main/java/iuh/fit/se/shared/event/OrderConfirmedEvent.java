package iuh.fit.se.shared.event;

import java.util.List;
import lombok.Getter;

@Getter
public final class OrderConfirmedEvent extends DomainEvent {

    private final Long orderId;
    private final Long tableId;
    private final String orderNote;
    private final List<OrderItemSnapshot> items;

    public OrderConfirmedEvent(Long orderId, Long tableId, String orderNote, List<OrderItemSnapshot> items) {
        this.orderId = orderId;
        this.tableId = tableId;
        this.orderNote = orderNote;
        this.items = List.copyOf(items);
    }

    public List<Long> getOrderItemIds() {
        return items.stream().map(OrderItemSnapshot::orderItemId).toList();
    }
}
