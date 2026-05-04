package iuh.fit.se.menu.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import iuh.fit.se.menu.api.dto.MenuCategorySummaryResponse;
import iuh.fit.se.menu.application.MenuService;
import iuh.fit.se.shared.response.ApiResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class MenuItemControllerTest {

    @Mock
    private MenuService menuService;

    @Test
    void getCategoriesDelegatesToService() {
        MenuItemController controller = new MenuItemController(menuService);
        List<MenuCategorySummaryResponse> categories = List.of(
                new MenuCategorySummaryResponse(1L, "Drinks", "Beverages", 1)
        );
        when(menuService.getStaffMenuCategories()).thenReturn(categories);

        ResponseEntity<ApiResponse<List<MenuCategorySummaryResponse>>> response = controller.getCategories();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).containsExactlyElementsOf(categories);
        verify(menuService).getStaffMenuCategories();
    }
}