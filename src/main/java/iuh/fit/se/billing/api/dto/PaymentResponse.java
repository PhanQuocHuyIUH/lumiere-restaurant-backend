package iuh.fit.se.billing.api.dto;

import iuh.fit.se.billing.domain.Payment;
import iuh.fit.se.billing.domain.PaymentMethod;
import iuh.fit.se.billing.domain.PaymentProvider;
import iuh.fit.se.billing.domain.PaymentStatus;
import iuh.fit.se.shared.domain.TaxMode;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long paymentId,
        Long orderId,
    Long shiftId,
        BigDecimal subtotalAmount,
        BigDecimal taxAmount,
        BigDecimal amount,
        TaxMode taxMode,
        Integer taxRateBps,
        PaymentMethod paymentMethod,
        PaymentProvider provider,
        PaymentStatus status,
        String qrContent,
        String payUrl,
        Instant qrExpiresAt,
        String providerTransactionId,
        String providerResponseCode,
        Instant createdAt,
        Instant paidAt,
        Instant failedAt
) {

    public static PaymentResponse from(Payment payment) {
        String payUrl = payment.getProvider() == PaymentProvider.VNPAY ? payment.getQrContent() : null;

        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
            payment.getShiftId(),
                payment.getSubtotalAmount() == null ? BigDecimal.ZERO : payment.getSubtotalAmount().toBigDecimal(),
                payment.getTaxAmount() == null ? BigDecimal.ZERO : payment.getTaxAmount().toBigDecimal(),
                payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount().toBigDecimal(),
                payment.getTaxMode(),
                payment.getTaxRateBps(),
                payment.getPaymentMethod(),
                payment.getProvider(),
                payment.getStatus(),
                payment.getQrContent(),
                payUrl,
                payment.getQrExpiresAt(),
                payment.getProviderTransactionId(),
                payment.getProviderResponseCode(),
                payment.getCreatedAt(),
                payment.getPaidAt(),
                payment.getFailedAt()
        );
    }
}
