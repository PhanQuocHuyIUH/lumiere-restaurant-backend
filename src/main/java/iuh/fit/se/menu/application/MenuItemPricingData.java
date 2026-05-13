package iuh.fit.se.menu.application;

import iuh.fit.se.shared.domain.TaxMode;
import java.math.BigDecimal;

public record MenuItemPricingData(
        Long id,
        String name,
        String categoryName,
        BigDecimal price,
        TaxMode itemTaxMode,
        Integer itemTaxRateBps
) {}
