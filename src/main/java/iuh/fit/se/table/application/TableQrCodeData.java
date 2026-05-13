package iuh.fit.se.table.application;

import iuh.fit.se.table.domain.TableQrCode;
import java.time.Instant;

public record TableQrCodeData(
        Long tableId,
        String tableCode,
        String qrKey,
    String qrImageUrl,
        String status,
        Instant rotateAfter,
        Instant lastIssuedSessionAt
) {

    public static TableQrCodeData from(TableQrCode qrCode, String tableCode, String qrImageUrl) {
        return new TableQrCodeData(
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
