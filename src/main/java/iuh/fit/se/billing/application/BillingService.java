package iuh.fit.se.billing.application;

import iuh.fit.se.billing.api.dto.CreatePaymentRequest;
import iuh.fit.se.billing.api.dto.InvoiceResponse;
import iuh.fit.se.billing.api.dto.PaymentResponse;
import iuh.fit.se.billing.api.dto.RefundRequest;
import iuh.fit.se.billing.application.dto.ShiftPaymentSummary;
import iuh.fit.se.billing.domain.PaymentProvider;
import java.util.Map;

public interface BillingService {

    PaymentResponse createPayment(CreatePaymentRequest request, String idempotencyKey);

    BillingWebhookResult processWebhook(
            PaymentProvider provider,
            String httpMethod,
            Map<String, Object> payload,
            String signature
    );

    PaymentResponse createRefund(Long paymentId, RefundRequest request, String idempotencyKey);

    PaymentResponse getPaymentStatusByOrderId(Long orderId);

    InvoiceResponse getInvoiceForOrder(Long orderId);

    ShiftPaymentSummary getShiftPaymentSummary(Long shiftId);
}
