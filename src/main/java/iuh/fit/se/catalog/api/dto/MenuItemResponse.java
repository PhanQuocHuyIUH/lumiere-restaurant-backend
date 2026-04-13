package iuh.fit.se.catalog.api.dto;

import iuh.fit.se.catalog.application.MenuItemDTO;
import iuh.fit.se.catalog.domain.MenuItem;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MenuItemResponse {

    private Long id;
    private Long categoryId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer cookTime;
    private String imageUrl;
    private boolean available;
    private String kitchenLabel;
    private String kitchenNote;
    private Instant createdAt;
    private Instant updatedAt;

    public static MenuItemResponse from(MenuItem menuItem) {
        return MenuItemResponse.builder()
                .id(menuItem.getId())
                .categoryId(menuItem.getCategoryId())
                .name(menuItem.getName())
                .description(menuItem.getDescription())
                .price(menuItem.getPrice())
                .cookTime(menuItem.getCookTime())
                .imageUrl(menuItem.getImageUrl())
                .available(menuItem.isAvailable())
                .kitchenLabel(menuItem.getKitchenLabel())
                .kitchenNote(menuItem.getKitchenNote())
                .createdAt(menuItem.getCreatedAt())
                .updatedAt(menuItem.getUpdatedAt())
                .build();
    }

    public static MenuItemResponse from(MenuItemDTO menuItem) {
        return MenuItemResponse.builder()
                .id(menuItem.id())
                .name(menuItem.name())
                .price(menuItem.price())
                .available(menuItem.available())
                .build();
    }
}