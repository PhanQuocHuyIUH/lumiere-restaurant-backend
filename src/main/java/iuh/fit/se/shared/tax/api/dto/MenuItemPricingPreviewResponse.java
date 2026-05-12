package iuh.fit.se.shared.tax.api.dto;

import iuh.fit.se.shared.domain.TaxMode;
import java.math.BigDecimal;
import java.util.List;

public record MenuItemPricingPreviewResponse(
        TaxMode globalTaxMode,
        int globalTaxRateBps,
        List<ItemRow> items
) {

    public record ItemRow(
            Long menuItemId,
            String name,
            String category,
            TaxMode effectiveTaxMode,
            int effectiveTaxRateBps,
            BigDecimal grossPrice,
            BigDecimal netPrice,
            BigDecimal taxAmount
    ) {}
}
