package iuh.fit.se.kitchen.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "idempotency_keys", schema = "shared")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class KitchenIdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idem_key", nullable = false, length = 200)
    private String idemKey;

    @Column(name = "module", nullable = false, length = 50)
    private String module;

    @Column(name = "operation", nullable = false, length = 100)
    private String operation;

    @Column(name = "response_status")
    private Integer responseStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", columnDefinition = "jsonb")
    private Map<String, Object> responseBody;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.expiresAt == null) {
            this.expiresAt = this.createdAt.plusSeconds(24 * 60 * 60);
        }
    }

    public static KitchenIdempotencyKey reserve(String module, String operation, String idemKey, Instant expiresAt) {
        return KitchenIdempotencyKey.builder()
                .module(module)
                .operation(operation)
                .idemKey(idemKey)
                .expiresAt(expiresAt)
                .build();
    }

    public boolean hasResponseBody() {
        return this.responseBody != null;
    }

    public boolean isExpired(Instant now) {
        return this.expiresAt != null && this.expiresAt.isBefore(now);
    }

    public void markCompleted(int responseStatus, Map<String, Object> responseBody) {
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
    }
}