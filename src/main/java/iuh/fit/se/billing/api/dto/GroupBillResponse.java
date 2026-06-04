package iuh.fit.se.billing.api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Consolidated bill for a table group ("gộp bàn"): every active member order rolled up into
 * one set of totals, with a per-table breakdown so the cashier can still see what each table ordered.
 */
public record GroupBillResponse(
        Long groupId,
        Long masterTableId,
        String masterTableCode,
        Long anchorOrderId,
        List<GroupOrderBill> orders,
        List<InvoiceItem> items,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal total,
        boolean payable,
        String reason
) {

    /** One member order's contribution to the group bill. */
    public record GroupOrderBill(
            Long orderId,
            Long tableId,
            String tableCode,
            String status,
            BigDecimal subtotal,
            BigDecimal tax,
            BigDecimal total,
            List<InvoiceItem> items
    ) {
    }
}
