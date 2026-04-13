package iuh.fit.se.catalog.api.dto;

import iuh.fit.se.catalog.domain.MenuCategory;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MenuCategoryResponse {

    private Long id;
    private String name;
    private String description;
    private Integer displayOrder;
    private List<MenuItemResponse> items;

    public static MenuCategoryResponse from(MenuCategory menuCategory, List<MenuItemResponse> items) {
        return MenuCategoryResponse.builder()
                .id(menuCategory.getId())
                .name(menuCategory.getName())
                .description(menuCategory.getDescription())
                .displayOrder(menuCategory.getDisplayOrder())
                .items(items)
                .build();
    }
}