package iuh.fit.se.table.application.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.table.domain.QrSession;
import iuh.fit.se.table.infrastructure.QrSessionRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class QrSessionCacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(QrSessionCacheService.class);
    private static final String KEY_PREFIX = "qr:session:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final QrSessionRepository qrSessionRepository;

    public QrSessionCacheService(RedisTemplate<String, Object> redisTemplate,
                                 ObjectMapper objectMapper,
                                 QrSessionRepository qrSessionRepository) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.qrSessionRepository = qrSessionRepository;
    }

    public Optional<QrSession> get(String sessionId) {
        String key = KEY_PREFIX + sessionId;
        try {
            Object raw = redisTemplate.opsForValue().get(key);
            if (raw instanceof String) {
                Map<String, Object> map = objectMapper.readValue(
                        (String) raw,
                        new TypeReference<Map<String, Object>>() {
                        }
                );
                long expiresAt = ((Number) map.getOrDefault("expiresAt", 0)).longValue();
                String status = (String) map.getOrDefault("status", "");
                Instant now = Instant.now();
                if (expiresAt <= now.toEpochMilli() || "EXPIRED".equals(status)) {
                    // cached expired
                    redisTemplate.delete(key);
                    return Optional.empty();
                }
                // Hit - load full entity from DB to ensure touch/save consistency
                return qrSessionRepository.findBySessionId(sessionId).map(session -> {
                    // refresh cache TTL based on remaining time
                    refreshCacheFor(session);
                    return session;
                });
            }
        } catch (Exception ex) {
            LOGGER.warn("Redis error reading qr session {}", sessionId, ex);
        }

        // Fallback to DB
        return qrSessionRepository.findBySessionId(sessionId).map(session -> {
            try {
                save(session);
            } catch (Exception ex) {
                LOGGER.warn("Failed to cache qr session {}", sessionId, ex);
            }
            return session;
        });
    }

    public void save(QrSession session) {
        String key = KEY_PREFIX + session.getSessionId();
        long now = Instant.now().toEpochMilli();
        long expiresAt = session.getExpiresAt() == null ? now : session.getExpiresAt().toEpochMilli();
        long ttlMillis = Math.max(0, expiresAt - now);
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("sessionId", session.getSessionId());
            map.put("tableId", session.getTableId());
            map.put("expiresAt", expiresAt);
            map.put("status", session.getStatus() == null ? null : session.getStatus().name());
            map.put("lastAccessAt", session.getLastAccessAt() == null ? null : session.getLastAccessAt().toEpochMilli());
            String json = objectMapper.writeValueAsString(map);
            if (ttlMillis <= 0) {
                redisTemplate.opsForValue().set(key, json);
            } else {
                redisTemplate.opsForValue().set(key, json, ttlMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        } catch (Exception ex) {
            LOGGER.warn("Redis error saving qr session {}", session.getSessionId(), ex);
        }
    }

    public void delete(String sessionId) {
        try {
            redisTemplate.delete(KEY_PREFIX + sessionId);
        } catch (Exception ex) {
            LOGGER.warn("Redis error deleting qr session {}", sessionId, ex);
        }
    }

    private void refreshCacheFor(QrSession session) {
        long now = Instant.now().toEpochMilli();
        long expiresAt = session.getExpiresAt() == null ? now : session.getExpiresAt().toEpochMilli();
        long ttlMillis = Math.max(0, expiresAt - now);
        String key = KEY_PREFIX + session.getSessionId();
        try {
            Object raw = redisTemplate.opsForValue().get(key);
            if (raw instanceof String) {
                if (ttlMillis > 0) {
                    redisTemplate.opsForValue().set(key, raw, ttlMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
                } else {
                    redisTemplate.delete(key);
                }
            }
        } catch (Exception ex) {
            LOGGER.debug("Failed to refresh cache ttl for {}", session.getSessionId(), ex);
        }
    }
}
