package iuh.fit.se.ordering.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.ordering.api.dto.RecommendationLogRequest;
import iuh.fit.se.shared.ai.RecommendationFeedbackFlushJob;
import iuh.fit.se.shared.ai.client.dto.FeedbackEventItem;
import iuh.fit.se.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recommendation-logs")
public class RecommendationLogController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecommendationLogController.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RecommendationLogController(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> logRecommendation(
            @Valid @RequestBody RecommendationLogRequest request
    ) {
        try {
            String json = objectMapper.writeValueAsString(
                    new FeedbackEventItem("RECOMMENDATION_CLICK", request.shown(), request.clicked())
            );
            stringRedisTemplate.opsForList().rightPush(RecommendationFeedbackFlushJob.REDIS_LOG_KEY, json);
        } catch (JsonProcessingException ex) {
            LOGGER.warn("Failed to serialize recommendation log: {}", ex.getMessage());
        } catch (Exception ex) {
            LOGGER.warn("Failed to store recommendation log in Redis: {}", ex.getMessage());
        }
        return ResponseEntity.ok(ApiResponse.ok("Log recorded", null));
    }
}
