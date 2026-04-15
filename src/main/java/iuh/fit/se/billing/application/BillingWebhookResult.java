package iuh.fit.se.billing.application;

import iuh.fit.se.billing.api.dto.PaymentResponse;

public record BillingWebhookResult(
        boolean signatureValid,
        boolean success,
        String responseCode,
        String message,
        PaymentResponse payment
) {
}
