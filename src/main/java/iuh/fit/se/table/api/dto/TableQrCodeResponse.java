package iuh.fit.se.table.api.dto;

import iuh.fit.se.table.application.TableQrCodeDTO;
import java.time.Instant;

public record TableQrCodeResponse(
        Long tableId,
        String tableCode,
        String qrKey,
    String qrImageUrl,
        String status,
        Instant rotateAfter,
        Instant lastIssuedSessionAt
) {

    public static TableQrCodeResponse from(TableQrCodeDTO dto) {
        return new TableQrCodeResponse(
                dto.tableId(),
                dto.tableCode(),
                dto.qrKey(),
            dto.qrImageUrl(),
                dto.status(),
                dto.rotateAfter(),
                dto.lastIssuedSessionAt()
        );
    }
}
