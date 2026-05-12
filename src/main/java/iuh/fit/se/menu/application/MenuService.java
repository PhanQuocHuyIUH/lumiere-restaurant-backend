package iuh.fit.se.menu.application;

import iuh.fit.se.menu.api.dto.CustomerMenuCategoryResponse;
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
import iuh.fit.se.shared.ai.client.dto.ComboGenerateRequest;
import iuh.fit.se.shared.ai.client.dto.ComboGenerateResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface MenuService {

    List<MenuCategoryResponse> getMenu();

    List<MenuCategorySummaryResponse> getStaffMenuCategories();

    List<CustomerMenuCategoryResponse> getCustomerMenu();

    List<ManagerMenuCategoryListItemResponse> getAllCategoriesForManager();

    List<MenuItemResponse> getAvailableItemsByCategory(Long categoryId);

    MenuItemDTO getItem(Long id);

    MenuItemDTO updateMenuItemImage(Long id, MultipartFile file);

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

    MenuItemAvailabilityDTO checkIngredientAvailability(Long menuItemId, int quantity);

    CookTimeSuggestionResponse getSuggestedCookTime(Long menuItemId);

    void updateCookTime(Long menuItemId, int newCookTimeMinutes);
}
