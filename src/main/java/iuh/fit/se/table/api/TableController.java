package iuh.fit.se.table.api;

import iuh.fit.se.billing.api.dto.BillSummaryResponse;
import iuh.fit.se.billing.api.dto.PaymentRequestResponse;
import iuh.fit.se.billing.application.BillingService;
import iuh.fit.se.billing.application.PaymentRequestService;
import iuh.fit.se.billing.domain.PaymentRequestMethod;
import iuh.fit.se.menu.api.dto.CustomerMenuCategoryResponse;
import iuh.fit.se.menu.api.dto.CustomerTrendingResponse;
import iuh.fit.se.menu.application.MenuService;
import iuh.fit.se.ordering.api.dto.OrderResponse;
import iuh.fit.se.shared.exception.DomainException;
import iuh.fit.se.table.api.dto.QrInitResponse;
import iuh.fit.se.table.api.dto.TableQrCodeResponse;
import iuh.fit.se.table.api.dto.TableResponse;
import iuh.fit.se.table.api.dto.UpdateTableStatusRequest;
import iuh.fit.se.table.application.QrSessionToken;
import iuh.fit.se.table.application.TableData;
import iuh.fit.se.table.application.TableGroupData;
import iuh.fit.se.table.application.TableService;
import iuh.fit.se.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tables")
public class TableController {

    private final TableService tableService;
    private final MenuService menuService;
    private final BillingService billingService;
    private final PaymentRequestService paymentRequestService;

    public TableController(
            TableService tableService,
            MenuService menuService,
            BillingService billingService,
            PaymentRequestService paymentRequestService
    ) {
        this.tableService = tableService;
        this.menuService = menuService;
        this.billingService = billingService;
        this.paymentRequestService = paymentRequestService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TableResponse>>> getAllTables() {
        List<TableResponse> tables = tableService.getAllTables().stream()
                .map(TableResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(tables));
    }

    @GetMapping("/{tableCode}")
    public ResponseEntity<ApiResponse<TableResponse>> getTableByCode(@PathVariable("tableCode") String tableCode) {
        TableData table = tableService.getTableByCode(tableCode);
        return ResponseEntity.ok(ApiResponse.ok(TableResponse.from(table)));
    }

    @GetMapping("/{tableCode}/current-order")
    public ResponseEntity<ApiResponse<OrderResponse>> getCurrentOrderByTableCode(
            @PathVariable("tableCode") String tableCode
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tableService.getCurrentOrderByTableCode(tableCode)));
    }

    @GetMapping("/qr/{qrKey}/init")
    public ResponseEntity<ApiResponse<QrInitResponse>> initQrByQrKey(@PathVariable("qrKey") String qrKey) {
        TableData table = tableService.getTableByQrKey(qrKey);
        QrSessionToken session = tableService.issueQrSession(table.tableCode());
        return ResponseEntity.ok(ApiResponse.ok("QR init loaded", QrInitResponse.from(table.tableCode(), session)));
    }

    @GetMapping("/qr/{qrKey}/menu")
    public ResponseEntity<ApiResponse<List<CustomerMenuCategoryResponse>>> getCustomerMenuByQr(
            @PathVariable("qrKey") String qrKey,
            @RequestHeader("X-QR-Session") String qrSessionId
    ) {
        TableData table = tableService.getTableByQrKey(qrKey);
        tableService.validateQrSession(qrSessionId, table.tableCode());
        return ResponseEntity.ok(ApiResponse.ok(menuService.getCustomerMenu()));
    }

    @GetMapping("/qr/{qrKey}/trending")
    public ResponseEntity<ApiResponse<CustomerTrendingResponse>> getTrendingByQr(
            @PathVariable("qrKey") String qrKey,
            @RequestHeader("X-QR-Session") String qrSessionId
    ) {
        TableData table = tableService.getTableByQrKey(qrKey);
        tableService.validateQrSession(qrSessionId, table.tableCode());
        return ResponseEntity.ok(ApiResponse.ok(menuService.getTrending()));
    }


    @GetMapping("/qr/{qrKey}/bill-summary")
    public ResponseEntity<ApiResponse<BillSummaryResponse>> getBillSummary(
            @PathVariable("qrKey") String qrKey,
            @RequestHeader("X-QR-Session") String qrSessionId
    ) {
        TableData table = tableService.getTableByQrKey(qrKey);
        tableService.validateQrSession(qrSessionId, table.tableCode());
        return ResponseEntity.ok(ApiResponse.ok(billingService.getBillSummaryForTable(table.tableCode())));
    }

    @GetMapping("/qr/{qrKey}/payment-request")
    public ResponseEntity<ApiResponse<PaymentRequestResponse>> getCurrentPaymentRequest(
            @PathVariable("qrKey") String qrKey,
            @RequestHeader("X-QR-Session") String qrSessionId
    ) {
        TableData table = tableService.getTableByQrKey(qrKey);
        tableService.validateQrSession(qrSessionId, table.tableCode());
        return ResponseEntity.ok(ApiResponse.ok(
                paymentRequestService.findActiveByTableCode(table.tableCode()).orElse(null)));
    }

    @PostMapping("/qr/{qrKey}/payment-request")
    public ResponseEntity<ApiResponse<PaymentRequestResponse>> createPaymentRequest(
            @PathVariable("qrKey") String qrKey,
            @RequestHeader("X-QR-Session") String qrSessionId,
            @RequestBody Map<String, String> body
    ) {
        TableData table = tableService.getTableByQrKey(qrKey);
        tableService.validateQrSession(qrSessionId, table.tableCode());

        String raw = body == null ? null : body.get("preferredMethod");
        if (raw == null || raw.isBlank()) {
            throw new DomainException("preferredMethod là bắt buộc (CASH hoặc TRANSFER)");
        }
        PaymentRequestMethod method;
        try {
            method = PaymentRequestMethod.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new DomainException("preferredMethod không hợp lệ: " + raw);
        }

        PaymentRequestResponse created = paymentRequestService
                .requestForTable(table.tableCode(), qrSessionId, method);
        return ResponseEntity.ok(ApiResponse.ok("Yêu cầu thanh toán đã được gửi", created));
    }

    @GetMapping("/{tableCode}/qr-code")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<TableQrCodeResponse>> getOrCreateQrCode(
            @PathVariable("tableCode") String tableCode
    ) {
        TableQrCodeResponse response = TableQrCodeResponse.from(tableService.getOrCreateTableQrCode(tableCode));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{tableCode}/qr-code/rotate")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<TableQrCodeResponse>> rotateQrCode(
            @PathVariable("tableCode") String tableCode
    ) {
        TableQrCodeResponse response = TableQrCodeResponse.from(tableService.rotateTableQrCode(tableCode));
        return ResponseEntity.ok(ApiResponse.ok("QR code rotated", response));
    }

    @PutMapping("/{tableCode}/status")
    @PreAuthorize("hasAnyRole('WAITER', 'MANAGER')")
    public ResponseEntity<ApiResponse<TableResponse>> updateStatus(
            @PathVariable("tableCode") String tableCode,
            @Valid @RequestBody UpdateTableStatusRequest request
    ) {
        TableData updated = tableService.updateTableStatus(tableCode, request.status());
        return ResponseEntity.ok(ApiResponse.ok("Table status updated", TableResponse.from(updated)));
    }

    @PostMapping("/{fromCode}/move-to/{toCode}")
    @PreAuthorize("hasAnyRole('WAITER', 'MANAGER')")
    public ResponseEntity<ApiResponse<TableResponse>> moveTable(
            @PathVariable("fromCode") String fromCode,
            @PathVariable("toCode") String toCode
    ) {
        TableData updated = tableService.moveTable(fromCode, toCode);
        return ResponseEntity.ok(ApiResponse.ok("Order moved to new table", TableResponse.from(updated)));
    }

    @PostMapping("/{masterCode}/group")
    @PreAuthorize("hasAnyRole('WAITER', 'MANAGER')")
    public ResponseEntity<ApiResponse<TableGroupData>> createGroup(
            @PathVariable("masterCode") String masterCode,
            @RequestBody Map<String, Object> body
    ) {
        @SuppressWarnings("unchecked")
        List<String> memberCodes = (List<String>) body.getOrDefault("memberCodes", List.of());
        String note = (String) body.get("note");
        TableGroupData group = tableService.createTableGroup(masterCode, memberCodes, note);
        return ResponseEntity.ok(ApiResponse.ok("Table group created", group));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/groups/{groupId}")
    @PreAuthorize("hasAnyRole('WAITER', 'MANAGER')")
    public ResponseEntity<ApiResponse<TableGroupData>> closeGroup(
            @PathVariable("groupId") Long groupId
    ) {
        TableGroupData group = tableService.closeTableGroup(groupId);
        return ResponseEntity.ok(ApiResponse.ok("Table group closed", group));
    }

    @GetMapping("/groups")
    @PreAuthorize("hasAnyRole('WAITER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<TableGroupData>>> getOpenGroups() {
        return ResponseEntity.ok(ApiResponse.ok(tableService.getOpenTableGroups()));
    }
}