package iuh.fit.se.shared.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.shared.ai.client.dto.FeedbackEventItem;
import iuh.fit.se.shared.ai.client.dto.FeedbackRequest;
import iuh.fit.se.shared.ai.client.dto.FeedbackResponse;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecommendationFeedbackFlushJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecommendationFeedbackFlushJob.class);
    public static final String REDIS_LOG_KEY = "ai:recommendation:logs";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AiClient aiClient;

    public RecommendationFeedbackFlushJob(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            AiClient aiClient
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.aiClient = aiClient;
    }

    @Async("aiTaskExecutor")
    @Scheduled(fixedDelayString = "${ai.feedback.flush-interval-ms:300000}")
    public void flush() {
        List<String> rawEvents = stringRedisTemplate.opsForList()
                .range(REDIS_LOG_KEY, 0, -1);

        if (rawEvents == null || rawEvents.isEmpty()) {
            return;
        }

        stringRedisTemplate.delete(REDIS_LOG_KEY);

        List<FeedbackEventItem> events = rawEvents.stream()
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, FeedbackEventItem.class);
                    } catch (JsonProcessingException ex) {
                        LOGGER.debug("Skipping malformed feedback event: {}", ex.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        if (events.isEmpty()) {
            return;
        }

        try {
            aiClient.post("/ai/feedback", new FeedbackRequest(events), FeedbackResponse.class, AiOperation.FEEDBACK);
            LOGGER.debug("Flushed {} recommendation feedback events to AI service", events.size());
        } catch (Exception ex) {
            LOGGER.warn("Failed to flush feedback to AI service: {}", ex.getMessage());
        }
    }
}
