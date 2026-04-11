package iuh.fit.se.shared.exception;

public class InvalidStateTransitionException extends DomainException {

    public InvalidStateTransitionException(String aggregateName, String fromState, String toState) {
        super("Invalid state transition for " + aggregateName + ": " + fromState + " -> " + toState);
    }
}
