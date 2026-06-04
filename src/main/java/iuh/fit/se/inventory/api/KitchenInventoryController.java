package iuh.fit.se.inventory.api;

import iuh.fit.se.inventory.api.dto.AdjustStockRequest;
import iuh.fit.se.inventory.api.dto.ImportStockRequest;
import iuh.fit.se.inventory.api.dto.IngredientResponse;
import iuh.fit.se.inventory.application.InventoryService;
import iuh.fit.se.shared.response.ApiResponse;
import iuh.fit.se.shared.security.JwtPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kitchen/inventory")
@PreAuthorize("hasAnyRole('KITCHEN', 'MANAGER')")
public class KitchenInventoryController {

    private final InventoryService inventoryService;

    public KitchenInventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<IngredientResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getAllIngredients()));
    }

    @PutMapping("/{id}/adjust")
    public ResponseEntity<ApiResponse<IngredientResponse>> adjust(
            @PathVariable("id") Long id,
            @Valid @RequestBody AdjustStockRequest request,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        IngredientResponse result = inventoryService.adjustStock(id, request, principal.getStaffId());
        return ResponseEntity.ok(ApiResponse.ok("Stock adjusted", result));
    }

    /**
     * Nhập kho (kèm hạn dùng) ngay từ KDS — bếp nhận hàng giao có thể nhập lô mới
     * và khai báo hạn dùng theo ngày hoặc số ngày sử dụng. Lô được theo dõi FEFO.
     */
    @PostMapping("/stock/import")
    public ResponseEntity<ApiResponse<IngredientResponse>> importStock(
            @Valid @RequestBody ImportStockRequest request,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        IngredientResponse result = inventoryService.importStock(request, principal.getStaffId());
        return ResponseEntity.ok(ApiResponse.ok("Stock imported", result));
    }
}
