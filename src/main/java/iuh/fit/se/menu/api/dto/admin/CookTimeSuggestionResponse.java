package iuh.fit.se.menu.api.dto.admin;

import java.time.Instant;

public record CookTimeSuggestionResponse(
        Long menuItemId,
        Integer currentCookTimeMinutes,
        Integer suggestedCookTimeMinutes,
        Integer sampleSize,
        Instant lastUpdatedAt
) {
}
