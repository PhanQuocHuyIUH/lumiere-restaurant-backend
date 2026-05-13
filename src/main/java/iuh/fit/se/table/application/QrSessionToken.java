package iuh.fit.se.table.application;

import java.time.Instant;

public record QrSessionToken(
        String sessionId,
        Instant expiresAt
) {
}
