package iuh.fit.se.menu.api;

import iuh.fit.se.menu.api.dto.admin.AdminMenuCategoryListItemResponse;
import iuh.fit.se.menu.api.dto.admin.CreateMenuCategoryRequest;
import iuh.fit.se.menu.api.dto.admin.MenuCategoryDetailResponse;
import iuh.fit.se.menu.api.dto.admin.UpdateMenuCategoryRequest;
import iuh.fit.se.menu.application.MenuService;
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
@RequestMapping("/admin/menu/categories")
@PreAuthorize("hasRole('MANAGER')")
public class AdminMenuCategoryController {

    private final MenuService menuService;

    public AdminMenuCategoryController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminMenuCategoryListItemResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(menuService.getAllCategoriesForAdmin()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MenuCategoryDetailResponse>> create(
            @Valid @RequestBody CreateMenuCategoryRequest request
    ) {
        MenuCategoryDetailResponse created = menuService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Menu category created", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuCategoryDetailResponse>> getDetail(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.getCategoryDetail(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuCategoryDetailResponse>> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateMenuCategoryRequest request
    ) {
        MenuCategoryDetailResponse updated = menuService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Menu category updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        menuService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.ok("Menu category deleted", null));
    }
}
