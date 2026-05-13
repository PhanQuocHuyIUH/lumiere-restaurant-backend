package iuh.fit.se.menu.api;

import iuh.fit.se.menu.api.dto.manager.CreateMenuItemRequest;
import iuh.fit.se.menu.api.dto.manager.CookTimeSuggestionResponse;
import iuh.fit.se.menu.api.dto.manager.GenerateComboSuggestionsRequest;
import iuh.fit.se.menu.api.dto.manager.MenuItemManagerDetailResponse;
import iuh.fit.se.menu.api.dto.manager.RecipeItemResponse;
import iuh.fit.se.menu.api.dto.manager.UpdateMenuItemRequest;
import iuh.fit.se.menu.api.dto.manager.UpsertFixedComboRequest;
import iuh.fit.se.menu.api.dto.manager.UpsertPickComboRequest;
import iuh.fit.se.menu.api.dto.manager.UpsertRecipeRequest;
import iuh.fit.se.menu.application.MenuService;
import iuh.fit.se.shared.ai.client.dto.ComboGenerateResponse;
import iuh.fit.se.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/manager/menu/items")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerMenuItemController {

    private final MenuService menuService;

    public ManagerMenuItemController(MenuService menuService) {
        this.menuService = menuService;
    }

    @PutMapping("/{id}/image")
    public ResponseEntity<ApiResponse<MenuItemManagerDetailResponse>> updateImage(
            @PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file
    ) {
        MenuItemManagerDetailResponse updated = menuService.updateMenuItemImage(id, file);
        return ResponseEntity.ok(ApiResponse.ok("Menu item image updated", updated));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MenuItemManagerDetailResponse>> create(@Valid @RequestBody CreateMenuItemRequest request) {
        MenuItemManagerDetailResponse created = menuService.createMenuItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Menu item created", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuItemManagerDetailResponse>> getDetail(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.getMenuItemManagerDetail(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuItemManagerDetailResponse>> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateMenuItemRequest request
    ) {
        MenuItemManagerDetailResponse updated = menuService.updateMenuItem(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Menu item updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        menuService.deleteMenuItem(id);
        return ResponseEntity.ok(ApiResponse.ok("Menu item deleted", null));
    }

    @PutMapping("/{id}/combo/fixed")
    public ResponseEntity<ApiResponse<MenuItemManagerDetailResponse>> upsertFixedCombo(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpsertFixedComboRequest request
    ) {
        MenuItemManagerDetailResponse updated = menuService.upsertFixedComboConfig(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Fixed combo config updated", updated));
    }

    @PutMapping("/{id}/combo/pick")
    public ResponseEntity<ApiResponse<MenuItemManagerDetailResponse>> upsertPickCombo(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpsertPickComboRequest request
    ) {
        MenuItemManagerDetailResponse updated = menuService.upsertPickComboConfig(id, request);
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
    public ResponseEntity<ApiResponse<CookTimeSuggestionResponse>> getSuggestedCookTime(
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.getSuggestedCookTime(id)));
    }

    @PutMapping("/{id}/cooktime")
    public ResponseEntity<ApiResponse<Void>> updateCookTime(
            @PathVariable("id") Long id,
            @RequestBody Map<String, Integer> payload
    ) {
        Integer newCookTime = payload.get("cookTime");
        if (newCookTime == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("cookTime is required", null));
        }
        menuService.updateCookTime(id, newCookTime);
        return ResponseEntity.ok(ApiResponse.ok("Cook time updated", null));
    }
}
