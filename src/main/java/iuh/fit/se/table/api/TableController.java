package iuh.fit.se.table.api;

import iuh.fit.se.menu.api.dto.CustomerMenuCategoryResponse;
import iuh.fit.se.menu.api.dto.CustomerTrendingResponse;
import iuh.fit.se.menu.application.MenuService;
import iuh.fit.se.table.api.dto.QrInitResponse;
import iuh.fit.se.table.api.dto.TableQrCodeResponse;
import iuh.fit.se.table.api.dto.TableResponse;
import iuh.fit.se.table.api.dto.UpdateTableStatusRequest;
import iuh.fit.se.table.application.QrSessionTokenDTO;
import iuh.fit.se.table.application.TableDTO;
import iuh.fit.se.table.application.TableService;
import iuh.fit.se.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    public TableController(TableService tableService, MenuService menuService) {
        this.tableService = tableService;
        this.menuService = menuService;
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
        TableDTO table = tableService.getTableByCode(tableCode);
        return ResponseEntity.ok(ApiResponse.ok(TableResponse.from(table)));
    }

    @GetMapping("/qr/{qrKey}/init")
    public ResponseEntity<ApiResponse<QrInitResponse>> initQrByQrKey(@PathVariable("qrKey") String qrKey) {
        TableDTO table = tableService.getTableByQrKey(qrKey);
        QrSessionTokenDTO session = tableService.issueQrSession(table.tableCode());
        return ResponseEntity.ok(ApiResponse.ok("QR init loaded", QrInitResponse.from(table.tableCode(), session)));
    }

    @GetMapping("/qr/{qrKey}/menu")
    public ResponseEntity<ApiResponse<List<CustomerMenuCategoryResponse>>> getCustomerMenuByQr(
            @PathVariable("qrKey") String qrKey,
            @RequestHeader("X-QR-Session") String qrSessionId
    ) {
        TableDTO table = tableService.getTableByQrKey(qrKey);
        tableService.validateQrSession(qrSessionId, table.tableCode());
        return ResponseEntity.ok(ApiResponse.ok(menuService.getCustomerMenu()));
    }

    @GetMapping("/qr/{qrKey}/trending")
    public ResponseEntity<ApiResponse<CustomerTrendingResponse>> getTrendingByQr(
            @PathVariable("qrKey") String qrKey,
            @RequestHeader("X-QR-Session") String qrSessionId
    ) {
        TableDTO table = tableService.getTableByQrKey(qrKey);
        tableService.validateQrSession(qrSessionId, table.tableCode());
        return ResponseEntity.ok(ApiResponse.ok(menuService.getTrending()));
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
        TableDTO updated = tableService.updateTableStatus(tableCode, request.status());
        return ResponseEntity.ok(ApiResponse.ok("Table status updated", TableResponse.from(updated)));
    }
}