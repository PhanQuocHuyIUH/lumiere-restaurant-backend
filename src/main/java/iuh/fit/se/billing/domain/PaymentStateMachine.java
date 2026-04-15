package iuh.fit.se.billing.domain;

import iuh.fit.se.shared.exception.InvalidStateTransitionException;
import java.util.Map;
import java.util.Set;

public final class PaymentStateMachine {

    private static final Map<PaymentStatus, Set<PaymentStatus>> TRANSITIONS = Map.of(
            PaymentStatus.PENDING, Set.of(PaymentStatus.SUCCESS, PaymentStatus.FAILED),
            PaymentStatus.SUCCESS, Set.of(PaymentStatus.REFUNDED)
    );

    private PaymentStateMachine() {
    }

    public static void validate(PaymentStatus from, PaymentStatus to) {
        if (from == null || to == null || !TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new InvalidStateTransitionException("Payment", String.valueOf(from), String.valueOf(to));
        }
    }
}
