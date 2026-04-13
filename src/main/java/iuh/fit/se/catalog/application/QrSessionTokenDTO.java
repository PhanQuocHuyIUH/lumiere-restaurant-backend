package iuh.fit.se.catalog.application;

import java.time.Instant;

public record QrSessionTokenDTO(
        String sessionId,
        Instant expiresAt
) {
}
