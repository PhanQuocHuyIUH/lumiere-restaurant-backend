package iuh.fit.se.catalog.application;

import iuh.fit.se.catalog.domain.TableQrCode;
import java.time.Instant;

public record TableQrCodeDTO(
        Long tableId,
        String tableCode,
        String qrKey,
        String qrUrl,
        String status,
        Instant rotateAfter,
        Instant lastIssuedSessionAt
) {

    public static TableQrCodeDTO from(TableQrCode qrCode, String tableCode, String qrUrl) {
        return new TableQrCodeDTO(
                qrCode.getTableId(),
                tableCode,
                qrCode.getQrKey(),
                qrUrl,
                qrCode.getStatus().name(),
                qrCode.getRotateAfter(),
                qrCode.getLastIssuedSessionAt()
        );
    }
}
