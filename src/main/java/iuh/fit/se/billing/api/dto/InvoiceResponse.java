package iuh.fit.se.billing.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record InvoiceResponse(
        Long orderId,
        String invoiceNumber,
        List<InvoiceItem> items,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal discount,
        BigDecimal total,
        String paymentMethod,
        Instant paymentTime,
        Long cashierId
) {}
