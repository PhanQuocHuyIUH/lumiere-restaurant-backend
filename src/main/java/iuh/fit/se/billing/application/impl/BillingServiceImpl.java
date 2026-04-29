package iuh.fit.se.billing.application.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.billing.api.dto.CreatePaymentRequest;
import iuh.fit.se.billing.api.dto.PaymentResponse;
import iuh.fit.se.billing.api.dto.RefundRequest;
import iuh.fit.se.billing.application.BillingService;
import iuh.fit.se.billing.application.BillingWebhookResult;
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
import iuh.fit.se.billing.domain.VnpayMessageMapper;
import iuh.fit.se.billing.infrastructure.PaymentRepository;
import iuh.fit.se.billing.infrastructure.PaymentTransactionRepository;
import iuh.fit.se.billing.infrastructure.PaymentWebhookRepository;
import iuh.fit.se.billing.infrastructure.RefundRepository;
import iuh.fit.se.ordering.api.dto.OrderResponse;
import iuh.fit.se.ordering.application.OrderingService;
import iuh.fit.se.shared.event.PaymentFailedEvent;
import iuh.fit.se.shared.event.PaymentSuccessEvent;
import iuh.fit.se.shared.exception.DomainException;
import iuh.fit.se.shared.exception.ResourceNotFoundException;
import iuh.fit.se.shared.security.JwtPrincipal;
import iuh.fit.se.shared.util.IdempotencyUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
@Transactional
public class BillingServiceImpl implements BillingService {

    private static final String IDEM_MODULE = "billing";
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
    private final PaymentWebhookRepository paymentWebhookRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RefundRepository refundRepository;
    private final PaymentIdempotencyService paymentIdempotencyService;
    private final OrderingService orderingService;
    private final WebhookService webhookService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

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

    public BillingServiceImpl(
            PaymentRepository paymentRepository,
            PaymentWebhookRepository paymentWebhookRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            RefundRepository refundRepository,
                PaymentIdempotencyService paymentIdempotencyService,
            OrderingService orderingService,
            WebhookService webhookService,
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentWebhookRepository = paymentWebhookRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.refundRepository = refundRepository;
        this.paymentIdempotencyService = paymentIdempotencyService;
        this.orderingService = orderingService;
        this.webhookService = webhookService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
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

            eventPublisher.publishEvent(new PaymentSuccessEvent(payment.getId(), payment.getOrderId(), payment.getAmount()));
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
    public PaymentResponse createRefund(Long paymentId, RefundRequest request, String idempotencyKey) {
        String normalizedKey = IdempotencyUtil.normalizeKey(idempotencyKey);
        return paymentIdempotencyService.executeIdempotent(
            normalizedKey,
            OP_CREATE_REFUND,
            () -> createRefundInternal(paymentId, request)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentStatusByOrderId(Long orderId) {
        Payment payment = paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId=" + orderId));
        return PaymentResponse.from(payment);
    }

    private PaymentResponse createPaymentInternal(CreatePaymentRequest request) {
        validatePaymentMethodProvider(request.paymentMethod(), request.provider());

        OrderResponse order = orderingService.getOrderDetail(request.orderId());
        ensureOrderCanCreatePayment(order);

        if (paymentRepository.existsByOrderIdAndStatus(order.id(), PaymentStatus.SUCCESS)) {
            throw new DomainException("A successful payment already exists for order: " + order.id());
        }

        Long cashierId = resolveStaffIdForCashier();
        Payment payment = Payment.createPending(
                order.id(),
                order.totalAmount(),
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
                payment.getAmount(),
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
            eventPublisher.publishEvent(new PaymentSuccessEvent(payment.getId(), payment.getOrderId(), payment.getAmount()));
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

        return PaymentResponse.from(payment);
    }

    private PaymentResponse createRefundInternal(Long paymentId, RefundRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        if (!payment.isSuccessful()) {
            throw new DomainException("Only successful payment can be refunded");
        }

        if (request.amount().compareTo(payment.getAmount()) > 0) {
            throw new DomainException("Refund amount cannot exceed payment amount");
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

        RefundGatewayResult result = initiateRefund(payment, refund, request);
        if (result.success()) {
            refund.markSuccess(result.providerRefundId(), result.responsePayload());
            if (request.amount().compareTo(payment.getAmount()) == 0) {
                payment.markRefunded(result.responsePayload());
                payment = paymentRepository.save(payment);
            }
            transaction.markSuccess(
                    result.providerRefundId(),
                    result.httpStatusCode(),
                    result.durationMs(),
                    result.responsePayload()
            );
        } else {
            refund.markFailed(result.responsePayload());
            transaction.markFailed(result.httpStatusCode(), result.durationMs(), result.responsePayload());
        }

        refundRepository.save(refund);
        paymentTransactionRepository.save(transaction);
        return PaymentResponse.from(payment);
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
                payment.getAmount(),
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
            case CASH -> new GatewayActionResult(
                    PaymentTransition.SUCCESS,
                    "CASH-" + payment.getId(),
                    "00",
                    "Cash payment accepted",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Instant.now(),
                    HttpStatus.OK.value(),
                    0,
                    Map.<String, Object>of("provider", "CASH", "status", "SUCCESS")
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
        params.put("vnp_Amount", String.valueOf(toMinorUnits(payment.getAmount())));
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
        String content = "VIETQR|PAY-" + payment.getId() + "|" + payment.getAmount().setScale(0, RoundingMode.HALF_UP).toPlainString();
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
                content,
                Instant.now().plus(Duration.ofMinutes(15)),
                HttpStatus.OK.value(),
                0,
                Map.of("qrContent", content)
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
                    false,
                    null,
                    "UNSUPPORTED",
                    "VIETQR refund is not supported",
                    HttpStatus.BAD_REQUEST.value(),
                    0,
                    Map.of("error", "VIETQR refund is not supported")
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
            return callbackAmount == toMinorUnits(payment.getAmount());
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
