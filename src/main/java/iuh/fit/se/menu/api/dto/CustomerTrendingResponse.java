package iuh.fit.se.menu.api.dto;

import java.util.List;

public record CustomerTrendingResponse(
        TrendingMenuItemResponse hero,
        List<TrendingMenuItemResponse> topRanked,
        List<TrendingMenuItemResponse> combos
) {}
