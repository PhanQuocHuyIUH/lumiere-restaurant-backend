package iuh.fit.se.billing.application.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.billing.api.dto.CreateGroupPaymentRequest;
import iuh.fit.se.billing.api.dto.CreatePaymentRequest;
import iuh.fit.se.billing.api.dto.GroupBillResponse;
import iuh.fit.se.billing.api.dto.PaymentResponse;
import iuh.fit.se.billing.api.dto.BillSummaryResponse;
import iuh.fit.se.billing.api.dto.PaymentRequestResponse;
import iuh.fit.se.billing.api.dto.RefundRequest;
import iuh.fit.se.billing.api.dto.RefundResponse;
import iuh.fit.se.billing.application.PaymentRequestService;
import iuh.fit.se.table.application.TableData;
import iuh.fit.se.table.application.TableService;
import iuh.fit.se.billing.application.BillingService;
import iuh.fit.se.billing.application.BillingWebhookResult;
import iuh.fit.se.billing.application.dto.ShiftPaymentSummary;
import iuh.fit.se.billing.application.WebhookProcessResult;
import iuh.fit.se.billing.application.WebhookService;
import iuh.fit.se.billing.domain.Payment;
import iuh.fit.se.billing.domain.PaymentMethod;
import iuh.fit.se.billing.domain.PaymentProvider;
import iuh.fit.se.billing.domain.PaymentStatus;
import iuh.fit.se.billing.domain.PaymentTransaction;
import iuh.fit.se.billing.domain.Refund;
import iuh.fit.se.billing.domain.TxnStatus;
import iuh.fit.se.billing.domain.TxnType;
import iuh.fit.se.billing.domain.PaymentWebhook;
import iuh.fit.se.billing.domain.VietQrCodec;
import iuh.fit.se.billing.domain.VnpayMessageMapper;
import iuh.fit.se.billing.repository.PaymentRepository;
import iuh.fit.se.billing.repository.PaymentTransactionRepository;
import iuh.fit.se.billing.repository.PaymentWebhookRepository;
import iuh.fit.se.billing.repository.RefundRepository;
import iuh.fit.se.ordering.api.dto.OrderResponse;
import iuh.fit.se.ordering.domain.OrderItemStatus;
import iuh.fit.se.billing.api.dto.InvoiceResponse;
import iuh.fit.se.billing.api.dto.InvoiceItem;
import iuh.fit.se.ordering.application.OrderingService;
import iuh.fit.se.shared.event.PaymentFailedEvent;
import iuh.fit.se.shared.event.PaymentSuccessEvent;
import iuh.fit.se.identity.application.StaffService;
import iuh.fit.se.shared.exception.DomainException;
import iuh.fit.se.shared.exception.ResourceNotFoundException;
import iuh.fit.se.shared.security.JwtPrincipal;
import iuh.fit.se.shared.settings.repository.SystemSettingRepository;
import iuh.fit.se.shared.util.IdempotencyUtil;
import iuh.fit.se.shift.application.ShiftService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
@Transactional
public class BillingServiceImpl implements BillingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BillingServiceImpl.class);
    private static final String OP_CREATE_PAYMENT = "CREATE_PAYMENT";
    private static final String OP_CREATE_REFUND = "CREATE_REFUND";
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> VNPAY_PAYMENT_REQUIRED_FIELDS = Set.of(
            "vnp_Version",
            "vnp_Command",
            "vnp_TmnCode",
            "vnp_Amount",
            "vnp_CurrCode",
            "vnp_CreateDate",
            "vnp_IpAddr",
            "vnp_Locale",
            "vnp_OrderInfo",
            "vnp_OrderType",
            "vnp_ReturnUrl",
            "vnp_ExpireDate",
            "vnp_TxnRef"
    );

    private final PaymentRepository paymentRepository;
    private final iuh.fit.se.menu.application.MenuService menuService;
    private final PaymentWebhookRepository paymentWebhookRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RefundRepository refundRepository;
    private final PaymentIdempotencyService paymentIdempotencyService;
    private final OrderingService orderingService;
    private final WebhookService webhookService;
    private final ShiftService shiftService;
    private final StaffService staffService;
    private final SystemSettingRepository systemSettingRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final TableService tableService;
    private final PaymentRequestService paymentRequestService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${app.payment.vnpay.endpoint:}")
    private String vnpayEndpoint;

    @Value("${app.payment.vnpay.refund-endpoint:}")
    private String vnpayRefundEndpoint;

    @Value("${app.payment.vnpay.tmn-code:}")
    private String vnpayTmnCode;

    @Value("${app.payment.vnpay.hash-secret:}")
    private String vnpayHashSecret;

    @Value("${app.payment.vnpay.return-url:}")
    private String vnpayReturnUrl;

    @Value("${app.payment.vnpay.ipn-url:}")
    private String vnpayIpnUrl;

    @Value("${app.payment.vnpay.command:pay}")
    private String vnpayCommand;

    @Value("${app.payment.vnpay.version:2.1.0}")
    private String vnpayVersion;

    @Value("${app.payment.vietqr.bank-bin:}")
    private String vietqrBankBin;

    @Value("${app.payment.vietqr.account-number:}")
    private String vietqrAccountNumber;

    @Value("${app.payment.vietqr.account-name:}")
    private String vietqrAccountName;

    @Value("${app.payment.vietqr.merchant-city:}")
    private String vietqrMerchantCity;

    @Value("${app.payment.vietqr.qr-expiry-minutes:30}")
    private int vietqrQrExpiryMinutes;

    public BillingServiceImpl(
            PaymentRepository paymentRepository,
            iuh.fit.se.menu.application.MenuService menuService,
            PaymentWebhookRepository paymentWebhookRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            RefundRepository refundRepository,
            PaymentIdempotencyService paymentIdempotencyService,
            OrderingService orderingService,
            WebhookService webhookService,
            @Autowired @org.springframework.context.annotation.Lazy ShiftService shiftService,
            StaffService staffService,
            SystemSettingRepository systemSettingRepository,
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper,
            TableService tableService,
            PaymentRequestService paymentRequestService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.paymentRepository = paymentRepository;
        this.menuService = menuService;
        this.paymentWebhookRepository = paymentWebhookRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.refundRepository = refundRepository;
        this.paymentIdempotencyService = paymentIdempotencyService;
        this.orderingService = orderingService;
        this.webhookService = webhookService;
        this.shiftService = shiftService;
        this.staffService = staffService;
        this.systemSettingRepository = systemSettingRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.tableService = tableService;
        this.paymentRequestService = paymentRequestService;
        this.messagingTemplate = messagingTemplate;
        this.restClient = RestClient.builder().build();
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceForOrder(Long orderId) {
        try {
            OrderResponse order = orderingService.getOrderDetail(orderId);

            Optional<Payment> paymentOpt = paymentRepository
                    .findTopByOrderIdAndStatusOrderByCreatedAtDesc(orderId, PaymentStatus.SUCCESS)
                    .or(() -> paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId));

            List<iuh.fit.se.ordering.api.dto.OrderItemResponse> orderItems =
                    order.items() == null ? List.<iuh.fit.se.ordering.api.dto.OrderItemResponse>of() : order.items();

            // Only items that contribute to the bill: skip cancelled lines and non-billable combo children.
            // Matches refreshOrderTotal() so JSON invoice stays consistent with totals & UI.
            List<iuh.fit.se.ordering.api.dto.OrderItemResponse> billableItems = orderItems.stream()
                    .filter(it -> it.billable() && it.status() != OrderItemStatus.CANCELLED)
                    .toList();

            // Bulk-fetch menu names only for IDs missing from the upstream response (avoid N+1).
            List<Long> missingMenuIds = billableItems.stream()
                    .filter(it -> it.menuItemId() != null
                            && (it.menuItemName() == null || it.menuItemName().isBlank()))
                    .map(iuh.fit.se.ordering.api.dto.OrderItemResponse::menuItemId)
                    .distinct()
                    .toList();
            Map<Long, String> menuNames = new HashMap<>();
            if (!missingMenuIds.isEmpty()) {
                try {
                    menuService.getMenuItemsBulk(missingMenuIds).forEach(m -> {
                        if (m != null && m.id() != null) {
                            menuNames.put(m.id(), m.name() == null ? "" : m.name());
                        }
                    });
                } catch (Exception ex) {
                    LOGGER.warn("Bulk fetch menu names failed for order {} — falling back to empty names", orderId, ex);
                }
            }

            List<InvoiceItem> items = billableItems.stream().map(it -> {
                String name = it.menuItemName();
                if (name == null || name.isBlank()) {
                    name = it.menuItemId() == null ? "" : menuNames.getOrDefault(it.menuItemId(), "");
                }
                int qty = it.quantity() == null ? 0 : it.quantity();
                BigDecimal unitPrice = it.unitPrice() == null ? BigDecimal.ZERO : it.unitPrice();
                BigDecimal subtotalLine = it.subtotal() == null ? BigDecimal.ZERO : it.subtotal();
                return new InvoiceItem(name, qty, unitPrice, subtotalLine);
            }).toList();

            BigDecimal subtotal = (order.subtotalAmount() != null) ? order.subtotalAmount() : BigDecimal.ZERO;
            BigDecimal tax = (order.taxAmount() != null) ? order.taxAmount() : BigDecimal.ZERO;
            BigDecimal discount = BigDecimal.ZERO;
            BigDecimal total = (order.totalAmount() != null) ? order.totalAmount() : subtotal.add(tax);

            String paymentMethod = paymentOpt
                    .map(p -> p.getPaymentMethod() == null ? "" : p.getPaymentMethod().name())
                    .orElse("");
            Instant paymentTime = paymentOpt.map(Payment::getPaidAt).orElse(order.paidAt());
            Long cashierId = paymentOpt.map(Payment::getCashierId).orElse(order.confirmedById());
            String cashierName = resolveCashierName(cashierId);

            // Stable invoice number: prefer paymentId so reprints always match.
            // Fall back to orderId-only marker for unpaid drafts so the receipt label is still readable.
            String invoiceNumber = paymentOpt
                    .map(p -> String.format("INV-%d-%d", orderId, p.getId()))
                    .orElseGet(() -> String.format("INV-%d-DRAFT", orderId));

            String restaurantName = systemSettingRepository.findValueByKey("restaurant.name").orElse("");
            String restaurantAddress = systemSettingRepository.findValueByKey("restaurant.address").orElse("");
            String restaurantHotline = systemSettingRepository.findValueByKey("restaurant.hotline").orElse("");

            return new InvoiceResponse(
                    orderId, invoiceNumber, items, subtotal, tax, discount, total,
                    paymentMethod, paymentTime, cashierId, cashierName,
                    restaurantName, restaurantAddress, restaurantHotline
            );
        } catch (DomainException ex) {
            // Re-throw domain errors (incl. ResourceNotFoundException) so they map to proper 4xx
            throw ex;
        } catch (Exception ex) {
            LOGGER.error("Failed to build invoice for order {}", orderId, ex);
            throw new DomainException("Unable to build invoice for order " + orderId + ": " + ex.getMessage());
        }
    }

    @Override
    public PaymentResponse createPayment(CreatePaymentRequest request, String idempotencyKey) {
        String normalizedKey = IdempotencyUtil.normalizeKey(idempotencyKey);
        return paymentIdempotencyService.executeIdempotent(
            normalizedKey,
            OP_CREATE_PAYMENT,
            () -> createPaymentInternal(request)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public GroupBillResponse getGroupBill(Long groupId) {
        var group = tableService.getTableGroupById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("TableGroup", groupId));

        List<OrderResponse> orders = orderingService.getActiveOrdersForGroup(groupId);

        List<GroupBillResponse.GroupOrderBill> orderBills = new java.util.ArrayList<>();
        List<InvoiceItem> aggregatedItems = new java.util.ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        boolean allServed = !orders.isEmpty();

        Long anchorOrderId = null;
        for (OrderResponse order : orders) {
            if (order.status() != iuh.fit.se.ordering.domain.OrderStatus.SERVED) {
                allServed = false;
            }
            if (anchorOrderId == null && order.tableId() != null
                    && order.tableId().equals(group.masterTableId())) {
                anchorOrderId = order.id();
            }
            InvoiceResponse inv = getInvoiceForOrder(order.id());
            aggregatedItems.addAll(inv.items());
            subtotal = subtotal.add(inv.subtotal() == null ? BigDecimal.ZERO : inv.subtotal());
            tax = tax.add(inv.tax() == null ? BigDecimal.ZERO : inv.tax());
            total = total.add(inv.total() == null ? BigDecimal.ZERO : inv.total());
            orderBills.add(new GroupBillResponse.GroupOrderBill(
                    order.id(),
                    order.tableId(),
                    resolveTableCodeQuietly(order.tableId()),
                    order.status() == null ? null : order.status().name(),
                    inv.subtotal(),
                    inv.tax(),
                    inv.total(),
                    inv.items()
            ));
        }
        // Fall back to the oldest order as anchor when the master table itself has no active order.
        if (anchorOrderId == null && !orders.isEmpty()) {
            anchorOrderId = orders.get(0).id();
        }

        String reason = orders.isEmpty()
                ? "Nhóm bàn chưa có order nào để thanh toán"
                : (allServed ? null : "Còn order chưa phục vụ xong trong nhóm — chưa thể thanh toán");

        return new GroupBillResponse(
                group.id(),
                group.masterTableId(),
                resolveTableCodeQuietly(group.masterTableId()),
                anchorOrderId,
                orderBills,
                aggregatedItems,
                subtotal,
                tax,
                total,
                allServed,
                reason
        );
    }

    @Override
    public PaymentResponse createGroupPayment(Long groupId, CreateGroupPaymentRequest request, String idempotencyKey) {
        String normalizedKey = IdempotencyUtil.normalizeKey(idempotencyKey);
        return paymentIdempotencyService.executeIdempotent(
            normalizedKey,
            OP_CREATE_PAYMENT,
            () -> createGroupPaymentInternal(groupId, request)
        );
    }

    private String resolveTableCodeQuietly(Long tableId) {
        if (tableId == null) return null;
        try {
            return tableService.getTableById(tableId).tableCode();
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public BillingWebhookResult processWebhook(
            PaymentProvider provider,
            String httpMethod,
            Map<String, Object> payload,
            String signature
    ) {
        Map<String, Object> safePayload = payload == null ? Map.of() : payload;

        PaymentWebhook webhook = PaymentWebhook.receive(provider, httpMethod, safePayload, signature);
        webhook = paymentWebhookRepository.save(webhook);

        WebhookProcessResult processed = webhookService.processIPN(provider, httpMethod, safePayload, signature);
        webhook.markSignatureResult(processed.signatureValid());

        Payment payment = resolvePaymentForWebhook(provider, processed);
        if (payment == null) {
            String message = provider == PaymentProvider.VNPAY
                    ? VnpayMessageMapper.ipnResponseMessage("01")
                    : "Payment not found";
            webhook.markFailed(message);
            paymentWebhookRepository.save(webhook);
            return new BillingWebhookResult(processed.signatureValid(), false, "01", message, null);
        }

        webhook.linkPayment(payment.getId());

        if (!processed.signatureValid()) {
            createWebhookTransaction(payment, provider, processed, TxnStatus.FAILED);
            String message = provider == PaymentProvider.VNPAY
                    ? VnpayMessageMapper.ipnResponseMessage("97")
                    : "Invalid signature";
            webhook.markFailed(message);
            paymentWebhookRepository.save(webhook);
            return new BillingWebhookResult(false, false, "97", message, PaymentResponse.from(payment));
        }

        if (processed.success()) {
            if (payment.getStatus() == PaymentStatus.SUCCESS || payment.getStatus() == PaymentStatus.REFUNDED) {
                webhook.markDuplicate();
                paymentWebhookRepository.save(webhook);
                return new BillingWebhookResult(
                        true,
                        true,
                        processed.responseCode(),
                        "Duplicate webhook ignored",
                        PaymentResponse.from(payment)
                );
            }

            if (!payment.isPending()) {
                String message = provider == PaymentProvider.VNPAY
                        ? VnpayMessageMapper.ipnResponseMessage("02")
                        : "Payment is not pending";
                webhook.markFailed(message);
                paymentWebhookRepository.save(webhook);
                return new BillingWebhookResult(true, false, "02", message, PaymentResponse.from(payment));
            }

            if (provider == PaymentProvider.VNPAY && !isValidVnpayAmount(payment, processed.normalizedPayload())) {
                createWebhookTransaction(payment, provider, processed, TxnStatus.FAILED);
                String message = VnpayMessageMapper.ipnResponseMessage("04");
                webhook.markFailed(message);
                paymentWebhookRepository.save(webhook);
                return new BillingWebhookResult(true, false, "04", message, PaymentResponse.from(payment));
            }

            applyWebhookMetadata(payment, provider, processed);
            payment.markSuccess(processed.providerTransactionId(), processed.responseCode(), processed.normalizedPayload());
            payment = paymentRepository.save(payment);

            createWebhookTransaction(payment, provider, processed, TxnStatus.SUCCESS);
            webhook.markProcessed();
            paymentWebhookRepository.save(webhook);

                eventPublisher.publishEvent(new PaymentSuccessEvent(
                    payment.getId(),
                    payment.getOrderId(),
                    payment.getSubtotalAmount() == null ? BigDecimal.ZERO : payment.getSubtotalAmount().toBigDecimal(),
                    payment.getTaxAmount() == null ? BigDecimal.ZERO : payment.getTaxAmount().toBigDecimal(),
                    payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount().toBigDecimal()
                ));
            return new BillingWebhookResult(true, true, processed.responseCode(), processed.message(), PaymentResponse.from(payment));
        }

        applyWebhookMetadata(payment, provider, processed);
        if (payment.isPending()) {
            payment.markFailed(processed.responseCode(), processed.normalizedPayload());
            payment = paymentRepository.save(payment);
            eventPublisher.publishEvent(new PaymentFailedEvent(payment.getId(), payment.getOrderId(), processed.message()));
        }

        createWebhookTransaction(payment, provider, processed, TxnStatus.FAILED);
        webhook.markFailed(processed.message());
        paymentWebhookRepository.save(webhook);

        return new BillingWebhookResult(
                processed.signatureValid(),
                false,
                processed.responseCode(),
                processed.message(),
            PaymentResponse.from(payment)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public BillSummaryResponse getBillSummaryForTable(String tableCode) {
        TableData table = tableService.getTableByCode(tableCode);
        // Use the non-throwing variant: when there's no active order, catching
        // ResourceNotFoundException would still mark the surrounding tx
        // rollback-only and the eventual commit would fail with
        // UnexpectedRollbackException. Optional avoids that entirely.
        OrderResponse currentOrder = tableService.findCurrentOrderByTableCode(tableCode)
                .orElse(null);
        if (currentOrder == null) {
            return emptyBillSummary(tableCode);
        }

        Long orderId = currentOrder.id();
        try {
            boolean served = currentOrder.status() == iuh.fit.se.ordering.domain.OrderStatus.SERVED;
            String reason = served ? null
                    : "Đơn hàng chưa được phục vụ xong (trạng thái: " + currentOrder.status() + ")";

            InvoiceResponse invoice = getInvoiceForOrder(orderId);
            PaymentRequestResponse activeRequest = paymentRequestService
                    .findActiveByOrderId(orderId)
                    .orElse(null);

            return new BillSummaryResponse(
                    orderId,
                    table.tableCode(),
                    (currentOrder.status() != null) ? currentOrder.status().name() : null,
                    served && activeRequest == null,
                    reason,
                    invoice.items(),
                    invoice.subtotal(),
                    invoice.tax(),
                    invoice.total(),
                    activeRequest
            );
        } catch (DomainException ex) {
            // 4xx domain errors propagate untouched
            throw ex;
        } catch (Exception ex) {
            // Anything else (NPE, JPA, mapping…) — log with full stack and surface as 400 so the
            // customer screen shows a readable message instead of a bare "Internal server error".
            LOGGER.error("getBillSummaryForTable failed for table={} orderId={}", tableCode, orderId, ex);
            throw new DomainException(
                    "Không tải được hoá đơn (order " + orderId + "): " + ex.getMessage()
            );
        }
    }

    private BillSummaryResponse emptyBillSummary(String tableCode) {
        return new BillSummaryResponse(
                null, tableCode, null,
                false, "Bạn chưa gọi món, không thể yêu cầu thanh toán",
                List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                null
        );
    }

    @Override
    public RefundResponse createRefund(Long paymentId, RefundRequest request, String idempotencyKey) {
        String normalizedKey = IdempotencyUtil.normalizeKey(idempotencyKey);
        return paymentIdempotencyService.executeIdempotent(
            normalizedKey,
            OP_CREATE_REFUND,
            () -> createRefundInternal(paymentId, request),
            RefundResponse.class
        );
    }

    @Override
    public RefundResponse confirmRefund(Long refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund", refundId));
        if (!refund.isPending()) {
            throw new DomainException("Only PENDING refunds can be confirmed (current status: " + refund.getStatus() + ")");
        }

        Payment payment = paymentRepository.findById(refund.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", refund.getPaymentId()));

        Map<String, Object> responsePayload = new LinkedHashMap<>();
        responsePayload.put("provider", payment.getProvider().name());
        responsePayload.put("confirmedBy", resolveStaffIdForCashier());
        responsePayload.put("confirmedAt", Instant.now().toString());
        responsePayload.put("note", payment.getProvider() == PaymentProvider.CASH
                ? "Cash refund handed to customer"
                : "Manual bank transfer completed");

        String providerRefundId = payment.getProvider().name() + "-MANUAL-" + refund.getId();
        refund.markSuccess(providerRefundId, responsePayload);
        refundRepository.save(refund);

        applyRefundSuccessToPayment(payment, refund);
        return RefundResponse.from(refund, resolveCashierName(refund.getRequestedBy()));
    }

    @Override
    public RefundResponse cancelPendingRefund(Long refundId, String reason) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund", refundId));
        if (!refund.isPending()) {
            throw new DomainException("Only PENDING refunds can be cancelled (current status: " + refund.getStatus() + ")");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("cancelledAt", Instant.now().toString());
        payload.put("cancelledReason", firstNonBlank(reason, "CANCELLED_BY_CASHIER"));
        payload.put("cancelledBy", resolveStaffIdForCashier());
        refund.markFailed(payload);
        refundRepository.save(refund);
        return RefundResponse.from(refund, resolveCashierName(refund.getRequestedBy()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundResponse> listRefundsForPayment(Long paymentId) {
        return refundRepository.findAllByPaymentIdOrderByCreatedAtDesc(paymentId).stream()
                .map(r -> RefundResponse.from(r, resolveCashierName(r.getRequestedBy())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundResponse> listRefundsForOrder(Long orderId) {
        List<Payment> payments = paymentRepository.findAllByOrderIdOrderByCreatedAtDesc(orderId);
        if (payments.isEmpty()) return List.of();
        return payments.stream()
                .flatMap(p -> refundRepository.findAllByPaymentIdOrderByCreatedAtDesc(p.getId()).stream())
                .map(r -> RefundResponse.from(r, resolveCashierName(r.getRequestedBy())))
                .toList();
    }

    @Override
    public PaymentResponse confirmManualPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        PaymentProvider provider = payment.getProvider();
        if (provider != PaymentProvider.VIETQR && provider != PaymentProvider.CASH) {
            throw new DomainException("Manual confirmation is only supported for VIETQR or CASH provider");
        }
        if (!payment.isPending()) {
            throw new DomainException("Payment is not pending. Current status: " + payment.getStatus());
        }
        validateShiftForPayment(payment.getShiftId());

        Long cashierId = resolveStaffIdForCashier();
        String providerTxnPrefix = provider == PaymentProvider.CASH ? "CASH-MANUAL-" : "VIETQR-MANUAL-";
        String providerTxnId = providerTxnPrefix + payment.getId();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("provider", provider.name());
        payload.put("confirmation", "MANUAL");
        payload.put("confirmedBy", cashierId);
        payload.put("confirmedAt", Instant.now().toString());
        if (payment.getProviderPayload() != null) {
            payload.put("originalPayload", payment.getProviderPayload());
        }

        payment.markSuccess(providerTxnId, "00", payload);
        payment = paymentRepository.save(payment);

        BigDecimal amount = payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount().toBigDecimal();
        PaymentTransaction transaction = PaymentTransaction.initiated(
                payment.getId(),
                payment.getProvider(),
                TxnType.QUERY,
                amount,
                payload
        );
        transaction.markSuccess(providerTxnId, HttpStatus.OK.value(), 0, payload);
        paymentTransactionRepository.save(transaction);

        eventPublisher.publishEvent(new PaymentSuccessEvent(
                payment.getId(),
                payment.getOrderId(),
                payment.getSubtotalAmount() == null ? BigDecimal.ZERO : payment.getSubtotalAmount().toBigDecimal(),
                payment.getTaxAmount() == null ? BigDecimal.ZERO : payment.getTaxAmount().toBigDecimal(),
                amount
        ));

        return PaymentResponse.from(payment);
    }

    @Override
    public PaymentResponse cancelPendingPayment(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        if (!payment.isPending()) {
            throw new DomainException("Only PENDING payments can be cancelled. Current status: " + payment.getStatus());
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("cancelled", true);
        payload.put("cancelledAt", Instant.now().toString());
        payload.put("cancelledReason", firstNonBlank(reason, "CANCELLED_BY_CASHIER"));
        if (payment.getProviderPayload() != null) {
            payload.put("originalPayload", payment.getProviderPayload());
        }

        payment.markFailed("CANCELLED", payload);
        payment = paymentRepository.save(payment);

        BigDecimal amount = payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount().toBigDecimal();
        PaymentTransaction transaction = PaymentTransaction.initiated(
                payment.getId(),
                payment.getProvider(),
                TxnType.QUERY,
                amount,
                payload
        );
        transaction.markFailed(HttpStatus.OK.value(), 0, payload);
        paymentTransactionRepository.save(transaction);

        eventPublisher.publishEvent(new PaymentFailedEvent(payment.getId(), payment.getOrderId(),
                "Cancelled: " + firstNonBlank(reason, "CANCELLED_BY_CASHIER")));

        return PaymentResponse.from(payment);
    }

    /**
     * Mark every still-PENDING payment for an order as FAILED with reason=SUPERSEDED.
     * Called inside createPaymentInternal so the cashier can freely switch payment methods
     * without piling up stale PENDING records.
     */
    private void supersedePendingPaymentsForOrder(Long orderId) {
        List<Payment> stale = paymentRepository.findAllByOrderIdAndStatus(orderId, PaymentStatus.PENDING);
        if (stale.isEmpty()) return;
        for (Payment p : stale) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("cancelled", true);
            payload.put("cancelledAt", Instant.now().toString());
            payload.put("cancelledReason", "SUPERSEDED");
            if (p.getProviderPayload() != null) {
                payload.put("originalPayload", p.getProviderPayload());
            }
            p.markFailed("SUPERSEDED", payload);
        }
        paymentRepository.saveAll(stale);
    }

    private String resolveCashierName(Long cashierId) {
        if (cashierId == null) return null;
        try {
            return staffService.getStaffById(cashierId).getName();
        } catch (Exception ex) {
            LOGGER.warn("Failed to resolve cashier name for staffId={}", cashierId, ex);
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentStatusByOrderId(Long orderId) {
        Payment payment = paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId=" + orderId));
        return PaymentResponse.from(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftPaymentSummary getShiftPaymentSummary(Long shiftId) {
        Long cashRevenueLong = paymentRepository.sumAmountByShiftIdAndStatusAndPaymentMethod(
            shiftId,
            PaymentStatus.SUCCESS,
            PaymentMethod.CASH
        );
        Long transferRevenueLong = paymentRepository.sumAmountByShiftIdAndStatusAndPaymentMethodIn(
            shiftId,
            PaymentStatus.SUCCESS,
            Set.of(PaymentMethod.QR_CODE, PaymentMethod.VNPAY_ATM)
        );
        BigDecimal cashRevenue = BigDecimal.valueOf(cashRevenueLong == null ? 0L : cashRevenueLong);
        BigDecimal transferRevenue = BigDecimal.valueOf(transferRevenueLong == null ? 0L : transferRevenueLong);
        long totalBills = paymentRepository.countByShiftIdAndStatus(shiftId, PaymentStatus.SUCCESS);

        return new ShiftPaymentSummary(shiftId, cashRevenue, transferRevenue, totalBills);
    }

    private PaymentResponse createPaymentInternal(CreatePaymentRequest request) {
        validatePaymentMethodProvider(request.paymentMethod(), request.provider());
        validateShiftForPayment(request.shiftId());

        OrderResponse order = orderingService.getOrderDetail(request.orderId());
        ensureOrderCanCreatePayment(order);

        if (paymentRepository.existsByOrderIdAndStatus(order.id(), PaymentStatus.SUCCESS)) {
            throw new DomainException("A successful payment already exists for order: " + order.id());
        }

        // Atomically mark any existing PENDING payments for this order as FAILED
        // with reason SUPERSEDED so we don't accumulate stale pending records when
        // the cashier retries with a different method.
        supersedePendingPaymentsForOrder(order.id());

        Long cashierId = resolveStaffIdForCashier();
        Payment payment = Payment.createPending(
                order.id(),
            request.shiftId(),
                order.totalAmount(),
            order.subtotalAmount(),
            order.taxAmount(),
            order.taxMode(),
            order.taxRateBps(),
                request.paymentMethod(),
                request.provider(),
                cashierId
        );
        payment = paymentRepository.save(payment);

        Map<String, Object> rawRequest = IdempotencyUtil.toJsonMap(objectMapper, request);
        PaymentTransaction transaction = PaymentTransaction.initiated(
                payment.getId(),
                payment.getProvider(),
                TxnType.INITIATE,
                payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount().toBigDecimal(),
                rawRequest
        );
        transaction = paymentTransactionRepository.save(transaction);

        GatewayActionResult gatewayResult = initiatePayment(payment, request);

        payment.registerProviderInit(
                gatewayResult.providerTransactionId(),
                gatewayResult.responseCode(),
                gatewayResult.vnpTmnCode(),
                gatewayResult.vnpBankTranNo(),
                gatewayResult.vnpTransactionStatus(),
                gatewayResult.vnpPayDate(),
                gatewayResult.bankCode(),
                gatewayResult.cardType(),
                gatewayResult.qrContent(),
                gatewayResult.qrExpiresAt(),
                gatewayResult.responsePayload()
        );

        if (gatewayResult.transition() == PaymentTransition.SUCCESS) {
            payment.markSuccess(
                    gatewayResult.providerTransactionId(),
                    gatewayResult.responseCode(),
                    gatewayResult.responsePayload()
            );
                eventPublisher.publishEvent(new PaymentSuccessEvent(
                    payment.getId(),
                    payment.getOrderId(),
                    payment.getSubtotalAmount() == null ? BigDecimal.ZERO : payment.getSubtotalAmount().toBigDecimal(),
                    payment.getTaxAmount() == null ? BigDecimal.ZERO : payment.getTaxAmount().toBigDecimal(),
                    payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount().toBigDecimal()
                ));
        } else if (gatewayResult.transition() == PaymentTransition.FAILED) {
            payment.markFailed(gatewayResult.responseCode(), gatewayResult.responsePayload());
            eventPublisher.publishEvent(new PaymentFailedEvent(payment.getId(), payment.getOrderId(), gatewayResult.message()));
        }

        payment = paymentRepository.save(payment);

        if (gatewayResult.transition() == PaymentTransition.FAILED) {
            transaction.markFailed(gatewayResult.httpStatusCode(), gatewayResult.durationMs(), gatewayResult.responsePayload());
        } else {
            transaction.markSuccess(
                    gatewayResult.providerTransactionId(),
                    gatewayResult.httpStatusCode(),
                    gatewayResult.durationMs(),
                    gatewayResult.responsePayload()
            );
        }
        paymentTransactionRepository.save(transaction);

        PaymentResponse response = PaymentResponse.from(payment);
        // Broadcast the QR / payment record to waiter app so a server can carry the
        // phone over to the table for the customer to scan. Failure to broadcast is
        // non-fatal — the cashier still has the QR on admin.
        broadcastPaymentToWaiter(payment, order, response);
        return response;
    }

    /**
     * One-payment settlement for a whole table group. The anchor Payment lives on the master order
     * but carries the group total; on success the {@link iuh.fit.se.billing.listener.BillingOrderEventListener}
     * cascade marks every member order PAID and closes the group.
     */
    private PaymentResponse createGroupPaymentInternal(Long groupId, CreateGroupPaymentRequest request) {
        validatePaymentMethodProvider(request.paymentMethod(), request.provider());
        validateShiftForPayment(request.shiftId());

        var group = tableService.getTableGroupById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("TableGroup", groupId));

        List<OrderResponse> orders = orderingService.getActiveOrdersForGroup(groupId);
        if (orders.isEmpty()) {
            throw new DomainException("Nhóm bàn chưa có order nào để thanh toán");
        }

        // Every member order must be fully served and free of an existing successful payment.
        OrderResponse anchor = null;
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (OrderResponse order : orders) {
            ensureOrderCanCreatePayment(order);
            if (paymentRepository.existsByOrderIdAndStatus(order.id(), PaymentStatus.SUCCESS)) {
                throw new DomainException("Order " + order.id() + " trong nhóm đã được thanh toán");
            }
            subtotal = subtotal.add(order.subtotalAmount() == null ? BigDecimal.ZERO : order.subtotalAmount());
            tax = tax.add(order.taxAmount() == null ? BigDecimal.ZERO : order.taxAmount());
            total = total.add(order.totalAmount() == null ? BigDecimal.ZERO : order.totalAmount());
            if (anchor == null && order.tableId() != null && order.tableId().equals(group.masterTableId())) {
                anchor = order;
            }
        }
        if (anchor == null) {
            anchor = orders.get(0);
        }
        if (total.signum() <= 0) {
            throw new DomainException("Tổng tiền nhóm bàn phải lớn hơn 0");
        }

        // Clear stale pending payments on the anchor so a method switch doesn't pile up records.
        supersedePendingPaymentsForOrder(anchor.id());

        Long cashierId = resolveStaffIdForCashier();
        Payment payment = Payment.createPendingForGroup(
                anchor.id(),
                groupId,
                request.shiftId(),
                total,
                subtotal,
                tax,
                anchor.taxMode(),
                anchor.taxRateBps(),
                request.paymentMethod(),
                request.provider(),
                cashierId
        );
        payment = paymentRepository.save(payment);

        // Synthesize the per-order request shape the gateway builders expect (amount comes from the
        // Payment, so only routing fields matter here).
        CreatePaymentRequest gatewayRequest = new CreatePaymentRequest(
                anchor.id(),
                request.shiftId(),
                request.paymentMethod(),
                request.provider(),
                request.locale(),
                request.clientIp(),
                request.bankCode()
        );

        Map<String, Object> rawRequest = IdempotencyUtil.toJsonMap(objectMapper, gatewayRequest);
        PaymentTransaction transaction = PaymentTransaction.initiated(
                payment.getId(),
                payment.getProvider(),
                TxnType.INITIATE,
                payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount().toBigDecimal(),
                rawRequest
        );
        transaction = paymentTransactionRepository.save(transaction);

        GatewayActionResult gatewayResult = initiatePayment(payment, gatewayRequest);

        payment.registerProviderInit(
                gatewayResult.providerTransactionId(),
                gatewayResult.responseCode(),
                gatewayResult.vnpTmnCode(),
                gatewayResult.vnpBankTranNo(),
                gatewayResult.vnpTransactionStatus(),
                gatewayResult.vnpPayDate(),
                gatewayResult.bankCode(),
                gatewayResult.cardType(),
                gatewayResult.qrContent(),
                gatewayResult.qrExpiresAt(),
                gatewayResult.responsePayload()
        );

        if (gatewayResult.transition() == PaymentTransition.SUCCESS) {
            payment.markSuccess(
                    gatewayResult.providerTransactionId(),
                    gatewayResult.responseCode(),
                    gatewayResult.responsePayload()
            );
            eventPublisher.publishEvent(new PaymentSuccessEvent(
                    payment.getId(),
                    payment.getOrderId(),
                    payment.getSubtotalAmount() == null ? BigDecimal.ZERO : payment.getSubtotalAmount().toBigDecimal(),
                    payment.getTaxAmount() == null ? BigDecimal.ZERO : payment.getTaxAmount().toBigDecimal(),
                    payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount().toBigDecimal()
            ));
        } else if (gatewayResult.transition() == PaymentTransition.FAILED) {
            payment.markFailed(gatewayResult.responseCode(), gatewayResult.responsePayload());
            eventPublisher.publishEvent(new PaymentFailedEvent(payment.getId(), payment.getOrderId(), gatewayResult.message()));
        }

        payment = paymentRepository.save(payment);

        if (gatewayResult.transition() == PaymentTransition.FAILED) {
            transaction.markFailed(gatewayResult.httpStatusCode(), gatewayResult.durationMs(), gatewayResult.responsePayload());
        } else {
            transaction.markSuccess(
                    gatewayResult.providerTransactionId(),
                    gatewayResult.httpStatusCode(),
                    gatewayResult.durationMs(),
                    gatewayResult.responsePayload()
            );
        }
        paymentTransactionRepository.save(transaction);

        PaymentResponse response = PaymentResponse.from(payment);
        broadcastPaymentToWaiter(payment, anchor, response);
        return response;
    }

    private void broadcastPaymentToWaiter(Payment payment, OrderResponse order, PaymentResponse response) {
        try {
            String tableCode = null;
            if (order.tableId() != null) {
                try {
                    tableCode = tableService.getTableById(order.tableId()).tableCode();
                } catch (Exception ex) {
                    LOGGER.warn("Failed to resolve tableCode for order {} during payment broadcast",
                            order.id(), ex);
                }
            }
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("event", "CREATED");
            envelope.put("tableCode", tableCode);
            envelope.put("payment", response);
            messagingTemplate.convertAndSend("/topic/waiter/payments", envelope);
        } catch (Exception ex) {
            LOGGER.warn("Failed to broadcast /topic/waiter/payments for payment {}: {}",
                    payment.getId(), ex.getMessage());
        }
    }

    private RefundResponse createRefundInternal(Long paymentId, RefundRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        if (!payment.isSuccessful()) {
            throw new DomainException("Only successful payment can be refunded");
        }

        // Cumulative validation: sum(refund SUCCESS) + new amount <= payment.amount
        BigDecimal paymentAmount = payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount().toBigDecimal();
        BigDecimal alreadyRefunded = refundRepository.sumSuccessAmountByPaymentId(payment.getId());
        if (alreadyRefunded == null) alreadyRefunded = BigDecimal.ZERO;
        BigDecimal refundable = paymentAmount.subtract(alreadyRefunded);
        if (refundable.signum() <= 0) {
            throw new DomainException("Payment has already been fully refunded");
        }
        if (request.amount().compareTo(refundable) > 0) {
            throw new DomainException("Refund amount " + request.amount()
                    + " exceeds remaining refundable amount " + refundable
                    + " (paid " + paymentAmount + ", already refunded " + alreadyRefunded + ")");
        }

        Long staffId = resolveStaffIdForCashier();
        Refund refund = Refund.createInitiated(
                payment.getId(),
                request.amount(),
                normalizeOptionalText(request.reason()),
                staffId
        );

        Map<String, Object> rawRequest = new LinkedHashMap<>();
        rawRequest.put("paymentId", payment.getId());
        rawRequest.put("amount", request.amount());
        rawRequest.put("reason", request.reason());
        rawRequest.put("provider", payment.getProvider().name());

        refund.markPending(rawRequest);
        refund = refundRepository.save(refund);

        PaymentTransaction transaction = PaymentTransaction.initiated(
            payment.getId(),
            payment.getProvider(),
            TxnType.REFUND,
            request.amount(),
            rawRequest
        );
        transaction = paymentTransactionRepository.save(transaction);

        // CASH / VIETQR: stop at PENDING — cashier must confirm via POST /payments/refunds/{id}/confirm
        // once the cash is handed over / bank transfer is done. This mirrors the CASH payment flow.
        if (payment.getProvider() == PaymentProvider.CASH || payment.getProvider() == PaymentProvider.VIETQR) {
            // Transaction stays in INITIATE/PENDING state until confirm; do not mark success here.
            return RefundResponse.from(refund, resolveCashierName(staffId));
        }

        // VNPAY: call gateway synchronously.
        RefundGatewayResult result = initiateRefund(payment, refund, request);
        if (result.success()) {
            refund.markSuccess(result.providerRefundId(), result.responsePayload());
            refund = refundRepository.save(refund);
            transaction.markSuccess(
                    result.providerRefundId(),
                    result.httpStatusCode(),
                    result.durationMs(),
                    result.responsePayload()
            );
            paymentTransactionRepository.save(transaction);
            applyRefundSuccessToPayment(payment, refund);
        } else {
            refund.markFailed(result.responsePayload());
            refundRepository.save(refund);
            transaction.markFailed(result.httpStatusCode(), result.durationMs(), result.responsePayload());
            paymentTransactionRepository.save(transaction);
        }

        return RefundResponse.from(refund, resolveCashierName(staffId));
    }

    /**
     * Apply the side effects of a refund reaching SUCCESS:
     *   1. If the cumulative refund total now equals the payment amount → mark payment REFUNDED.
     *   2. Publish PaymentRefundedEvent so the order listener can cancel the order (for full refund).
     */
    private void applyRefundSuccessToPayment(Payment payment, Refund refund) {
        BigDecimal paymentAmount = payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount().toBigDecimal();
        BigDecimal totalRefunded = refundRepository.sumSuccessAmountByPaymentId(payment.getId());
        if (totalRefunded == null) totalRefunded = BigDecimal.ZERO;
        boolean fullRefund = totalRefunded.compareTo(paymentAmount) >= 0;

        if (fullRefund && payment.getStatus() != PaymentStatus.REFUNDED) {
            Map<String, Object> markPayload = new LinkedHashMap<>();
            markPayload.put("trigger", "REFUND_SUCCESS");
            markPayload.put("refundId", refund.getId());
            markPayload.put("totalRefundedAmount", totalRefunded);
            payment.markRefunded(markPayload);
            paymentRepository.save(payment);
        }

        eventPublisher.publishEvent(new iuh.fit.se.shared.event.PaymentRefundedEvent(
                payment.getId(),
                payment.getOrderId(),
                refund.getId(),
                refund.getAmount() == null ? BigDecimal.ZERO : refund.getAmount().toBigDecimal(),
                totalRefunded,
                paymentAmount,
                fullRefund,
                refund.getReason(),
                refund.getRequestedBy()
        ));
    }

    private Payment resolvePaymentForWebhook(PaymentProvider provider, WebhookProcessResult processed) {
        if (processed.paymentId() != null) {
            Optional<Payment> byId = paymentRepository.findById(processed.paymentId());
            if (byId.isPresent()) {
                return byId.get();
            }
        }

        if (hasText(processed.providerTransactionId())) {
            Optional<Payment> byProviderTxn = paymentRepository.findTopByProviderAndProviderTransactionIdOrderByCreatedAtDesc(
                    provider,
                    processed.providerTransactionId()
            );
            if (byProviderTxn.isPresent()) {
                return byProviderTxn.get();
            }
        }

        return null;
    }

    private void createWebhookTransaction(
            Payment payment,
            PaymentProvider provider,
            WebhookProcessResult processed,
            TxnStatus status
    ) {
        PaymentTransaction transaction = PaymentTransaction.initiated(
            payment.getId(),
            provider,
            TxnType.QUERY,
            payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount().toBigDecimal(),
            processed.normalizedPayload()
        );

        if (status == TxnStatus.SUCCESS) {
            transaction.markSuccess(processed.providerTransactionId(), HttpStatus.OK.value(), 0, processed.normalizedPayload());
        } else {
            transaction.markFailed(HttpStatus.BAD_REQUEST.value(), 0, processed.normalizedPayload());
        }

        paymentTransactionRepository.save(transaction);
    }

    private void applyWebhookMetadata(Payment payment, PaymentProvider provider, WebhookProcessResult processed) {
        Map<String, Object> payload = processed.normalizedPayload();

        String vnpTmnCode = provider == PaymentProvider.VNPAY
                ? firstNonBlank(asString(payload.get("vnp_TmnCode")), payment.getVnpTmnCode())
                : payment.getVnpTmnCode();
        String vnpBankTranNo = provider == PaymentProvider.VNPAY
                ? firstNonBlank(asString(payload.get("vnp_BankTranNo")), payment.getVnpBankTranNo())
                : payment.getVnpBankTranNo();
        String vnpTransactionStatus = provider == PaymentProvider.VNPAY
                ? firstNonBlank(asString(payload.get("vnp_TransactionStatus")), payment.getVnpTransactionStatus())
                : payment.getVnpTransactionStatus();
        String vnpPayDate = provider == PaymentProvider.VNPAY
                ? firstNonBlank(asString(payload.get("vnp_PayDate")), payment.getVnpPayDate())
                : payment.getVnpPayDate();
        String bankCode = provider == PaymentProvider.VNPAY
                ? firstNonBlank(asString(payload.get("vnp_BankCode")), payment.getBankCode())
                : payment.getBankCode();
        String cardType = provider == PaymentProvider.VNPAY
                ? firstNonBlank(asString(payload.get("vnp_CardType")), payment.getCardType())
                : payment.getCardType();

        payment.registerProviderInit(
                firstNonBlank(processed.providerTransactionId(), payment.getProviderTransactionId()),
                firstNonBlank(processed.responseCode(), payment.getProviderResponseCode()),
                vnpTmnCode,
                vnpBankTranNo,
                vnpTransactionStatus,
                vnpPayDate,
                bankCode,
                cardType,
                payment.getQrContent(),
                payment.getQrExpiresAt(),
                payload
        );
    }

    private GatewayActionResult initiatePayment(Payment payment, CreatePaymentRequest request) {
        return switch (payment.getProvider()) {
            // CASH stays PENDING until the cashier explicitly confirms via /payments/{id}/confirm.
            // This prevents auto-SUCCESS when the cashier abandons the screen before collecting cash.
            case CASH -> new GatewayActionResult(
                    PaymentTransition.PENDING,
                    "CASH-" + payment.getId(),
                    "00",
                    "Cash payment awaiting cashier confirmation",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    HttpStatus.OK.value(),
                    0,
                    Map.<String, Object>of("provider", "CASH", "status", "PENDING")
            );
            case VNPAY -> buildVnpayPayment(payment, request);
            case VIETQR -> buildVietQrPayment(payment);
        };
    }

    private GatewayActionResult buildVnpayPayment(Payment payment, CreatePaymentRequest request) {
        requireConfig(vnpayEndpoint, "app.payment.vnpay.endpoint");
        requireConfig(vnpayTmnCode, "app.payment.vnpay.tmn-code");
        requireConfig(vnpayHashSecret, "app.payment.vnpay.hash-secret");
        requireConfig(vnpayReturnUrl, "app.payment.vnpay.return-url");

        String txnRef = String.valueOf(payment.getId());
        ZonedDateTime now = ZonedDateTime.now(VIETNAM_ZONE);
        ZonedDateTime expired = now.plusMinutes(15);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", vnpayVersion);
        params.put("vnp_Command", vnpayCommand);
        params.put("vnp_TmnCode", vnpayTmnCode);
        params.put("vnp_Amount", String.valueOf(toMinorUnits(payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount().toBigDecimal())));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", buildVnpayOrderInfo(payment.getOrderId()));
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", normalizeLocale(request.locale()));
        params.put("vnp_ReturnUrl", vnpayReturnUrl);
        params.put("vnp_IpAddr", normalizeIpAddress(request.clientIp()));
        params.put("vnp_CreateDate", VNPAY_TIME_FORMAT.format(now));
        params.put("vnp_ExpireDate", VNPAY_TIME_FORMAT.format(expired));

        String bankCode = normalizeVnpayBankCode(request.bankCode());
        if (hasText(bankCode)) {
            params.put("vnp_BankCode", bankCode);
        }

        validateRequiredVnpayPaymentParams(params);

        String hashData = buildQuery(params, true);
        String query = buildQuery(params, true);
        String secureHash = hmac("HmacSHA512", vnpayHashSecret, hashData);
        String payUrl = vnpayEndpoint + "?" + query + "&vnp_SecureHash=" + secureHash;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.putAll(params);
        payload.put("vnp_SecureHash", secureHash);
        payload.put("payUrl", payUrl);

        return new GatewayActionResult(
                PaymentTransition.PENDING,
                txnRef,
                "00",
                "VNPay payment initialized",
                vnpayTmnCode,
                null,
                "INIT",
                null,
                null,
                null,
                payUrl,
                Instant.now().plus(Duration.ofMinutes(15)),
                HttpStatus.OK.value(),
                0,
                payload
        );
    }

    private GatewayActionResult buildVietQrPayment(Payment payment) {
        requireConfig(vietqrBankBin, "app.payment.vietqr.bank-bin");
        requireConfig(vietqrAccountNumber, "app.payment.vietqr.account-number");

        BigDecimal amount = payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount().toBigDecimal();
        String purpose = "PAY" + payment.getId();
        String qrContent = VietQrCodec.build(new VietQrCodec.VietQrInput(
                vietqrBankBin,
                vietqrAccountNumber,
                VietQrCodec.SERVICE_TO_ACCOUNT,
                amount,
                vietqrAccountName,
                vietqrMerchantCity,
                purpose
        ));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("provider", "VIETQR");
        payload.put("bankBin", vietqrBankBin);
        payload.put("accountNumber", vietqrAccountNumber);
        payload.put("accountName", vietqrAccountName);
        payload.put("amount", amount.setScale(0, RoundingMode.HALF_UP).toPlainString());
        payload.put("purpose", purpose);
        payload.put("qrContent", qrContent);

        return new GatewayActionResult(
                PaymentTransition.PENDING,
                "VIETQR-" + payment.getId(),
                "00",
                "VietQR payment initialized",
                null,
                null,
                null,
                null,
                null,
                null,
                qrContent,
                Instant.now().plus(Duration.ofMinutes(vietqrQrExpiryMinutes)),
                HttpStatus.OK.value(),
                0,
                payload
        );
    }

    private RefundGatewayResult initiateRefund(Payment payment, Refund refund, RefundRequest request) {
        return switch (payment.getProvider()) {
            case CASH -> new RefundGatewayResult(
                    true,
                    "CASH-REFUND-" + refund.getId(),
                    "00",
                    "Cash refund success",
                    HttpStatus.OK.value(),
                    0,
                    Map.of("provider", "CASH", "refundStatus", "SUCCESS")
            );
            case VNPAY -> initiateVnpayRefund(payment, refund, request);
            case VIETQR -> new RefundGatewayResult(
                    true,
                    "VIETQR-REFUND-" + refund.getId(),
                    "00",
                    "VietQR refund recorded (manual bank transfer)",
                    HttpStatus.OK.value(),
                    0,
                    Map.of("provider", "VIETQR", "refundStatus", "SUCCESS", "note", "Refund performed via manual bank transfer")
            );
        };
    }

    private RefundGatewayResult initiateVnpayRefund(Payment payment, Refund refund, RefundRequest request) {
        requireConfig(vnpayRefundEndpoint, "app.payment.vnpay.refund-endpoint");
        requireConfig(vnpayTmnCode, "app.payment.vnpay.tmn-code");
        requireConfig(vnpayHashSecret, "app.payment.vnpay.hash-secret");

        ZonedDateTime now = ZonedDateTime.now(VIETNAM_ZONE);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("vnp_RequestId", "REF-" + refund.getId() + "-" + now.toEpochSecond());
        requestBody.put("vnp_Version", vnpayVersion);
        requestBody.put("vnp_Command", "refund");
        requestBody.put("vnp_TmnCode", vnpayTmnCode);
        requestBody.put("vnp_TransactionType", "02");
        requestBody.put("vnp_TxnRef", String.valueOf(payment.getId()));
        requestBody.put("vnp_Amount", String.valueOf(toMinorUnits(request.amount())));
        requestBody.put("vnp_TransactionNo", payment.getProviderTransactionId());
        requestBody.put("vnp_TransactionDate", firstNonBlank(payment.getVnpPayDate(), VNPAY_TIME_FORMAT.format(now)));
        requestBody.put("vnp_CreateBy", String.valueOf(resolveStaffIdForCashier()));
        requestBody.put("vnp_CreateDate", VNPAY_TIME_FORMAT.format(now));
        requestBody.put("vnp_OrderInfo", firstNonBlank(request.reason(), "Refund order " + payment.getOrderId()));

        String hashData = buildQuery(toStringMap(requestBody), false);
        String secureHash = hmac("HmacSHA512", vnpayHashSecret, hashData);
        requestBody.put("vnp_SecureHash", secureHash);

        GatewayHttpResponse response = postJson(vnpayRefundEndpoint, requestBody);
        String responseCode = asString(response.payload().get("vnp_ResponseCode"));
        boolean success = "00".equals(responseCode);

        return new RefundGatewayResult(
                success,
                asString(response.payload().get("vnp_TransactionNo")),
                responseCode,
                firstNonBlank(asString(response.payload().get("vnp_Message")), "VNPay refund response"),
                response.httpStatusCode(),
                response.durationMs(),
                mergePayload(requestBody, response.payload())
        );
    }

    private GatewayHttpResponse postJson(String url, Map<String, Object> requestBody) {
        Instant startedAt = Instant.now();
        try {
                ResponseEntity<Map<String, Object>> response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<>() {
                    });

            int durationMs = Math.toIntExact(Duration.between(startedAt, Instant.now()).toMillis());
            return new GatewayHttpResponse(response.getStatusCode().value(), toObjectMap(response.getBody()), durationMs);
        } catch (Exception ex) {
            int durationMs = Math.toIntExact(Duration.between(startedAt, Instant.now()).toMillis());
            Map<String, Object> payload = Map.of("error", firstNonBlank(ex.getMessage(), "Gateway call failed"));
            return new GatewayHttpResponse(HttpStatus.BAD_GATEWAY.value(), payload, durationMs);
        }
    }

    private void ensureOrderCanCreatePayment(OrderResponse order) {
        String status = order.status() == null ? "UNKNOWN" : order.status().name();
        if ("PAID".equals(status)) {
            throw new DomainException("Order is already paid: " + order.id());
        }
        if (!"SERVED".equals(status)) {
            throw new DomainException("Order must be SERVED before payment. Current status: " + status);
        }
        if (order.totalAmount() == null || order.totalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Order amount must be greater than zero");
        }
    }

    private void validatePaymentMethodProvider(PaymentMethod method, PaymentProvider provider) {
        boolean valid = (method == PaymentMethod.QR_CODE && (provider == PaymentProvider.VNPAY
                || provider == PaymentProvider.VIETQR))
                || (method == PaymentMethod.VNPAY_ATM && provider == PaymentProvider.VNPAY)
                || (method == PaymentMethod.CASH && provider == PaymentProvider.CASH);

        if (!valid) {
            throw new DomainException("Invalid paymentMethod/provider combination: " + method + "/" + provider);
        }
    }

    private void validateShiftForPayment(Long shiftId) {
        if (shiftId == null) {
            throw new DomainException("shiftId is required");
        }

        if (!shiftService.isShiftOpen(shiftId)) {
            throw new DomainException("Payment requires an existing open shift");
        }
    }

    private Long resolveStaffIdForCashier() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new InsufficientAuthenticationException("JWT Authorization is required");
        }

        if (!(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new InsufficientAuthenticationException("Invalid authenticated principal");
        }

        if (principal.getStaffId() == null || principal.getStaffId() <= 0) {
            throw new DomainException("Missing staffId in JWT claims");
        }

        return principal.getStaffId();
    }

    private long toMinorUnits(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100L)).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private boolean isValidVnpayAmount(Payment payment, Map<String, Object> payload) {
        if (payload == null) {
            return false;
        }

        String amountValue = asString(payload.get("vnp_Amount"));
        if (!hasText(amountValue)) {
            return false;
        }

        try {
            long callbackAmount = Long.parseLong(amountValue.trim());
            return callbackAmount == toMinorUnits(payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount().toBigDecimal());
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private String normalizeLocale(String locale) {
        if (!hasText(locale)) {
            return "vn";
        }
        return locale.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeIpAddress(String clientIp) {
        if (!hasText(clientIp)) {
            return "127.0.0.1";
        }
        return clientIp.trim();
    }

    private String normalizeVnpayBankCode(String bankCode) {
        if (!hasText(bankCode)) {
            return null;
        }
        return bankCode.trim().toUpperCase(Locale.ROOT);
    }

    private String buildVnpayOrderInfo(Long orderId) {
        return "Thanh toan don hang " + orderId;
    }

    private void validateRequiredVnpayPaymentParams(Map<String, String> params) {
        for (String field : VNPAY_PAYMENT_REQUIRED_FIELDS) {
            if (!hasText(params.get(field))) {
                throw new DomainException("Missing required VNPay payment field: " + field);
            }
        }
    }

    private String buildQuery(Map<String, String> payload, boolean encodeValue) {
        Map<String, String> sorted = new TreeMap<>(payload);
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (!hasText(entry.getValue())) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('&');
            }
            String key = encodeValue
                    ? URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                    : entry.getKey();
            String value = encodeValue
                    ? URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)
                    : entry.getValue();

            builder.append(key).append('=');
            builder.append(value);
        }
        return builder.toString();
    }

    private Map<String, String> toStringMap(Map<String, Object> source) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            result.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return result;
    }

    private String hmac(String algorithm, String secret, String data) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return toHex(bytes);
        } catch (Exception ex) {
            throw new DomainException("Unable to sign payment request");
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private Map<String, Object> toObjectMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (source == null) {
            return result;
        }
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private Map<String, Object> mergePayload(Map<String, Object> request, Map<String, Object> response) {
        Map<String, Object> merged = new LinkedHashMap<>();
        merged.put("request", request);
        merged.put("response", response);
        return merged;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String normalizeOptionalText(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String firstNonBlank(String first, String second) {
        if (hasText(first)) {
            return first;
        }
        return hasText(second) ? second : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void requireConfig(String value, String key) {
        if (!hasText(value)) {
            throw new DomainException("Missing required config: " + key);
        }
    }

    private enum PaymentTransition {
        PENDING,
        SUCCESS,
        FAILED
    }

    private record GatewayActionResult(
            PaymentTransition transition,
            String providerTransactionId,
            String responseCode,
            String message,
            String vnpTmnCode,
            String vnpBankTranNo,
            String vnpTransactionStatus,
            String vnpPayDate,
            String bankCode,
            String cardType,
            String qrContent,
            Instant qrExpiresAt,
            Integer httpStatusCode,
            Integer durationMs,
            Map<String, Object> responsePayload
    ) {
    }

    private record RefundGatewayResult(
            boolean success,
            String providerRefundId,
            String responseCode,
            String message,
            Integer httpStatusCode,
            Integer durationMs,
            Map<String, Object> responsePayload
    ) {
    }

    private record GatewayHttpResponse(
            int httpStatusCode,
            Map<String, Object> payload,
            int durationMs
    ) {
    }
}
