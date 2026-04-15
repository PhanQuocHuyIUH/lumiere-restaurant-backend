package iuh.fit.se.billing.domain;

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
@Table(name = "payment_transactions", schema = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "provider", columnDefinition = "payment_provider_enum", nullable = false)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "transaction_type", columnDefinition = "txn_type_enum", nullable = false)
    private TxnType transactionType;

    @Column(name = "provider_transaction_id")
    private String providerTransactionId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", columnDefinition = "txn_status_enum", nullable = false)
    @Builder.Default
    private TxnStatus status = TxnStatus.INITIATED;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_request", columnDefinition = "jsonb")
    private Map<String, Object> rawRequest;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response", columnDefinition = "jsonb")
    private Map<String, Object> rawResponse;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = TxnStatus.INITIATED;
        }
    }

    public static PaymentTransaction initiated(
            Long paymentId,
            PaymentProvider provider,
            TxnType transactionType,
            BigDecimal amount,
            Map<String, Object> rawRequest
    ) {
        return PaymentTransaction.builder()
                .paymentId(paymentId)
                .provider(provider)
                .transactionType(transactionType)
                .amount(amount)
                .rawRequest(rawRequest)
                .status(TxnStatus.INITIATED)
                .build();
    }

    public void markSuccess(
            String providerTransactionId,
            Integer httpStatusCode,
            Integer durationMs,
            Map<String, Object> rawResponse
    ) {
        this.status = TxnStatus.SUCCESS;
        this.providerTransactionId = providerTransactionId;
        this.httpStatusCode = httpStatusCode;
        this.durationMs = durationMs;
        this.rawResponse = rawResponse;
    }

    public void markFailed(
            Integer httpStatusCode,
            Integer durationMs,
            Map<String, Object> rawResponse
    ) {
        this.status = TxnStatus.FAILED;
        this.httpStatusCode = httpStatusCode;
        this.durationMs = durationMs;
        this.rawResponse = rawResponse;
    }
}
