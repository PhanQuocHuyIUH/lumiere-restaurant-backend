package iuh.fit.se.billing.application;

import iuh.fit.se.billing.api.dto.PaymentRequestResponse;
import iuh.fit.se.billing.domain.PaymentRequestMethod;
import java.util.List;
import java.util.Optional;

/**
 * UC008 — Khách hàng yêu cầu thanh toán.
 *
 * Customer (via QR session) signals the cashier that they want to pay.
 * The request stays in REQUESTED → ACKNOWLEDGED → COMPLETED until a payment
 * SUCCESS event auto-completes it, or the cashier cancels it.
 */
public interface PaymentRequestService {

    /**
     * Create (or return existing active) PaymentRequest for the active order at this table.
     * Validates: order exists at table, status = SERVED, no other active request.
     */
    PaymentRequestResponse requestForTable(String tableCode, String qrSessionId, PaymentRequestMethod method);

    /** Return active request for an order, if any (used by guards + customer poll). */
    Optional<PaymentRequestResponse> findActiveByOrderId(Long orderId);

    /** Return active request for the latest order of a table — used by customer GET endpoint. */
    Optional<PaymentRequestResponse> findActiveByTableCode(String tableCode);

    /** All requests in the given statuses, ordered oldest-first (cashier queue). */
    List<PaymentRequestResponse> listByStatuses(List<String> statuses);

    PaymentRequestResponse acknowledge(Long requestId, Long staffId);

    PaymentRequestResponse cancel(Long requestId, String reason);

    /** Auto-completion hook called by BillingOrderEventListener on PaymentSuccessEvent. */
    void completeActiveForOrderQuietly(Long orderId);
}
