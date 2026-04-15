package iuh.fit.se.billing.api.dto;

import iuh.fit.se.billing.domain.PaymentMethod;
import iuh.fit.se.billing.domain.PaymentProvider;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreatePaymentRequest(
        @NotNull(message = "orderId is required")
        Long orderId,

        @NotNull(message = "paymentMethod is required")
        PaymentMethod paymentMethod,

        @NotNull(message = "provider is required")
        PaymentProvider provider,

        @Pattern(regexp = "^(|[a-z]{2})$", message = "locale must be empty or 2 lowercase letters")
        String locale,

        String clientIp,

        @Pattern(regexp = "^(|[A-Za-z0-9]{3,20})$", message = "bankCode must be empty or 3-20 alphanumeric characters")
        String bankCode
) {
}
