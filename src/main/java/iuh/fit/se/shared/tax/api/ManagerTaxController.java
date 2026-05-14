package iuh.fit.se.shared.tax.api;

import iuh.fit.se.shared.response.ApiResponse;
import iuh.fit.se.shared.security.JwtPrincipal;
import iuh.fit.se.shared.tax.api.dto.MenuItemPricingPreviewResponse;
import iuh.fit.se.shared.tax.api.dto.TaxConfigResponse;
import iuh.fit.se.shared.tax.api.dto.TaxConfigUpdateRequest;
import iuh.fit.se.shared.tax.application.TaxConfigService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/tax")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerTaxController {

    private final TaxConfigService taxConfigService;

    public ManagerTaxController(TaxConfigService taxConfigService) {
        this.taxConfigService = taxConfigService;
    }

    /**
     * GET /manager/tax/config
     * Trả về cấu hình thuế toàn cục hiện tại.
     */
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<TaxConfigResponse>> getConfig() {
        return ResponseEntity.ok(ApiResponse.ok(taxConfigService.getActiveAsResponse()));
    }

    /**
     * PUT /manager/tax/config
     * Cập nhật template thuế toàn cục (dùng làm mặc định khi tạo món mới).
     * KHÔNG ảnh hưởng các menu items đã có — dùng POST /config/apply-to-all để áp dụng hàng loạt.
     */
    @PutMapping("/config")
    public ResponseEntity<ApiResponse<TaxConfigResponse>> updateConfig(
            @Valid @RequestBody TaxConfigUpdateRequest request,
            Authentication authentication
    ) {
        Long staffId = extractStaffId(authentication);
        TaxConfigResponse updated = taxConfigService.update(request.taxMode(), request.taxRateBps(), staffId);
        return ResponseEntity.ok(ApiResponse.ok("Tax config updated", updated));
    }

    /**
     * POST /manager/tax/config/apply-to-all
     * Cập nhật template VÀ áp dụng ngay xuống tất cả menu items đang active.
     * Trả về số items đã được cập nhật.
     */
    @PostMapping("/config/apply-to-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> applyToAll(
            @Valid @RequestBody TaxConfigUpdateRequest request,
            Authentication authentication
    ) {
        Long staffId = extractStaffId(authentication);
        int updatedCount = taxConfigService.applyToAllMenuItems(request.taxMode(), request.taxRateBps(), staffId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Tax config applied to all menu items",
                Map.of("taxMode", request.taxMode(),
                       "taxRateBps", request.taxRateBps(),
                       "updatedItemCount", updatedCount)
        ));
    }

    /**
     * GET /manager/tax/preview/menu-items
     * Xem trước giá gross/net/tax của từng menu item theo cấu hình thuế hiện tại
     * (bao gồm override per-item nếu có).
     */
    @GetMapping("/preview/menu-items")
    public ResponseEntity<ApiResponse<MenuItemPricingPreviewResponse>> previewMenuItems() {
        return ResponseEntity.ok(ApiResponse.ok(taxConfigService.previewMenuItemPricing()));
    }

    private Long extractStaffId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof JwtPrincipal principal) {
            return principal.getStaffId();
        }
        return null;
    }
}
