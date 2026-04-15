package iuh.fit.se.billing.application;

import java.util.Map;

public record WebhookProcessResult(
        boolean signatureValid,
        boolean success,
        String responseCode,
        String message,
        Long paymentId,
        String providerTransactionId,
        Map<String, Object> normalizedPayload
) {
}
