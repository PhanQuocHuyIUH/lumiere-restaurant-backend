package iuh.fit.se.catalog.domain;

import iuh.fit.se.shared.exception.InvalidStateTransitionException;
import java.util.Map;
import java.util.Set;

public final class RestaurantTableStateMachine {

    private static final Map<TableStatus, Set<TableStatus>> TRANSITIONS = Map.of(
            TableStatus.AVAILABLE, Set.of(TableStatus.OCCUPIED, TableStatus.RESERVED, TableStatus.CLEANING),
            TableStatus.OCCUPIED, Set.of(TableStatus.AVAILABLE, TableStatus.CLEANING),
            TableStatus.RESERVED, Set.of(TableStatus.AVAILABLE, TableStatus.OCCUPIED, TableStatus.CLEANING),
            TableStatus.CLEANING, Set.of(TableStatus.AVAILABLE)
    );

    private RestaurantTableStateMachine() {
    }

    public static void validate(TableStatus from, TableStatus to) {
        if (!TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new InvalidStateTransitionException("RestaurantTable", from.name(), to.name());
        }
    }
}