package iuh.fit.se.billing.domain;

import iuh.fit.se.shared.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import iuh.fit.se.shared.domain.Money;
import iuh.fit.se.shared.domain.TaxMode;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "payments", schema = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "shift_id")
    private Long shiftId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "vndAmount", column = @Column(name = "amount_vnd", nullable = false))
    })
    private Money amount;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "vndAmount", column = @Column(name = "subtotal_amount_vnd", nullable = false))
    })
    private Money subtotalAmount;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "vndAmount", column = @Column(name = "tax_amount_vnd", nullable = false))
    })
    private Money taxAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_mode", nullable = false)
    @Builder.Default
    private TaxMode taxMode = TaxMode.NO_TAX;

    @Column(name = "tax_rate_bps", nullable = false)
    @Builder.Default
    private Integer taxRateBps = 0;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "payment_method", columnDefinition = "payment_method_enum", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "provider", columnDefinition = "payment_provider_enum", nullable = false)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", columnDefinition = "payment_status_enum", nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "provider_transaction_id")
    private String providerTransactionId;

    @Column(name = "provider_response_code", length = 20)
    private String providerResponseCode;

    @Column(name = "vnp_tmn_code", length = 20)
    private String vnpTmnCode;

    @Column(name = "vnp_bank_tran_no")
    private String vnpBankTranNo;

    @Column(name = "vnp_transaction_status", length = 10)
    private String vnpTransactionStatus;

    @Column(name = "vnp_pay_date", length = 14)
    private String vnpPayDate;

    @Column(name = "bank_code", length = 50)
    private String bankCode;

    @Column(name = "card_type", length = 20)
    private String cardType;

    @Column(name = "qr_content", columnDefinition = "text")
    private String qrContent;

    @Column(name = "qr_expires_at")
    private Instant qrExpiresAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provider_payload", columnDefinition = "jsonb")
    private Map<String, Object> providerPayload;

    @Column(name = "cashier_id")
    private Long cashierId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = PaymentStatus.PENDING;
        }
        if (this.amount == null || this.amount.toLong() <= 0L) {
            throw new DomainException("Payment amount must be greater than zero");
        }
        if (this.subtotalAmount == null) {
            this.subtotalAmount = this.amount;
        }
        if (this.taxAmount == null) {
            this.taxAmount = Money.ofVnd(0L);
        }
        if (this.taxMode == null) {
            this.taxMode = TaxMode.NO_TAX;
        }
        if (this.taxRateBps == null) {
            this.taxRateBps = 0;
        }
    }

    public static Payment createPending(
            Long orderId,
            Long shiftId,
            BigDecimal amount,
            BigDecimal subtotalAmount,
            BigDecimal taxAmount,
            TaxMode taxMode,
            Integer taxRateBps,
            PaymentMethod paymentMethod,
            PaymentProvider provider,
            Long cashierId
    ) {
        return Payment.builder()
                .orderId(orderId)
            .shiftId(shiftId)
                .amount(Money.of(amount))
                .subtotalAmount(Money.of(subtotalAmount))
                .taxAmount(Money.of(taxAmount))
                .taxMode(taxMode == null ? TaxMode.NO_TAX : taxMode)
                .taxRateBps(taxRateBps == null ? 0 : Math.max(0, taxRateBps))
                .paymentMethod(paymentMethod)
                .provider(provider)
                .status(PaymentStatus.PENDING)
                .cashierId(cashierId)
                .build();
    }

    public void registerProviderInit(
            String providerTransactionId,
            String providerResponseCode,
            String vnpTmnCode,
            String vnpBankTranNo,
            String vnpTransactionStatus,
            String vnpPayDate,
            String bankCode,
            String cardType,
            String qrContent,
            Instant qrExpiresAt,
            Map<String, Object> providerPayload
    ) {
        this.providerTransactionId = providerTransactionId;
        this.providerResponseCode = providerResponseCode;
        this.vnpTmnCode = vnpTmnCode;
        this.vnpBankTranNo = vnpBankTranNo;
        this.vnpTransactionStatus = vnpTransactionStatus;
        this.vnpPayDate = vnpPayDate;
        this.bankCode = bankCode;
        this.cardType = cardType;
        this.qrContent = qrContent;
        this.qrExpiresAt = qrExpiresAt;
        this.providerPayload = providerPayload;
    }

    public void markSuccess(String providerTransactionId, String providerResponseCode, Map<String, Object> providerPayload) {
        PaymentStateMachine.validate(this.status, PaymentStatus.SUCCESS);
        this.status = PaymentStatus.SUCCESS;
        this.providerTransactionId = providerTransactionId;
        this.providerResponseCode = providerResponseCode;
        this.providerPayload = providerPayload;
        this.paidAt = Instant.now();
    }

    public void markFailed(String providerResponseCode, Map<String, Object> providerPayload) {
        PaymentStateMachine.validate(this.status, PaymentStatus.FAILED);
        this.status = PaymentStatus.FAILED;
        this.providerResponseCode = providerResponseCode;
        this.providerPayload = providerPayload;
        this.failedAt = Instant.now();
    }

    public void markRefunded(Map<String, Object> providerPayload) {
        PaymentStateMachine.validate(this.status, PaymentStatus.REFUNDED);
        this.status = PaymentStatus.REFUNDED;
        this.providerPayload = providerPayload;
    }

    public boolean isPending() {
        return this.status == PaymentStatus.PENDING;
    }

    public boolean isSuccessful() {
        return this.status == PaymentStatus.SUCCESS;
    }
}
