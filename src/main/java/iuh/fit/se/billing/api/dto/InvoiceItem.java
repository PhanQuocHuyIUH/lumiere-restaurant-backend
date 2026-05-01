package iuh.fit.se.billing.api.dto;

import java.math.BigDecimal;

public record InvoiceItem(
        String name,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
