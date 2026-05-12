package iuh.fit.se.shared.ai.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import iuh.fit.se.table.domain.TableStatus;
import iuh.fit.se.kitchen.domain.KitchenTask;
import iuh.fit.se.kitchen.domain.KitchenTaskStatus;
import iuh.fit.se.ordering.domain.Order;
import iuh.fit.se.ordering.domain.OrderItem;
import iuh.fit.se.ordering.domain.OrderItemStatus;
import iuh.fit.se.ordering.domain.OrderStatus;
import iuh.fit.se.ordering.domain.RevisionSource;
import java.math.BigDecimal;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OrderItemExportRow(
        Long id,
    Long orderId,
        Long revisionId,
    Integer revisionNumber,
    RevisionSource revisionSource,
    String qrSessionId,
    Long tableId,
    String tableCode,
    Integer tableFloor,
    Integer tableNo,
    Integer tableCapacity,
    TableStatus tableStatus,
        Long menuItemId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        String note,
    OrderStatus orderStatus,
    OrderItemStatus orderItemStatus,
    KitchenTaskStatus kitchenTaskStatus,
    Integer cookTimeSeconds,
    Instant createdAt,
    Instant kitchenStartedAt,
    Instant kitchenCompletedAt
) {

    public static OrderItemExportRow from(
        OrderItem orderItem,
        Order order,
        ExportOrderRevisionContext revisionContext,
        ExportTableContext tableContext,
        KitchenTask kitchenTask
    ) {
        return new OrderItemExportRow(
                orderItem.getId(),
        order == null ? null : order.getId(),
                orderItem.getRevisionId(),
        revisionContext == null ? null : revisionContext.revisionNumber(),
        revisionContext == null ? null : revisionContext.revisionSource(),
        revisionContext == null ? null : revisionContext.qrSessionId(),
        tableContext == null ? null : tableContext.tableId(),
        tableContext == null ? null : tableContext.tableCode(),
        tableContext == null ? null : tableContext.floor(),
        tableContext == null ? null : tableContext.tableNo(),
        tableContext == null ? null : tableContext.capacity(),
        tableContext == null ? null : tableContext.tableStatus(),
                orderItem.getMenuItemId(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice() == null ? BigDecimal.ZERO : orderItem.getUnitPrice().toBigDecimal(),
                orderItem.getSubtotal() == null ? BigDecimal.ZERO : orderItem.getSubtotal().toBigDecimal(),
                orderItem.getNote(),
        order == null ? null : order.getStatus(),
        orderItem.getStatus(),
        kitchenTask == null ? null : kitchenTask.getStatus(),
        kitchenTask == null ? null : kitchenTask.getActualCookSeconds(),
        orderItem.getCreatedAt(),
        kitchenTask == null ? null : kitchenTask.getStartedAt(),
        kitchenTask == null ? null : kitchenTask.getCompletedAt()
        );
    }
}
