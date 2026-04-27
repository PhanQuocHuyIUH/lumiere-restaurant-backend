package iuh.fit.se.table.application;

import java.time.Instant;

public record QrSessionTokenDTO(
        String sessionId,
        Instant expiresAt
) {
}
