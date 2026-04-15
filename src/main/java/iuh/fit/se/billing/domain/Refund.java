package iuh.fit.se.billing.domain;

import iuh.fit.se.shared.exception.InvalidStateTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "refunds", schema = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", columnDefinition = "refund_status_enum", nullable = false)
    @Builder.Default
    private RefundStatus status = RefundStatus.INITIATED;

    @Column(name = "idempotency_key", nullable = false, length = 200)
    private String idempotencyKey;

    @Column(name = "provider_refund_id")
    private String providerRefundId;

    @Column(name = "requested_by")
    private Long requestedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_request", columnDefinition = "jsonb")
    private Map<String, Object> rawRequest;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response", columnDefinition = "jsonb")
    private Map<String, Object> rawResponse;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = RefundStatus.INITIATED;
        }
    }

    public static Refund createInitiated(
            Long paymentId,
            BigDecimal amount,
            String reason,
            String idempotencyKey,
            Long requestedBy
    ) {
        return Refund.builder()
                .paymentId(paymentId)
                .amount(amount)
                .reason(reason)
                .idempotencyKey(idempotencyKey)
                .requestedBy(requestedBy)
                .status(RefundStatus.INITIATED)
                .build();
    }

    public void markPending(Map<String, Object> rawRequest) {
        validateTransition(RefundStatus.PENDING);
        this.status = RefundStatus.PENDING;
        this.rawRequest = rawRequest;
    }

    public void markSuccess(String providerRefundId, Map<String, Object> rawResponse) {
        validateTransition(RefundStatus.SUCCESS);
        this.status = RefundStatus.SUCCESS;
        this.providerRefundId = providerRefundId;
        this.rawResponse = rawResponse;
        this.completedAt = Instant.now();
    }

    public void markFailed(Map<String, Object> rawResponse) {
        validateTransition(RefundStatus.FAILED);
        this.status = RefundStatus.FAILED;
        this.rawResponse = rawResponse;
        this.completedAt = Instant.now();
    }

    private void validateTransition(RefundStatus to) {
        Set<RefundStatus> allowed;
        if (this.status == RefundStatus.INITIATED) {
            allowed = Set.of(RefundStatus.PENDING, RefundStatus.SUCCESS, RefundStatus.FAILED);
        } else if (this.status == RefundStatus.PENDING) {
            allowed = Set.of(RefundStatus.SUCCESS, RefundStatus.FAILED);
        } else {
            allowed = Set.of();
        }

        if (!allowed.contains(to)) {
            throw new InvalidStateTransitionException("Refund", String.valueOf(this.status), String.valueOf(to));
        }
    }
}
