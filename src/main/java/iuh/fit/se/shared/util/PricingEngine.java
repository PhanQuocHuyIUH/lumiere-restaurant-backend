package iuh.fit.se.shared.util;

import iuh.fit.se.shared.domain.Money;
import iuh.fit.se.shared.domain.PricingSnapshot;
import iuh.fit.se.shared.domain.TaxMode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PricingEngine {

    private final TaxMode defaultTaxMode;
    private final int defaultTaxRateBps;

    public PricingEngine(
            @Value("${pricing.tax.mode:EXCLUSIVE}") String defaultTaxMode,
            @Value("${pricing.tax.rate-bps:1000}") int defaultTaxRateBps
    ) {
        this.defaultTaxMode = parseTaxMode(defaultTaxMode);
        this.defaultTaxRateBps = Math.max(0, defaultTaxRateBps);
    }

    public PricingSnapshot snapshot(Money baseAmount) {
        return snapshot(baseAmount, defaultTaxMode, defaultTaxRateBps);
    }

    public PricingSnapshot snapshot(Money baseAmount, TaxMode taxMode, Integer taxRateBps) {
        Money safeBaseAmount = baseAmount == null ? Money.ofVnd(0L) : baseAmount;
        TaxMode mode = taxMode == null ? defaultTaxMode : taxMode;
        int rateBps = Math.max(0, taxRateBps == null ? defaultTaxRateBps : taxRateBps);

        if (mode == TaxMode.NO_TAX || rateBps <= 0) {
            return new PricingSnapshot(
                    safeBaseAmount,
                    Money.ofVnd(0L),
                    safeBaseAmount,
                    TaxMode.NO_TAX,
                    0
            );
        }

        if (mode == TaxMode.INCLUSIVE) {
            long total = safeBaseAmount.toLong();
            long tax = divideAndRound(total * (long) rateBps, 10000L + rateBps);
            long subtotal = Math.max(0L, total - tax);
            return new PricingSnapshot(
                    Money.ofVnd(subtotal),
                    Money.ofVnd(tax),
                    Money.ofVnd(total),
                    mode,
                    rateBps
            );
        }

        long subtotal = safeBaseAmount.toLong();
        long tax = divideAndRound(subtotal * (long) rateBps, 10000L);
        long total = subtotal + tax;
        return new PricingSnapshot(
                Money.ofVnd(subtotal),
                Money.ofVnd(tax),
                Money.ofVnd(total),
                mode,
                rateBps
        );
    }

    private long divideAndRound(long numerator, long denominator) {
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private TaxMode parseTaxMode(String value) {
        if (value == null || value.isBlank()) {
            return TaxMode.EXCLUSIVE;
        }
        try {
            return TaxMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return TaxMode.EXCLUSIVE;
        }
    }
}
