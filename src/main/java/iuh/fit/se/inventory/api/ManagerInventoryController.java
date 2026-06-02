package iuh.fit.se.inventory.api;

import iuh.fit.se.inventory.api.dto.CreateIngredientRequest;
import iuh.fit.se.inventory.api.dto.ExpiringLotResponse;
import iuh.fit.se.inventory.api.dto.ImportStockRequest;
import iuh.fit.se.inventory.api.dto.IngredientResponse;
import iuh.fit.se.inventory.api.dto.StockTransactionResponse;
import iuh.fit.se.inventory.api.dto.UpdateIngredientRequest;
import iuh.fit.se.inventory.api.dto.WasteLotRequest;
import iuh.fit.se.inventory.application.InventoryService;
import iuh.fit.se.shared.response.ApiResponse;
import iuh.fit.se.shared.security.JwtPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/inventory")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerInventoryController {

    private final InventoryService inventoryService;

    public ManagerInventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/ingredients")
    public ResponseEntity<ApiResponse<IngredientResponse>> create(
            @Valid @RequestBody CreateIngredientRequest request
    ) {
        IngredientResponse created = inventoryService.createIngredient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Ingredient created", created));
    }

    @GetMapping("/ingredients")
    public ResponseEntity<ApiResponse<List<IngredientResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getAllIngredients()));
    }

    @GetMapping("/ingredients/{id}")
    public ResponseEntity<ApiResponse<IngredientResponse>> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getIngredient(id)));
    }

    @PutMapping("/ingredients/{id}")
    public ResponseEntity<ApiResponse<IngredientResponse>> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateIngredientRequest request
    ) {
        IngredientResponse updated = inventoryService.updateIngredient(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Ingredient updated", updated));
    }

    @DeleteMapping("/ingredients/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        inventoryService.deleteIngredient(id);
        return ResponseEntity.ok(ApiResponse.ok("Ingredient deleted", null));
    }

    @PostMapping("/stock/import")
    public ResponseEntity<ApiResponse<IngredientResponse>> importStock(
            @Valid @RequestBody ImportStockRequest request,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        IngredientResponse result = inventoryService.importStock(request, principal.getStaffId());
        return ResponseEntity.ok(ApiResponse.ok("Stock imported", result));
    }

    @GetMapping("/stock/low")
    public ResponseEntity<ApiResponse<List<IngredientResponse>>> getLowStock() {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getLowStockIngredients()));
    }

    @GetMapping("/stock/{ingredientId}/history")
    public ResponseEntity<ApiResponse<List<StockTransactionResponse>>> getHistory(
            @PathVariable("ingredientId") Long ingredientId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getStockHistory(ingredientId)));
    }

    @GetMapping("/expiring")
    public ResponseEntity<ApiResponse<List<ExpiringLotResponse>>> getExpiring(
            @RequestParam(value = "days", defaultValue = "3") int days
    ) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getExpiringLots(days)));
    }

    @PostMapping("/lots/{id}/waste")
    public ResponseEntity<ApiResponse<Void>> wasteLot(
            @PathVariable("id") Long id,
            @Valid @RequestBody WasteLotRequest request,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        inventoryService.wasteLot(id, principal.getStaffId(), request.reason());
        return ResponseEntity.ok(ApiResponse.ok("Lot marked as waste", null));
    }
}
