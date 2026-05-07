package iuh.fit.se.menu.api;

import iuh.fit.se.menu.api.dto.admin.CreateMenuItemRequest;
import iuh.fit.se.menu.api.dto.admin.GenerateComboSuggestionsRequest;
import iuh.fit.se.menu.api.dto.admin.MenuItemAdminDetailResponse;
import iuh.fit.se.menu.api.dto.admin.RecipeItemResponse;
import iuh.fit.se.menu.api.dto.admin.UpdateMenuItemRequest;
import iuh.fit.se.menu.api.dto.admin.UpsertFixedComboRequest;
import iuh.fit.se.menu.api.dto.admin.UpsertPickComboRequest;
import iuh.fit.se.menu.api.dto.admin.UpsertRecipeRequest;
import iuh.fit.se.menu.application.MenuService;
import iuh.fit.se.shared.ai.client.dto.ComboGenerateResponse;
import iuh.fit.se.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/menu/items")
@PreAuthorize("hasRole('MANAGER')")
public class AdminMenuItemController {

    private final MenuService menuService;

    public AdminMenuItemController(MenuService menuService) {
        this.menuService = menuService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MenuItemAdminDetailResponse>> create(@Valid @RequestBody CreateMenuItemRequest request) {
        MenuItemAdminDetailResponse created = menuService.createMenuItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Menu item created", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuItemAdminDetailResponse>> getDetail(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.getMenuItemAdminDetail(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuItemAdminDetailResponse>> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateMenuItemRequest request
    ) {
        MenuItemAdminDetailResponse updated = menuService.updateMenuItem(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Menu item updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        menuService.deleteMenuItem(id);
        return ResponseEntity.ok(ApiResponse.ok("Menu item deleted", null));
    }

    @PutMapping("/{id}/combo/fixed")
    public ResponseEntity<ApiResponse<MenuItemAdminDetailResponse>> upsertFixedCombo(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpsertFixedComboRequest request
    ) {
        MenuItemAdminDetailResponse updated = menuService.upsertFixedComboConfig(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Fixed combo config updated", updated));
    }

    @PutMapping("/{id}/combo/pick")
    public ResponseEntity<ApiResponse<MenuItemAdminDetailResponse>> upsertPickCombo(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpsertPickComboRequest request
    ) {
        MenuItemAdminDetailResponse updated = menuService.upsertPickComboConfig(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Pick combo config updated", updated));
    }

    @PutMapping("/{id}/recipe")
    public ResponseEntity<ApiResponse<List<RecipeItemResponse>>> upsertRecipe(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpsertRecipeRequest request
    ) {
        List<RecipeItemResponse> result = menuService.upsertRecipe(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Recipe updated", result));
    }

    @GetMapping("/{id}/recipe")
    public ResponseEntity<ApiResponse<List<RecipeItemResponse>>> getRecipe(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.getRecipe(id)));
    }

    @DeleteMapping("/{id}/recipe")
    public ResponseEntity<ApiResponse<Void>> deleteRecipe(@PathVariable("id") Long id) {
        menuService.deleteRecipe(id);
        return ResponseEntity.ok(ApiResponse.ok("Recipe deleted", null));
    }

    @PostMapping("/combo-generate")
    public ResponseEntity<ApiResponse<ComboGenerateResponse>> generateComboSuggestions(
            @Valid @RequestBody GenerateComboSuggestionsRequest request
    ) {
        ComboGenerateResponse response = menuService.generateComboSuggestions(request.toAiRequest());
        return ResponseEntity.ok(ApiResponse.ok("Combo suggestions generated", response));
    }

    @GetMapping("/{id}/cooktime-suggest")
    public ResponseEntity<ApiResponse<iuh.fit.se.menu.api.dto.admin.CookTimeSuggestionResponse>> getSuggestedCookTime(
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.getSuggestedCookTime(id)));
    }

    @PutMapping("/{id}/cooktime")
    public ResponseEntity<ApiResponse<Void>> updateCookTime(
            @PathVariable("id") Long id,
            @RequestBody java.util.Map<String, Integer> payload
    ) {
        Integer newCookTime = payload.get("cookTime");
        if (newCookTime == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("cookTime is required", null));
        }
        menuService.updateCookTime(id, newCookTime);
        return ResponseEntity.ok(ApiResponse.ok("Cook time updated", null));
    }
}
