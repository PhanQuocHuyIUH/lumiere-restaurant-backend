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
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "payment_requests", schema = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentRequest {

    private static final Map<PaymentRequestStatus, Set<PaymentRequestStatus>> TRANSITIONS = Map.of(
            PaymentRequestStatus.REQUESTED,
                Set.of(PaymentRequestStatus.ACKNOWLEDGED, PaymentRequestStatus.COMPLETED, PaymentRequestStatus.CANCELLED),
            PaymentRequestStatus.ACKNOWLEDGED,
                Set.of(PaymentRequestStatus.COMPLETED, PaymentRequestStatus.CANCELLED)
    );

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "table_code", nullable = false)
    private String tableCode;

    @Column(name = "qr_session_id")
    private String qrSessionId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "preferred_method", columnDefinition = "payment_request_method_enum", nullable = false)
    private PaymentRequestMethod preferredMethod;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", columnDefinition = "payment_request_status_enum", nullable = false)
    @Builder.Default
    private PaymentRequestStatus status = PaymentRequestStatus.REQUESTED;

    @Column(name = "acknowledged_by")
    private Long acknowledgedBy;

    @Column(name = "cancelled_reason", columnDefinition = "text")
    private String cancelledReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = Instant.now();
        if (this.status == null) this.status = PaymentRequestStatus.REQUESTED;
    }

    public static PaymentRequest create(Long orderId, String tableCode, String qrSessionId, PaymentRequestMethod method) {
        return PaymentRequest.builder()
                .orderId(orderId)
                .tableCode(tableCode)
                .qrSessionId(qrSessionId)
                .preferredMethod(method)
                .status(PaymentRequestStatus.REQUESTED)
                .build();
    }

    public void acknowledge(Long staffId) {
        validateTransition(PaymentRequestStatus.ACKNOWLEDGED);
        this.status = PaymentRequestStatus.ACKNOWLEDGED;
        this.acknowledgedBy = staffId;
        this.acknowledgedAt = Instant.now();
    }

    public void complete() {
        validateTransition(PaymentRequestStatus.COMPLETED);
        this.status = PaymentRequestStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void cancel(String reason) {
        validateTransition(PaymentRequestStatus.CANCELLED);
        this.status = PaymentRequestStatus.CANCELLED;
        this.cancelledReason = reason;
        this.completedAt = Instant.now();
    }

    public boolean isActive() {
        return this.status == PaymentRequestStatus.REQUESTED || this.status == PaymentRequestStatus.ACKNOWLEDGED;
    }

    private void validateTransition(PaymentRequestStatus to) {
        Set<PaymentRequestStatus> allowed = TRANSITIONS.getOrDefault(this.status, Set.of());
        if (!allowed.contains(to)) {
            throw new InvalidStateTransitionException("PaymentRequest", String.valueOf(this.status), String.valueOf(to));
        }
    }
}
