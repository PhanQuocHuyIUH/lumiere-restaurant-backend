package iuh.fit.se.menu.application;

/**
 * Compact delta record published over STOMP — FE patches local state instead of refetching.
 */
public record AvailabilityUpdate(
        Long menuItemId,
        boolean available,
        boolean ingredientSufficient
) {}
