package iuh.fit.se.kitchen.domain;

import iuh.fit.se.shared.exception.InvalidStateTransitionException;
import java.util.Map;
import java.util.Set;

public final class KitchenTaskStateMachine {

    private static final Map<KitchenTaskStatus, Set<KitchenTaskStatus>> TRANSITIONS = Map.of(
            KitchenTaskStatus.CREATED, Set.of(KitchenTaskStatus.COOKING, KitchenTaskStatus.CANCELLED),
            KitchenTaskStatus.COOKING, Set.of(KitchenTaskStatus.DONE, KitchenTaskStatus.CANCELLED)
    );

    private KitchenTaskStateMachine() {
    }

    public static void validate(KitchenTaskStatus from, KitchenTaskStatus to) {
        if (from == null || to == null || !TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new InvalidStateTransitionException("KitchenTask", String.valueOf(from), String.valueOf(to));
        }
    }
}
