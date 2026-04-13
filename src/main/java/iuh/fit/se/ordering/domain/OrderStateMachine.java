package iuh.fit.se.ordering.domain;

import iuh.fit.se.shared.exception.InvalidStateTransitionException;
import java.util.Map;
import java.util.Set;

public final class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            OrderStatus.CREATED, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
            OrderStatus.PREPARING, Set.of(OrderStatus.READY),
            OrderStatus.READY, Set.of(OrderStatus.SERVED),
            OrderStatus.SERVED, Set.of(OrderStatus.PAID, OrderStatus.CONFIRMED)
    );

    private OrderStateMachine() {
    }

    public static void validate(OrderStatus from, OrderStatus to) {
        if (from == null || to == null || !TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new InvalidStateTransitionException("Order", String.valueOf(from), String.valueOf(to));
        }
    }
}
