package iuh.fit.se.shared.tax.api;

import iuh.fit.se.shared.response.ApiResponse;
import iuh.fit.se.shared.security.JwtPrincipal;
import iuh.fit.se.shared.tax.api.dto.MenuItemPricingPreviewResponse;
import iuh.fit.se.shared.tax.api.dto.TaxConfigResponse;
import iuh.fit.se.shared.tax.api.dto.TaxConfigUpdateRequest;
import iuh.fit.se.shared.tax.application.TaxConfigService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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
     * Cập nhật cấu hình thuế toàn cục. Có hiệu lực ngay lập tức với mọi order chưa CONFIRMED.
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
