package iuh.fit.se.shared.exception;

public class IdempotencyConflictException extends DomainException {

    public IdempotencyConflictException(String idempotencyKey) {
        super("Idempotency key is still being processed: " + idempotencyKey);
    }

    public IdempotencyConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
