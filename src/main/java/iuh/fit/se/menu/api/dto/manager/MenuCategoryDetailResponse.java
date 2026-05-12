package iuh.fit.se.menu.api.dto.manager;

import iuh.fit.se.menu.domain.MenuCategory;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MenuCategoryDetailResponse {

    private Long id;
    private String name;
    private String description;
    private Integer displayOrder;
    private Instant createdAt;
    private Instant updatedAt;

    public static MenuCategoryDetailResponse from(MenuCategory category) {
        return MenuCategoryDetailResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .displayOrder(category.getDisplayOrder())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}

