package iuh.fit.se.analytics.domain;

import iuh.fit.se.shared.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "order_events", schema = "analytics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "event_source", length = 50)
    private String eventSource;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_type", length = 50)
    private String actorType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.orderId == null || this.orderId <= 0) {
            throw new DomainException("orderId is required");
        }

        this.eventType = normalizeRequired(this.eventType, "eventType", 100);
        this.eventSource = normalizeOptional(this.eventSource, 50);
        this.actorType = normalizeOptional(this.actorType, 50);

        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }

        if (this.metadata != null && !(this.metadata instanceof LinkedHashMap)) {
            this.metadata = new LinkedHashMap<>(this.metadata);
        }
    }

    public static OrderEvent capture(
            Long orderId,
            String eventType,
            String eventSource,
            Long actorId,
            String actorType,
            Map<String, Object> metadata,
            Instant occurredOn
    ) {
        return OrderEvent.builder()
                .orderId(orderId)
                .eventType(normalizeRequired(eventType, "eventType", 100))
                .eventSource(normalizeOptional(eventSource, 50))
                .actorId(actorId)
                .actorType(normalizeOptional(actorType, 50))
                .metadata(metadata == null ? null : new LinkedHashMap<>(metadata))
                .createdAt(occurredOn == null ? Instant.now() : occurredOn)
                .build();
    }

    private static String normalizeRequired(String value, String fieldName, int maxLength) {
        String normalized = normalizeOptional(value, maxLength);
        if (normalized == null) {
            throw new DomainException(fieldName + " is required");
        }
        return normalized;
    }

    private static String normalizeOptional(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new DomainException("Value exceeds max length " + maxLength + ": " + normalized);
        }
        return normalized;
    }
}