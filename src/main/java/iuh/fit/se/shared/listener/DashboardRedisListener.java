package iuh.fit.se.shared.listener;

import iuh.fit.se.shared.event.PaymentSuccessEvent;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
public class DashboardRedisListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardRedisListener.class);
    private final RedisTemplate<String, Object> redisTemplate;

    public DashboardRedisListener(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentSuccess(PaymentSuccessEvent ev) {
        try {
            // increment counters
            String ordersKey = dashboardKeyOrders();
            String revenueKey = dashboardKeyRevenue();

            redisTemplate.opsForValue().increment(ordersKey, 1);
            // store revenue as numeric string - increment by amount
            redisTemplate.opsForValue().increment(revenueKey, ev.getAmount().longValue());

            // set TTL to end of day if not set
            long ttlSeconds = secondsUntilEndOfDay();
            setExpireIfNotSet(ordersKey, ttlSeconds);
            setExpireIfNotSet(revenueKey, ttlSeconds);
        } catch (Exception ex) {
            LOGGER.warn("Failed to update dashboard counters in Redis: {}", ex.getMessage());
        }
    }

    private void setExpireIfNotSet(String key, long seconds) {
        try {
            Long currentTtl = redisTemplate.getExpire(key);
            if (currentTtl == null || currentTtl <= 0) {
                redisTemplate.expire(key, Duration.ofSeconds(seconds));
            }
        } catch (Exception ex) {
            LOGGER.debug("Failed to set TTL for {}: {}", key, ex.getMessage());
        }
    }

    private String dashboardKeyOrders() {
        return "dashboard:today:orders";
    }

    private String dashboardKeyRevenue() {
        return "dashboard:today:revenue";
    }

    private long secondsUntilEndOfDay() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = now.with(LocalTime.MAX);
        return java.time.Duration.between(now, end).getSeconds();
    }
}
