package iuh.fit.se.shared.event;

import lombok.Getter;

@Getter
public final class KitchenTaskDoneEvent extends DomainEvent {

    private final Long taskId;
    private final Long orderItemId;
    private final Long orderId;

    public KitchenTaskDoneEvent(Long taskId, Long orderItemId, Long orderId) {
        this.taskId = taskId;
        this.orderItemId = orderItemId;
        this.orderId = orderId;
    }
}
