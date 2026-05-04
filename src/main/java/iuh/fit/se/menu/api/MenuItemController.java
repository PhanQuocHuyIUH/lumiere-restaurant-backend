package iuh.fit.se.menu.api;

import iuh.fit.se.menu.api.dto.MenuCategoryResponse;
import iuh.fit.se.menu.api.dto.MenuCategorySummaryResponse;
import iuh.fit.se.menu.api.dto.MenuItemResponse;
import iuh.fit.se.menu.application.MenuItemDTO;
import iuh.fit.se.menu.application.MenuService;
import iuh.fit.se.shared.response.ApiResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/menu")
public class MenuItemController {

    private final MenuService menuService;

    public MenuItemController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuCategoryResponse>>> getMenu() {
        return ResponseEntity.ok(ApiResponse.ok(menuService.getMenu()));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('WAITER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<MenuCategorySummaryResponse>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.ok(menuService.getStaffMenuCategories()));
    }

    /**
     * GET /menu/items?categoryId={id}
     * Returns available menu items. If categoryId is provided, filters by category.
     * Accessible by WAITER and MANAGER.
     */
    @GetMapping("/items")
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> getItems(
            @RequestParam(value = "categoryId", required = true) Long categoryId
    ) {
        List<MenuItemResponse> items = menuService.getAvailableItemsByCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> getItemById(@PathVariable("id") Long id) {
        MenuItemDTO item = menuService.getItem(id);
        return ResponseEntity.ok(ApiResponse.ok(MenuItemResponse.from(item)));
    }

    @PutMapping("/items/{id}/image")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateItemImage(
            @PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file
    ) {
        MenuItemDTO item = menuService.updateMenuItemImage(id, file);
        return ResponseEntity.ok(ApiResponse.ok("Menu item image updated", MenuItemResponse.from(item)));
    }
}