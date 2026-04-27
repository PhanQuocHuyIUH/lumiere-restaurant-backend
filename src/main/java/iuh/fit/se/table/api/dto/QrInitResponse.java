package iuh.fit.se.table.api.dto;

import iuh.fit.se.table.application.QrSessionTokenDTO;
import java.time.Instant;

public record QrInitResponse(
        String tableCode,
        String sessionId,
        Instant expiresAt
) {

    public static QrInitResponse from(String tableCode, QrSessionTokenDTO sessionToken) {
        return new QrInitResponse(
                tableCode,
                sessionToken.sessionId(),
                sessionToken.expiresAt()
        );
    }
}
