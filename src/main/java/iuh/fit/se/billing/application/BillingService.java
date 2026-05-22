package iuh.fit.se.billing.application;

import iuh.fit.se.billing.api.dto.BillSummaryResponse;
import iuh.fit.se.billing.api.dto.CreatePaymentRequest;
import iuh.fit.se.billing.api.dto.InvoiceResponse;
import iuh.fit.se.billing.api.dto.PaymentResponse;
import iuh.fit.se.billing.api.dto.RefundRequest;
import iuh.fit.se.billing.api.dto.RefundResponse;
import iuh.fit.se.billing.application.dto.ShiftPaymentSummary;
import iuh.fit.se.billing.domain.PaymentProvider;
import java.util.List;
import java.util.Map;

public interface BillingService {

    PaymentResponse createPayment(CreatePaymentRequest request, String idempotencyKey);

    BillingWebhookResult processWebhook(
            PaymentProvider provider,
            String httpMethod,
            Map<String, Object> payload,
            String signature
    );

    /**
     * Create a refund. For VNPAY this calls the gateway immediately; for CASH/VIETQR
     * the refund stops at PENDING and must be confirmed by the cashier once the
     * money is physically handed over / transferred.
     */
    RefundResponse createRefund(Long paymentId, RefundRequest request, String idempotencyKey);

    /** Confirm a PENDING refund (cashier has handed over cash / completed manual transfer). */
    RefundResponse confirmRefund(Long refundId);

    /** Cancel a PENDING refund the cashier decided not to perform. */
    RefundResponse cancelPendingRefund(Long refundId, String reason);

    /** All refunds for a given payment, newest first. */
    List<RefundResponse> listRefundsForPayment(Long paymentId);

    /** All refunds across every payment of an order — used by Cashier "Lịch sử" tab. */
    List<RefundResponse> listRefundsForOrder(Long orderId);

    PaymentResponse confirmManualPayment(Long paymentId);

    PaymentResponse cancelPendingPayment(Long paymentId, String reason);

    PaymentResponse getPaymentStatusByOrderId(Long orderId);

    InvoiceResponse getInvoiceForOrder(Long orderId);

    /**
     * UC008 — Customer-facing bill summary for the active order at a table.
     * Stripped-down version of the invoice (no payment/cashier metadata).
     */
    BillSummaryResponse getBillSummaryForTable(String tableCode);

    ShiftPaymentSummary getShiftPaymentSummary(Long shiftId);
}
