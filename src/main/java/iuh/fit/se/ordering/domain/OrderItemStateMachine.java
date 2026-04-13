package iuh.fit.se.ordering.domain;

import iuh.fit.se.shared.exception.InvalidStateTransitionException;
import java.util.Map;
import java.util.Set;

public final class OrderItemStateMachine {

    private static final Map<OrderItemStatus, Set<OrderItemStatus>> TRANSITIONS = Map.of(
            OrderItemStatus.PENDING, Set.of(OrderItemStatus.PREPARING, OrderItemStatus.CANCELLED),
            OrderItemStatus.PREPARING, Set.of(OrderItemStatus.DONE, OrderItemStatus.CANCELLED),
            OrderItemStatus.DONE, Set.of(OrderItemStatus.SERVED, OrderItemStatus.CANCELLED)
    );

    private OrderItemStateMachine() {
    }

    public static void validate(OrderItemStatus from, OrderItemStatus to) {
        if (from == null || to == null || !TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new InvalidStateTransitionException("OrderItem", String.valueOf(from), String.valueOf(to));
        }
    }
}
