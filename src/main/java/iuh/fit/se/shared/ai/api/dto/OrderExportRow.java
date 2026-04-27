package iuh.fit.se.shared.ai.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import iuh.fit.se.table.domain.TableStatus;
import iuh.fit.se.ordering.domain.Order;
import iuh.fit.se.ordering.domain.OrderStatus;
import iuh.fit.se.ordering.domain.RevisionSource;
import java.math.BigDecimal;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OrderExportRow(
        Long id,
        Long tableId,
    String tableCode,
    Integer tableFloor,
    Integer tableNo,
    Integer tableCapacity,
    TableStatus tableStatus,
        OrderStatus status,
        BigDecimal totalAmount,
        Long confirmedById,
        Long servedById,
        String note,
        boolean splitBillAllowed,
    Integer latestRevisionNumber,
    RevisionSource latestRevisionSource,
    String latestQrSessionId,
    Long latestRevisionCreatedByStaffId,
    Instant latestRevisionCreatedAt,
        Instant createdAt,
        Instant confirmedAt,
        Instant readyAt,
        Instant servedAt,
        Instant paidAt,
        Instant cancelledAt
) {

    public static OrderExportRow from(
        Order order,
        ExportOrderRevisionContext revisionContext,
        ExportTableContext tableContext
    ) {
        return new OrderExportRow(
                order.getId(),
                order.getTableId(),
        tableContext == null ? null : tableContext.tableCode(),
        tableContext == null ? null : tableContext.floor(),
        tableContext == null ? null : tableContext.tableNo(),
        tableContext == null ? null : tableContext.capacity(),
        tableContext == null ? null : tableContext.tableStatus(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getConfirmedById(),
                order.getServedById(),
                order.getNote(),
                order.isSplitBillAllowed(),
        revisionContext == null ? null : revisionContext.revisionNumber(),
        revisionContext == null ? null : revisionContext.revisionSource(),
        revisionContext == null ? null : revisionContext.qrSessionId(),
        revisionContext == null ? null : revisionContext.createdByStaffId(),
        revisionContext == null ? null : revisionContext.createdAt(),
                order.getCreatedAt(),
                order.getConfirmedAt(),
                order.getReadyAt(),
                order.getServedAt(),
                order.getPaidAt(),
                order.getCancelledAt()
        );
    }
}
