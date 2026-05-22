package iuh.fit.se.billing.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record BillSummaryResponse(
        Long orderId,
        String tableCode,
        String orderStatus,
        boolean payable,
        String payableReason,
        List<InvoiceItem> items,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal total,
        PaymentRequestResponse activeRequest
) {}
