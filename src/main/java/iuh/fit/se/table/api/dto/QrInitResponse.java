package iuh.fit.se.table.api.dto;

import iuh.fit.se.table.application.QrSessionToken;
import java.time.Instant;

public record QrInitResponse(
        String tableCode,
        String sessionId,
        Instant expiresAt
) {

    public static QrInitResponse from(String tableCode, QrSessionToken sessionToken) {
        return new QrInitResponse(
                tableCode,
                sessionToken.sessionId(),
                sessionToken.expiresAt()
        );
    }
}
