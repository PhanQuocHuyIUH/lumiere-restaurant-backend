package iuh.fit.se.billing.api;

import iuh.fit.se.billing.api.dto.CreatePaymentRequest;
import iuh.fit.se.billing.api.dto.PaymentResponse;
import iuh.fit.se.billing.api.dto.RefundRequest;
import iuh.fit.se.billing.api.dto.RefundResponse;
import iuh.fit.se.billing.application.BillingService;
import iuh.fit.se.billing.api.dto.InvoiceResponse;
import iuh.fit.se.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        PaymentResponse response = billingService.createPayment(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Payment created", response));
    }

    @GetMapping("/orders/{orderId}/status")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentStatusByOrderId(@PathVariable("orderId") Long orderId) {
        PaymentResponse response = billingService.getPaymentStatusByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/orders/{orderId}/invoice")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceForOrder(@PathVariable("orderId") Long orderId) {
        InvoiceResponse response = billingService.getInvoiceForOrder(orderId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAnyRole('CASHIER', 'MANAGER')")
    public ResponseEntity<ApiResponse<RefundResponse>> createRefund(
            @PathVariable("id") Long paymentId,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody RefundRequest request
    ) {
        RefundResponse response = billingService.createRefund(paymentId, request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.ok("Refund created", response));
    }

    @PostMapping("/refunds/{refundId}/confirm")
    @PreAuthorize("hasAnyRole('CASHIER', 'MANAGER')")
    public ResponseEntity<ApiResponse<RefundResponse>> confirmRefund(@PathVariable("refundId") Long refundId) {
        RefundResponse response = billingService.confirmRefund(refundId);
        return ResponseEntity.ok(ApiResponse.ok("Refund confirmed", response));
    }

    @PostMapping("/refunds/{refundId}/cancel")
    @PreAuthorize("hasAnyRole('CASHIER', 'MANAGER')")
    public ResponseEntity<ApiResponse<RefundResponse>> cancelPendingRefund(
            @PathVariable("refundId") Long refundId,
            @RequestBody(required = false) java.util.Map<String, String> body
    ) {
        String reason = body == null ? null : body.get("reason");
        RefundResponse response = billingService.cancelPendingRefund(refundId, reason);
        return ResponseEntity.ok(ApiResponse.ok("Refund cancelled", response));
    }

    @GetMapping("/{id}/refunds")
    @PreAuthorize("hasAnyRole('CASHIER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<RefundResponse>>> listRefundsForPayment(@PathVariable("id") Long paymentId) {
        return ResponseEntity.ok(ApiResponse.ok(billingService.listRefundsForPayment(paymentId)));
    }

    @GetMapping("/orders/{orderId}/refunds")
    @PreAuthorize("hasAnyRole('CASHIER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<RefundResponse>>> listRefundsForOrder(@PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(ApiResponse.ok(billingService.listRefundsForOrder(orderId)));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmManualPayment(@PathVariable("id") Long paymentId) {
        PaymentResponse response = billingService.confirmManualPayment(paymentId);
        return ResponseEntity.ok(ApiResponse.ok("Payment confirmed", response));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancelPendingPayment(
            @PathVariable("id") Long paymentId,
            @RequestBody(required = false) java.util.Map<String, String> body
    ) {
        String reason = body == null ? null : body.get("reason");
        PaymentResponse response = billingService.cancelPendingPayment(paymentId, reason);
        return ResponseEntity.ok(ApiResponse.ok("Payment cancelled", response));
    }
}
