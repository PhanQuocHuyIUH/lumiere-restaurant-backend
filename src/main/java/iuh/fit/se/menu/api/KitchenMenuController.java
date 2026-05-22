package iuh.fit.se.menu.api;

import iuh.fit.se.menu.api.dto.manager.MenuItemManagerDetailResponse;
import iuh.fit.se.menu.application.MenuService;
import iuh.fit.se.shared.response.ApiResponse;
import iuh.fit.se.shared.security.JwtPrincipal;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kitchen-only access to the menu domain: limited to toggling an item OFF.
 * Re-enabling stays manager-only (in {@link ManagerMenuItemController#markAvailable}).
 */
@RestController
@RequestMapping("/kitchen/menu-items")
@PreAuthorize("hasAnyRole('KITCHEN', 'MANAGER')")
public class KitchenMenuController {

    private final MenuService menuService;

    public KitchenMenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @PostMapping("/{id}/mark-unavailable")
    public ResponseEntity<ApiResponse<MenuItemManagerDetailResponse>> markUnavailable(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        String reason = body == null ? null : body.get("reason");
        Long staffId = principal == null ? null : principal.getStaffId();
        MenuItemManagerDetailResponse result = menuService.markMenuItemUnavailable(id, reason, staffId);
        return ResponseEntity.ok(ApiResponse.ok("Menu item marked unavailable", result));
    }
}
