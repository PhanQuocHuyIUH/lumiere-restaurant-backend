package iuh.fit.se.kitchen.domain;

import iuh.fit.se.shared.exception.InvalidStateTransitionException;
import java.util.Map;
import java.util.Set;

public final class KitchenBatchStateMachine {

    private static final Map<KitchenBatchStatus, Set<KitchenBatchStatus>> TRANSITIONS = Map.of(
            KitchenBatchStatus.SUGGESTED, Set.of(KitchenBatchStatus.CONFIRMED),
            KitchenBatchStatus.CONFIRMED, Set.of(KitchenBatchStatus.IN_PROGRESS),
            KitchenBatchStatus.IN_PROGRESS, Set.of(KitchenBatchStatus.DONE)
    );

    private KitchenBatchStateMachine() {
    }

    public static void validate(KitchenBatchStatus from, KitchenBatchStatus to) {
        if (from == null || to == null || !TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new InvalidStateTransitionException("KitchenBatch", String.valueOf(from), String.valueOf(to));
        }
    }
}
