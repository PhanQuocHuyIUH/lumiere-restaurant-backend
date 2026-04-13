package iuh.fit.se.catalog.api;

import iuh.fit.se.catalog.api.dto.MenuCategoryResponse;
import iuh.fit.se.catalog.application.CatalogService;
import iuh.fit.se.shared.response.ApiResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/menu/categories")
public class MenuCategoryController {

    private final CatalogService catalogService;

    public MenuCategoryController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuCategoryResponse>>> getMenu() {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.getMenu()));
    }
}