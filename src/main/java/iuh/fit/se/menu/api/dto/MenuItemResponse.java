package iuh.fit.se.menu.api.dto;

import iuh.fit.se.menu.application.MenuItemData;
import iuh.fit.se.menu.domain.ComboKind;
import iuh.fit.se.menu.domain.MenuItem;
import iuh.fit.se.menu.domain.MenuItemType;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemResponse {

    private Long id;
    private Long categoryId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer cookTime;
    private String imageUrl;
    private boolean available;
    private boolean ingredientSufficient;

    private Instant createdAt;
    private Instant updatedAt;
    private MenuItemType itemType;
    private ComboKind comboKind;

    public static MenuItemResponse from(MenuItem menuItem) {
        return from(menuItem, true);
    }

    public static MenuItemResponse from(MenuItem menuItem, boolean ingredientSufficient) {
        return MenuItemResponse.builder()
                .id(menuItem.getId())
                .categoryId(menuItem.getCategoryId())
                .name(menuItem.getName())
                .description(menuItem.getDescription())
            .price(menuItem.getPrice() == null ? BigDecimal.ZERO : menuItem.getPrice().toBigDecimal())
                .cookTime(menuItem.getCookTime())
                .imageUrl(menuItem.getImageUrl())
                .available(menuItem.isAvailable())
                .ingredientSufficient(ingredientSufficient)
                .createdAt(menuItem.getCreatedAt())
                .updatedAt(menuItem.getUpdatedAt())
                .itemType(menuItem.getItemType())
                .comboKind(menuItem.getComboKind())
                .build();
    }

    public static MenuItemResponse from(MenuItemData menuItem) {
        return MenuItemResponse.builder()
                .id(menuItem.id())
                .name(menuItem.name())
            .price(menuItem.price() == null ? BigDecimal.ZERO : menuItem.price())
                .imageUrl(menuItem.imageUrl())
                .available(menuItem.available())
                .ingredientSufficient(menuItem.ingredientSufficient())
                .itemType(menuItem.itemType())
                .comboKind(menuItem.comboKind())
                .build();
    }
}