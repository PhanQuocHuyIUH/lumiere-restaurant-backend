package iuh.fit.se.identity.domain;

import iuh.fit.se.shared.exception.InvalidStateTransitionException;
import java.util.Map;
import java.util.Set;

public final class StaffStateMachine {

    private static final Map<StaffStatus, Set<StaffStatus>> TRANSITIONS = Map.of(
            StaffStatus.ACTIVE, Set.of(StaffStatus.INACTIVE),
            StaffStatus.INACTIVE, Set.of(StaffStatus.ACTIVE)
    );

    private StaffStateMachine() {
    }

    public static void validate(StaffStatus from, StaffStatus to) {
        if (from == to) {
            return;
        }

        if (from == null || to == null || !TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new InvalidStateTransitionException("Staff", String.valueOf(from), String.valueOf(to));
        }
    }
}
