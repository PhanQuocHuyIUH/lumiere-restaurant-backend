package iuh.fit.se.ordering.api.dto;

import iuh.fit.se.ordering.domain.OrderItem;
import iuh.fit.se.ordering.domain.OrderItemStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderItemResponse(
        Long id,
        Long revisionId,
        Long menuItemId,
        Long parentOrderItemId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        String note,
        OrderItemStatus status,
        Instant createdAt,
        boolean billable,
        boolean comboParent
) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getRevisionId(),
                item.getMenuItemId(),
                item.getParentOrderItemId(),
                item.getQuantity(),
            item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice().toBigDecimal(),
                item.calculateSubtotal(),
                item.getNote(),
                item.getStatus(),
                item.getCreatedAt(),
                item.isBillable(),
                item.isComboParent()
        );
    }
}
