package iuh.fit.se.shared.ai.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import iuh.fit.se.billing.domain.Payment;
import iuh.fit.se.billing.domain.PaymentMethod;
import iuh.fit.se.billing.domain.PaymentProvider;
import iuh.fit.se.billing.domain.PaymentStatus;
import iuh.fit.se.shared.domain.TaxMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PaymentExportRow(
        Long id,
        Long orderId,
        BigDecimal subtotalAmount,
        BigDecimal taxAmount,
        BigDecimal amount,
        TaxMode taxMode,
        Integer taxRateBps,
        PaymentMethod paymentMethod,
        PaymentProvider provider,
        PaymentStatus status,
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
        Long cashierId,
        Instant createdAt,
        Instant paidAt,
        Instant failedAt,
        Map<String, Object> providerPayload
) {

    public static PaymentExportRow from(Payment payment) {
        return new PaymentExportRow(
                payment.getId(),
                payment.getOrderId(),
            payment.getSubtotalAmount() == null ? BigDecimal.ZERO : payment.getSubtotalAmount().toBigDecimal(),
            payment.getTaxAmount() == null ? BigDecimal.ZERO : payment.getTaxAmount().toBigDecimal(),
            payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount().toBigDecimal(),
                payment.getTaxMode(),
                payment.getTaxRateBps(),
                payment.getPaymentMethod(),
                payment.getProvider(),
                payment.getStatus(),
                payment.getProviderTransactionId(),
                payment.getProviderResponseCode(),
                payment.getVnpTmnCode(),
                payment.getVnpBankTranNo(),
                payment.getVnpTransactionStatus(),
                payment.getVnpPayDate(),
                payment.getBankCode(),
                payment.getCardType(),
                payment.getQrContent(),
                payment.getQrExpiresAt(),
                payment.getCashierId(),
                payment.getCreatedAt(),
                payment.getPaidAt(),
                payment.getFailedAt(),
                payment.getProviderPayload()
        );
    }
}
