package iuh.fit.se.billing.application.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.billing.api.dto.PaymentResponse;
import iuh.fit.se.shared.exception.IdempotencyConflictException;
import iuh.fit.se.shared.util.IdempotencyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Manages payment idempotency using Redis for 24-hour deduplication.
 * Replaces DB-based idempotency to reduce database load.
 * 
 * State machine:
 * - PENDING: SETNX set when request starts
 * - SUCCESS: Response is cached after operation succeeds
 * - FAILED: Only checked on retry; operation fails again
 * 
 * If Redis is unavailable, graceful degradation: operation proceeds without protection.
 */
@Slf4j
@Service
public class PaymentIdempotencyService {

    private static final String REDIS_KEY_PREFIX = "idem:payment:";
    private static final long EXPIRY_HOURS = 24L;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public PaymentIdempotencyService(
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Execute payment operation with Redis-based idempotency protection.
     * @param idempotencyKey UUID provided by client
     * @param operation Operation type (CREATE_PAYMENT or CREATE_REFUND)
     * @param action Supplier that performs the actual operation
     * @return PaymentResponse from action or cached response
     * @throws IdempotencyConflictException if operation is already in PENDING state
     */
    public PaymentResponse executeIdempotent(
            String idempotencyKey,
            String operation,
            Supplier<PaymentResponse> action
    ) {
        String redisKey = REDIS_KEY_PREFIX + idempotencyKey;
        
        try {
            // Try to mark this request as PENDING using SETNX (set if not exists)
            String pendingMarker = operation + ":PENDING:" + System.currentTimeMillis();
            Boolean setSuccess = redisTemplate.opsForValue().setIfAbsent(
                    redisKey,
                    pendingMarker,
                    EXPIRY_HOURS,
                    TimeUnit.HOURS
            );

            if (setSuccess == null || !setSuccess) {
                // Key already exists: check if it's a duplicate request or concurrent request
                Object existing = redisTemplate.opsForValue().get(redisKey);
                if (existing != null) {
                    String existingStr = existing.toString();
                    if (existingStr.startsWith(operation + ":PENDING")) {
                        // Request is already being processed - conflict
                        throw new IdempotencyConflictException(idempotencyKey);
                    } else if (existingStr.startsWith(operation + ":SUCCESS")) {
                        // Request was previously successful - return cached response
                        String cachedJson = existingStr.substring((operation + ":SUCCESS:").length());
                        try {
                            return objectMapper.readValue(cachedJson, PaymentResponse.class);
                        } catch (Exception e) {
                            log.warn("Failed to deserialize cached response for key {}", idempotencyKey, e);
                            // Fall through to retry
                        }
                    }
                }
                // Unknown state or deserialization failed - let it through
            }

            // Execute the operation
            PaymentResponse response = action.get();

            // Cache the successful response
            try {
                String responseJson = objectMapper.writeValueAsString(response);
                String successMarker = operation + ":SUCCESS:" + responseJson;
                redisTemplate.opsForValue().set(
                        redisKey,
                        successMarker,
                        EXPIRY_HOURS,
                        TimeUnit.HOURS
                );
            } catch (Exception e) {
                log.warn("Failed to cache response for key {}", idempotencyKey, e);
                // Response still succeeds, just not cached
            }

            return response;

        } catch (IdempotencyConflictException e) {
            throw e;
        } catch (Exception e) {
            // Redis is unavailable - graceful degradation
            log.warn("Redis error during idempotency check for key {}, allowing operation to proceed", idempotencyKey, e);
            return action.get();
        }
    }

    /**
     * Clear idempotency key from Redis (e.g., for testing or manual cleanup).
     */
    public void clearIdempotencyKey(String idempotencyKey) {
        try {
            String redisKey = REDIS_KEY_PREFIX + idempotencyKey;
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.warn("Failed to clear idempotency key {}", idempotencyKey, e);
        }
    }
}
