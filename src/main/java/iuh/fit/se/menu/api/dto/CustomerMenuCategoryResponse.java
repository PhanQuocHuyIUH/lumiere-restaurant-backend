package iuh.fit.se.menu.api.dto;

import iuh.fit.se.menu.domain.MenuCategory;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerMenuCategoryResponse {

    private Long id;
    private String name;
    private String description;
    private Integer displayOrder;
    private List<CustomerMenuItemResponse> items;

    public static CustomerMenuCategoryResponse from(MenuCategory category, List<CustomerMenuItemResponse> items) {
        return CustomerMenuCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .displayOrder(category.getDisplayOrder())
                .items(items)
                .build();
    }
}
