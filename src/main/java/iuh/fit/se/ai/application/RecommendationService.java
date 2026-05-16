package iuh.fit.se.ai.application;

import iuh.fit.se.ai.client.dto.RecommendRequest;
import iuh.fit.se.ai.client.dto.RecommendResponse;

public interface RecommendationService {
    RecommendResponse recommend(RecommendRequest request);
}
