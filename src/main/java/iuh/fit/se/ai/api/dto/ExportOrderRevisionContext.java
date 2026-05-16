package iuh.fit.se.ai.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import iuh.fit.se.ordering.domain.OrderRevision;
import iuh.fit.se.ordering.domain.RevisionSource;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ExportOrderRevisionContext(
        Long orderId,
        Integer revisionNumber,
        RevisionSource revisionSource,
        String qrSessionId,
        Long createdByStaffId,
        Instant createdAt
) {

    public static ExportOrderRevisionContext from(OrderRevision revision) {
        return new ExportOrderRevisionContext(
                revision.getOrderId(),
                revision.getRevisionNumber(),
                revision.getRevisionSource(),
                revision.getQrSessionId(),
                revision.getCreatedByStaffId(),
                revision.getCreatedAt()
        );
    }
}
