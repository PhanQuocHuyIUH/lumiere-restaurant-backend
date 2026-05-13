package iuh.fit.se.shared.event;

public record OrderItemSnapshot(
        Long orderItemId,
        Long menuItemId,
        String menuItemName,
        String menuItemImageUrl,
        int quantity,
        String note,
        int cookTime
) {}
