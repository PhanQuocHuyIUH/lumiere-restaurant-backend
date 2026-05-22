package iuh.fit.se.menu.application;

import iuh.fit.se.menu.api.dto.ComboDetailResponse;
import iuh.fit.se.menu.api.dto.CustomerMenuCategoryResponse;
import iuh.fit.se.menu.api.dto.CustomerTrendingResponse;
import iuh.fit.se.menu.api.dto.MenuCategorySummaryResponse;
import iuh.fit.se.menu.api.dto.MenuCategoryResponse;
import iuh.fit.se.menu.api.dto.MenuItemResponse;
import iuh.fit.se.menu.api.dto.manager.ManagerMenuCategoryListItemResponse;
import iuh.fit.se.menu.api.dto.manager.CookTimeSuggestionResponse;
import iuh.fit.se.menu.api.dto.manager.CreateMenuCategoryRequest;
import iuh.fit.se.menu.api.dto.manager.CreateMenuItemRequest;
import iuh.fit.se.menu.api.dto.manager.MenuCategoryDetailResponse;
import iuh.fit.se.menu.api.dto.manager.MenuItemManagerDetailResponse;
import iuh.fit.se.menu.api.dto.manager.RecipeItemResponse;
import iuh.fit.se.menu.api.dto.manager.UpdateMenuCategoryRequest;
import iuh.fit.se.menu.api.dto.manager.UpdateMenuItemRequest;
import iuh.fit.se.menu.api.dto.manager.UpsertFixedComboRequest;
import iuh.fit.se.menu.api.dto.manager.UpsertPickComboRequest;
import iuh.fit.se.menu.api.dto.manager.UpsertRecipeRequest;
import iuh.fit.se.ai.client.dto.ComboGenerateRequest;
import iuh.fit.se.ai.client.dto.ComboGenerateResponse;
import iuh.fit.se.shared.domain.TaxMode;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface MenuService {

    List<MenuCategoryResponse> getMenu();

    List<MenuCategorySummaryResponse> getStaffMenuCategories();

    List<CustomerMenuCategoryResponse> getCustomerMenu();

    CustomerTrendingResponse getTrending();

    List<ManagerMenuCategoryListItemResponse> getAllCategoriesForManager();

    List<MenuItemResponse> getAvailableItemsByCategory(Long categoryId);

    List<MenuItemResponse> getAllItemsByCategoryForManager(Long categoryId);

    MenuItemData getItem(Long id);

    ComboDetailResponse getComboDetail(Long id);

    List<MenuItemData> getMenuItemsBulk(List<Long> ids);

    List<MenuItemPricingData> getAllMenuItemsForTaxPreview();

    /** Áp dụng taxMode + taxRateBps lên tất cả menu items đang active. Trả về số items đã cập nhật. */
    int applyTaxToAllItems(TaxMode taxMode, int taxRateBps);

    MenuItemManagerDetailResponse updateMenuItemImage(Long id, MultipartFile file);

    MenuItemManagerDetailResponse createMenuItem(CreateMenuItemRequest request);

    MenuItemManagerDetailResponse updateMenuItem(Long id, UpdateMenuItemRequest request);

    void deleteMenuItem(Long id);

    MenuItemManagerDetailResponse getMenuItemManagerDetail(Long id);

    MenuItemManagerDetailResponse upsertFixedComboConfig(Long comboItemId, UpsertFixedComboRequest request);

    MenuItemManagerDetailResponse upsertPickComboConfig(Long comboItemId, UpsertPickComboRequest request);

    MenuCategoryDetailResponse createCategory(CreateMenuCategoryRequest request);

    MenuCategoryDetailResponse getCategoryDetail(Long id);

    MenuCategoryDetailResponse updateCategory(Long id, UpdateMenuCategoryRequest request);

    void deleteCategory(Long id);

    List<RecipeItemResponse> upsertRecipe(Long menuItemId, UpsertRecipeRequest request);

    List<RecipeItemResponse> getRecipe(Long menuItemId);

    void deleteRecipe(Long menuItemId);

    ComboGenerateResponse generateComboSuggestions(ComboGenerateRequest request);

    MenuItemAvailability checkIngredientAvailability(Long menuItemId, int quantity);

    /** Kitchen/Manager toggle a menu item off (e.g. món bị cháy/hỏng, không liên quan tới NL).
     *  Publishes a delta event so waiter & customer apps refresh availability without refetching. */
    MenuItemManagerDetailResponse markMenuItemUnavailable(Long menuItemId, String reason, Long staffId);

    /** Manager-only — bật lại món đã bị markUnavailable thủ công. */
    MenuItemManagerDetailResponse markMenuItemAvailable(Long menuItemId, Long staffId);

    /** Trả về list menuItemIds bị ảnh hưởng (sufficient flag thay đổi) sau khi một ingredient
     *  được điều chỉnh / nhập kho. Inventory module gọi để publish delta event. */
    List<MenuItemAvailability> recomputeAvailabilityForIngredient(Long ingredientId);

    /** Inventory module gọi sau khi adjust/import stock để menu module phát delta lên
     *  /topic/menu/availability — giữ STOMP publish ở chung 1 chỗ. */
    void publishAvailabilityDeltaForIngredient(Long ingredientId, String ingredientName, String trigger);

    CookTimeSuggestionResponse getSuggestedCookTime(Long menuItemId);

    void updateCookTime(Long menuItemId, int newCookTimeMinutes);
}
