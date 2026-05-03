package iuh.fit.se.billing.api.dto;

import iuh.fit.se.billing.domain.Payment;
import iuh.fit.se.billing.domain.PaymentMethod;
import iuh.fit.se.billing.domain.PaymentProvider;
import iuh.fit.se.billing.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long paymentId,
        Long orderId,
    Long shiftId,
        BigDecimal amount,
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
                payment.getAmount(),
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
