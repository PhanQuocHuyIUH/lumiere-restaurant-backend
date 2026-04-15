package iuh.fit.se.billing.domain;

import iuh.fit.se.shared.exception.DomainException;
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
import java.util.Locale;
import java.util.Map;
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
@Table(name = "payment_webhooks", schema = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentWebhook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id")
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "provider", columnDefinition = "payment_provider_enum", nullable = false)
    private PaymentProvider provider;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> rawPayload;

    @Column(name = "signature", length = 512)
    private String signature;

    @Column(name = "signature_valid")
    private Boolean signatureValid;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", columnDefinition = "webhook_status_enum", nullable = false)
    @Builder.Default
    private WebhookStatus status = WebhookStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (this.receivedAt == null) {
            this.receivedAt = Instant.now();
        }
        if (this.status == null) {
            this.status = WebhookStatus.PENDING;
        }
        if (this.retryCount == null) {
            this.retryCount = 0;
        }
        this.httpMethod = normalizeHttpMethod(this.httpMethod);
        if (this.rawPayload == null) {
            throw new DomainException("Webhook payload must not be null");
        }
    }

    public static PaymentWebhook receive(
            PaymentProvider provider,
            String httpMethod,
            Map<String, Object> rawPayload,
            String signature
    ) {
        return PaymentWebhook.builder()
                .provider(provider)
                .httpMethod(httpMethod)
                .rawPayload(rawPayload)
                .signature(signature)
                .status(WebhookStatus.PENDING)
                .retryCount(0)
                .build();
    }

    public void linkPayment(Long paymentId) {
        this.paymentId = paymentId;
    }

    public void markSignatureResult(boolean signatureValid) {
        this.signatureValid = signatureValid;
    }

    public void markProcessed() {
        this.status = WebhookStatus.PROCESSED;
        this.processedAt = Instant.now();
        this.errorMessage = null;
    }

    public void markDuplicate() {
        this.status = WebhookStatus.DUPLICATE;
        this.processedAt = Instant.now();
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = WebhookStatus.FAILED;
        this.processedAt = Instant.now();
        this.retryCount = this.retryCount + 1;
        this.errorMessage = errorMessage;
    }

    private String normalizeHttpMethod(String method) {
        if (method == null || method.isBlank()) {
            throw new DomainException("Webhook HTTP method is required");
        }

        String normalized = method.trim().toUpperCase(Locale.ROOT);
        if (!"GET".equals(normalized) && !"POST".equals(normalized)) {
            throw new DomainException("Unsupported webhook HTTP method: " + method);
        }

        return normalized;
    }
}
