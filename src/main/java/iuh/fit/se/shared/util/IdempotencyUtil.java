package iuh.fit.se.shared.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.shared.exception.DomainException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.regex.Pattern;

public final class IdempotencyUtil {

    private static final Pattern UUID_V4_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"
    );
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private IdempotencyUtil() {
    }

    public static String normalizeKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new DomainException("X-Idempotency-Key is required");
        }

        String normalized = rawKey.trim();
        if (!UUID_V4_PATTERN.matcher(normalized).matches()) {
            throw new DomainException("X-Idempotency-Key must be a valid UUID v4 value");
        }

        return normalized;
    }

    public static Instant defaultExpiry() {
        return Instant.now().plus(DEFAULT_TTL);
    }

    public static Map<String, Object> toJsonMap(ObjectMapper objectMapper, Object payload) {
        return objectMapper.convertValue(payload, new TypeReference<>() {
        });
    }

    public static <T> T fromJsonMap(ObjectMapper objectMapper, Map<String, Object> payload, Class<T> targetClass) {
        return objectMapper.convertValue(payload, targetClass);
    }
}
