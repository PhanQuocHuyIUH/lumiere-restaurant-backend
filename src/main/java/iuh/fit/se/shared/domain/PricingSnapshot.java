package iuh.fit.se.shared.domain;

public record PricingSnapshot(
        Money subtotalAmount,
        Money taxAmount,
        Money totalAmount,
        TaxMode taxMode,
        Integer taxRateBps
) {
}
