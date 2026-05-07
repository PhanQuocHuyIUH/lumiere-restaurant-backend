package iuh.fit.se.menu.application;

import iuh.fit.se.menu.api.dto.CustomerMenuCategoryResponse;
import iuh.fit.se.menu.api.dto.MenuCategorySummaryResponse;
import iuh.fit.se.menu.api.dto.MenuCategoryResponse;
import iuh.fit.se.menu.api.dto.MenuItemResponse;
import iuh.fit.se.menu.api.dto.admin.AdminMenuCategoryListItemResponse;
import iuh.fit.se.menu.api.dto.admin.CookTimeSuggestionResponse;
import iuh.fit.se.menu.api.dto.admin.CreateMenuCategoryRequest;
import iuh.fit.se.menu.api.dto.admin.CreateMenuItemRequest;
import iuh.fit.se.menu.api.dto.admin.MenuCategoryDetailResponse;
import iuh.fit.se.menu.api.dto.admin.MenuItemAdminDetailResponse;
import iuh.fit.se.menu.api.dto.admin.RecipeItemResponse;
import iuh.fit.se.menu.api.dto.admin.UpdateMenuCategoryRequest;
import iuh.fit.se.menu.api.dto.admin.UpdateMenuItemRequest;
import iuh.fit.se.menu.api.dto.admin.UpsertFixedComboRequest;
import iuh.fit.se.menu.api.dto.admin.UpsertPickComboRequest;
import iuh.fit.se.menu.api.dto.admin.UpsertRecipeRequest;
import iuh.fit.se.shared.ai.client.dto.ComboGenerateRequest;
import iuh.fit.se.shared.ai.client.dto.ComboGenerateResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface MenuService {

    List<MenuCategoryResponse> getMenu();

    List<MenuCategorySummaryResponse> getStaffMenuCategories();

    List<CustomerMenuCategoryResponse> getCustomerMenu();

    List<AdminMenuCategoryListItemResponse> getAllCategoriesForAdmin();

    List<MenuItemResponse> getAvailableItemsByCategory(Long categoryId);

    MenuItemDTO getItem(Long id);

    MenuItemDTO updateMenuItemImage(Long id, MultipartFile file);

    MenuItemAdminDetailResponse createMenuItem(CreateMenuItemRequest request);

    MenuItemAdminDetailResponse updateMenuItem(Long id, UpdateMenuItemRequest request);

    void deleteMenuItem(Long id);

    MenuItemAdminDetailResponse getMenuItemAdminDetail(Long id);

    MenuItemAdminDetailResponse upsertFixedComboConfig(Long comboItemId, UpsertFixedComboRequest request);

    MenuItemAdminDetailResponse upsertPickComboConfig(Long comboItemId, UpsertPickComboRequest request);

    MenuCategoryDetailResponse createCategory(CreateMenuCategoryRequest request);

    MenuCategoryDetailResponse getCategoryDetail(Long id);

    MenuCategoryDetailResponse updateCategory(Long id, UpdateMenuCategoryRequest request);

    void deleteCategory(Long id);

    List<RecipeItemResponse> upsertRecipe(Long menuItemId, UpsertRecipeRequest request);

    List<RecipeItemResponse> getRecipe(Long menuItemId);

    void deleteRecipe(Long menuItemId);

    ComboGenerateResponse generateComboSuggestions(ComboGenerateRequest request);

    MenuItemAvailabilityDTO checkIngredientAvailability(Long menuItemId, int quantity);

    CookTimeSuggestionResponse getSuggestedCookTime(Long menuItemId);

    void updateCookTime(Long menuItemId, int newCookTimeMinutes);
}
