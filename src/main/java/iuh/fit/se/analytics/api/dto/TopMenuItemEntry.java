package iuh.fit.se.analytics.api.dto;

import java.math.BigDecimal;

/**
 * Summary of a single menu item's performance within an analytics period.
 */
public record TopMenuItemEntry(
        Long menuItemId,
        String menuItemName,
        Long totalQuantity,     // total units ordered
        Long orderCount,         // number of distinct orders containing this item
        BigDecimal totalRevenue  // total revenue attributed to this item (subtotal sum)
) {}
