package iuh.fit.se.ai.ratelimit;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Fixed-window rate limiter backed by Redis INCR + EXPIRE.
 *
 * <p>Fails open on Redis errors: if the limiter cannot reach Redis it allows
 * the request through so a Redis outage does not take the API down with it.
 */
@Component
public class RedisRateLimiter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisRateLimiter.class);

    private final StringRedisTemplate redis;

    public RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean tryAcquire(String bucket, int limit, Duration window) {
        String key = "rl:" + bucket;
        try {
            Long count = redis.opsForValue().increment(key);
            if (count == null) {
                return true;
            }
            if (count == 1L) {
                redis.expire(key, window);
            }
            return count <= limit;
        } catch (Exception ex) {
            LOGGER.warn("Rate limiter unavailable, failing open: bucket={}, reason={}", bucket, ex.getMessage());
            return true;
        }
    }
}
