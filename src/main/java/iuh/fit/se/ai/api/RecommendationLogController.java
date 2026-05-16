package iuh.fit.se.ai.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.ai.api.dto.RecommendationLogRequest;
import iuh.fit.se.ai.RecommendationFeedbackFlushJob;
import iuh.fit.se.ai.client.dto.FeedbackEventItem;
import iuh.fit.se.ai.ratelimit.RedisRateLimiter;
import iuh.fit.se.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recommendation-logs")
public class RecommendationLogController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecommendationLogController.class);
    private static final int RATE_LIMIT_PER_MINUTE = 60;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
    private static final int MAX_FEEDBACK_ITEMS = 20;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisRateLimiter rateLimiter;

    public RecommendationLogController(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            RedisRateLimiter rateLimiter
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> logRecommendation(
            @Valid @RequestBody RecommendationLogRequest request,
            HttpServletRequest httpRequest
    ) {
        String bucket = resolveBucket(request, httpRequest);
        if (!rateLimiter.tryAcquire(bucket, RATE_LIMIT_PER_MINUTE, RATE_LIMIT_WINDOW)) {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("Too many recommendation logs", null));
        }

        // Sanitize before writing to the buffer — rate limit alone does not stop a
        // single request from inflating CTR counters by repeating the same item id.
        List<Integer> shown = sanitizeShown(request.shown());
        List<Integer> clicked = sanitizeClicked(request.clicked(), shown);
        if (shown.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok("Log recorded", null));
        }

        try {
            String json = objectMapper.writeValueAsString(
                    new FeedbackEventItem("RECOMMENDATION_CLICK", shown, clicked)
            );
            stringRedisTemplate.opsForList().rightPush(RecommendationFeedbackFlushJob.REDIS_LOG_KEY, json);
        } catch (JsonProcessingException ex) {
            LOGGER.warn("Failed to serialize recommendation log: {}", ex.getMessage());
        } catch (Exception ex) {
            LOGGER.warn("Failed to store recommendation log in Redis: {}", ex.getMessage());
        }
        return ResponseEntity.ok(ApiResponse.ok("Log recorded", null));
    }

    private List<Integer> sanitizeShown(List<Integer> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        Set<Integer> seen = new LinkedHashSet<>();
        for (Integer id : raw) {
            if (id == null) continue;
            seen.add(id);
            if (seen.size() >= MAX_FEEDBACK_ITEMS) break;
        }
        return new ArrayList<>(seen);
    }

    private List<Integer> sanitizeClicked(List<Integer> rawClicked, List<Integer> shown) {
        if (rawClicked == null || rawClicked.isEmpty() || shown.isEmpty()) {
            return List.of();
        }
        Set<Integer> shownSet = new LinkedHashSet<>(shown);
        Set<Integer> seen = new LinkedHashSet<>();
        for (Integer id : rawClicked) {
            if (id == null || !shownSet.contains(id)) continue;
            seen.add(id);
            if (seen.size() >= MAX_FEEDBACK_ITEMS) break;
        }
        return new ArrayList<>(seen);
    }

    private String resolveBucket(RecommendationLogRequest request, HttpServletRequest httpRequest) {
        String sessionId = request.sessionId();
        if (sessionId != null && !sessionId.isBlank()) {
            return "reclog:session:" + sessionId.trim();
        }
        return "reclog:ip:" + clientIp(httpRequest);
    }

    private String clientIp(HttpServletRequest httpRequest) {
        String forwarded = httpRequest.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            String first = comma >= 0 ? forwarded.substring(0, comma) : forwarded;
            return first.trim();
        }
        return httpRequest.getRemoteAddr();
    }
}
