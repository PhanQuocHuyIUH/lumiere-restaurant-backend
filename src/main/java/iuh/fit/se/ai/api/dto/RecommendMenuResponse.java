package iuh.fit.se.ai.api.dto;

import iuh.fit.se.ai.client.dto.RecommendItem;
import iuh.fit.se.ai.client.dto.RecommendResponse;
import java.util.List;

/**
 * FE-facing response cho POST /orders/recommendations.
 * Tách riêng khỏi {@link RecommendResponse} (AI client DTO dùng snake_case
 * để match Python service) để JSON gửi xuống FE giữ camelCase.
 */
public record RecommendMenuResponse(
        boolean success,
        String source,
        String modelVersion,
        List<Item> items
) {

    public record Item(
            Long menuItemId,
            double score,
            String reason
    ) {
        public static Item from(RecommendItem ai) {
            return new Item(ai.menuItemId(), ai.score(), ai.reason());
        }
    }

    public static RecommendMenuResponse from(RecommendResponse ai) {
        if (ai == null) {
            return new RecommendMenuResponse(false, "backend-fallback", null, List.of());
        }
        List<Item> mapped = ai.items() == null
                ? List.of()
                : ai.items().stream().map(Item::from).toList();
        return new RecommendMenuResponse(ai.success(), ai.source(), ai.modelVersion(), mapped);
    }
}
