package iuh.fit.se.table.application;

import iuh.fit.se.table.domain.TableQrCode;
import java.time.Instant;

public record TableQrCodeDTO(
        Long tableId,
        String tableCode,
        String qrKey,
    String qrImageUrl,
        String status,
        Instant rotateAfter,
        Instant lastIssuedSessionAt
) {

    public static TableQrCodeDTO from(TableQrCode qrCode, String tableCode, String qrImageUrl) {
        return new TableQrCodeDTO(
                qrCode.getTableId(),
                tableCode,
                qrCode.getQrKey(),
            qrImageUrl,
                qrCode.getStatus().name(),
                qrCode.getRotateAfter(),
                qrCode.getLastIssuedSessionAt()
        );
    }
}
