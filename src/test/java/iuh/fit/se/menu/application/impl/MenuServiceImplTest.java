package iuh.fit.se.menu.application.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import iuh.fit.se.inventory.infrastructure.IngredientRepository;
import iuh.fit.se.menu.api.dto.MenuCategorySummaryResponse;
import iuh.fit.se.menu.domain.MenuCategory;
import iuh.fit.se.menu.infrastructure.ComboFixedComponentRepository;
import iuh.fit.se.menu.infrastructure.ComboPickSlotItemRepository;
import iuh.fit.se.menu.infrastructure.ComboPickSlotRepository;
import iuh.fit.se.menu.infrastructure.MenuCategoryRepository;
import iuh.fit.se.menu.infrastructure.MenuItemIngredientRepository;
import iuh.fit.se.menu.infrastructure.MenuItemRepository;
import iuh.fit.se.shared.storage.ImageStorageService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MenuServiceImplTest {

    @Mock
    private MenuCategoryRepository menuCategoryRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private ComboFixedComponentRepository comboFixedComponentRepository;

    @Mock
    private ComboPickSlotRepository comboPickSlotRepository;

    @Mock
    private ComboPickSlotItemRepository comboPickSlotItemRepository;

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private MenuItemIngredientRepository menuItemIngredientRepository;

    @Mock
    private IngredientRepository ingredientRepository;

    @InjectMocks
    private MenuServiceImpl menuService;

    @Test
    void getStaffMenuCategoriesMapsCategoriesInDisplayOrder() {
        MenuCategory drinks = MenuCategory.builder()
                .id(2L)
                .name("Drinks")
                .description("Beverages")
                .displayOrder(2)
                .build();
        MenuCategory mains = MenuCategory.builder()
                .id(1L)
                .name("Mains")
                .description("Main dishes")
                .displayOrder(1)
                .build();

        when(menuCategoryRepository.findAllByDeletedAtIsNullOrderByDisplayOrderAscIdAsc())
                .thenReturn(List.of(mains, drinks));

        List<MenuCategorySummaryResponse> result = menuService.getStaffMenuCategories();

        assertThat(result).containsExactly(
                new MenuCategorySummaryResponse(1L, "Mains", "Main dishes", 1),
                new MenuCategorySummaryResponse(2L, "Drinks", "Beverages", 2)
        );
    }
}