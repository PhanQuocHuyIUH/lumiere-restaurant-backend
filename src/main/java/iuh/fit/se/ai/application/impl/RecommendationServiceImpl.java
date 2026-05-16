package iuh.fit.se.ai.application.impl;

import iuh.fit.se.ai.application.RecommendationService;
import iuh.fit.se.ai.AiClient;
import iuh.fit.se.ai.AiOperation;
import iuh.fit.se.ai.client.dto.RecommendRequest;
import iuh.fit.se.ai.client.dto.RecommendResponse;
import iuh.fit.se.shared.exception.DomainException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private final AiClient aiClient;

    public RecommendationServiceImpl(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    public RecommendResponse recommend(RecommendRequest request) {
        RecommendRequest safeRequest = normalize(request);
        return aiClient.post("/ai/recommend", safeRequest, RecommendResponse.class, AiOperation.RECOMMEND)
                .orElseGet(() -> new RecommendResponse(false, "backend-fallback", List.of(), null));
    }

    private RecommendRequest normalize(RecommendRequest request) {
        if (request == null) {
            throw new DomainException("Recommend request is required");
        }

        List<Long> currentItems = request.currentItems() == null
                ? List.of()
                : request.currentItems().stream()
                        .filter(id -> id != null && id > 0)
                        .distinct()
                        .toList();

        if (currentItems.isEmpty()) {
            throw new DomainException("currentItems must not be empty");
        }

        int topK = request.topK() <= 0 ? 3 : request.topK();
        return new RecommendRequest(currentItems, topK);
    }
}
