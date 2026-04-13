package iuh.fit.se.catalog.api;

import iuh.fit.se.catalog.api.dto.MenuCategoryResponse;
import iuh.fit.se.catalog.api.dto.TableQrCodeResponse;
import iuh.fit.se.catalog.api.dto.TableResponse;
import iuh.fit.se.catalog.application.CatalogService;
import iuh.fit.se.catalog.application.QrSessionTokenDTO;
import iuh.fit.se.catalog.application.TableDTO;
import iuh.fit.se.shared.response.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/menu/tables")
public class TableController {

    private final CatalogService catalogService;

    public TableController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/{tableCode}")
    public ResponseEntity<ApiResponse<TableResponse>> getTableByCode(@PathVariable("tableCode") String tableCode) {
        TableDTO table = catalogService.getTableByCode(tableCode);
        return ResponseEntity.ok(ApiResponse.ok(TableResponse.from(table)));
    }

    @GetMapping("/qr/{qrKey}/qr-init")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getQrInitByQrKey(@PathVariable("qrKey") String qrKey) {
        TableDTO table = catalogService.getTableByQrKey(qrKey);
        List<MenuCategoryResponse> categories = catalogService.getMenu();
        QrSessionTokenDTO session = catalogService.issueQrSession(table.tableCode());

        Map<String, Object> payload = Map.of(
                "table", TableResponse.from(table),
                "categories", categories,
                "session", session
        );

        return ResponseEntity.ok(ApiResponse.ok("QR init loaded", payload));
    }

    @GetMapping("/{tableCode}/qr-code")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<TableQrCodeResponse>> getOrCreateQrCode(
            @PathVariable("tableCode") String tableCode
    ) {
        TableQrCodeResponse response = TableQrCodeResponse.from(catalogService.getOrCreateTableQrCode(tableCode));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{tableCode}/qr-code/rotate")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<TableQrCodeResponse>> rotateQrCode(
            @PathVariable("tableCode") String tableCode
    ) {
        TableQrCodeResponse response = TableQrCodeResponse.from(catalogService.rotateTableQrCode(tableCode));
        return ResponseEntity.ok(ApiResponse.ok("QR code rotated", response));
    }
}