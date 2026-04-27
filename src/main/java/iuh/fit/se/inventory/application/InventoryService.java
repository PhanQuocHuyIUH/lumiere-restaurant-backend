package iuh.fit.se.inventory.application;

import iuh.fit.se.inventory.api.dto.AdjustStockRequest;
import iuh.fit.se.inventory.api.dto.CreateIngredientRequest;
import iuh.fit.se.inventory.api.dto.ImportStockRequest;
import iuh.fit.se.inventory.api.dto.IngredientResponse;
import iuh.fit.se.inventory.api.dto.StockTransactionResponse;
import iuh.fit.se.inventory.api.dto.UpdateIngredientRequest;
import java.util.List;

public interface InventoryService {

    // Manager CRUD
    IngredientResponse createIngredient(CreateIngredientRequest request);

    List<IngredientResponse> getAllIngredients();

    IngredientResponse getIngredient(Long id);

    IngredientResponse updateIngredient(Long id, UpdateIngredientRequest request);

    void deleteIngredient(Long id);

    // Stock operations (Manager + Kitchen)
    IngredientResponse importStock(ImportStockRequest request, Long staffId);

    IngredientResponse adjustStock(Long ingredientId, AdjustStockRequest request, Long staffId);

    // Views
    List<IngredientResponse> getLowStockIngredients();

    List<StockTransactionResponse> getStockHistory(Long ingredientId);
}
