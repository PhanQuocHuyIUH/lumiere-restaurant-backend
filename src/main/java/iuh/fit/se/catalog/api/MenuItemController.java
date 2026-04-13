package iuh.fit.se.catalog.api;

import iuh.fit.se.catalog.api.dto.MenuItemResponse;
import iuh.fit.se.catalog.application.CatalogService;
import iuh.fit.se.catalog.application.MenuItemDTO;
import iuh.fit.se.shared.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/menu/items")
public class MenuItemController {

    private final CatalogService catalogService;

    public MenuItemController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> getItemById(@PathVariable("id") Long id) {
        MenuItemDTO item = catalogService.getItem(id);
        return ResponseEntity.ok(ApiResponse.ok(MenuItemResponse.from(item)));
    }

    @GetMapping("/{id}/available")
    public ResponseEntity<ApiResponse<Boolean>> isItemAvailable(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.isItemAvailable(id)));
    }
}