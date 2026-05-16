package iuh.fit.se.ai.api;

import iuh.fit.se.ai.api.dto.RecommendMenuRequest;
import iuh.fit.se.ai.api.dto.RecommendMenuResponse;
import iuh.fit.se.ai.application.RecommendationService;
import iuh.fit.se.ai.client.dto.RecommendResponse;
import iuh.fit.se.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FE-facing endpoint giữ nguyên path cũ {@code POST /orders/recommendations}
 * để không vỡ client. Logic đã được tách khỏi OrderingService sang module ai.
 */
@RestController
@RequestMapping("/orders/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RecommendMenuResponse>> recommendItems(
            @Valid @RequestBody RecommendMenuRequest request
    ) {
        RecommendResponse aiResponse = recommendationService.recommend(request.toAiRequest());
        return ResponseEntity.ok(ApiResponse.ok(
                "Recommendations generated",
                RecommendMenuResponse.from(aiResponse)
        ));
    }
}
