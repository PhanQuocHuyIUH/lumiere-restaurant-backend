package iuh.fit.se.catalog.api.dto;

import iuh.fit.se.catalog.application.TableQrCodeDTO;
import java.time.Instant;

public record TableQrCodeResponse(
        Long tableId,
        String tableCode,
        String qrKey,
        String qrUrl,
        String status,
        Instant rotateAfter,
        Instant lastIssuedSessionAt
) {

    public static TableQrCodeResponse from(TableQrCodeDTO dto) {
        return new TableQrCodeResponse(
                dto.tableId(),
                dto.tableCode(),
                dto.qrKey(),
                dto.qrUrl(),
                dto.status(),
                dto.rotateAfter(),
                dto.lastIssuedSessionAt()
        );
    }
}
