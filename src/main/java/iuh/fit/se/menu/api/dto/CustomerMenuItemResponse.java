package iuh.fit.se.menu.api.dto;

import iuh.fit.se.menu.domain.ComboKind;
import iuh.fit.se.menu.domain.MenuItem;
import iuh.fit.se.menu.domain.MenuItemType;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerMenuItemResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer cookTime;
    private String imageUrl;
    private boolean available;
    private boolean ingredientSufficient;
    private MenuItemType itemType;
    private ComboKind comboKind;

    public static CustomerMenuItemResponse from(MenuItem menuItem) {
        return from(menuItem, true);
    }

    public static CustomerMenuItemResponse from(MenuItem menuItem, boolean ingredientSufficient) {
        return CustomerMenuItemResponse.builder()
                .id(menuItem.getId())
                .name(menuItem.getName())
                .description(menuItem.getDescription())
            .price(menuItem.getPrice() == null ? BigDecimal.ZERO : menuItem.getPrice().toBigDecimal())
                .cookTime(menuItem.getCookTime())
                .imageUrl(menuItem.getImageUrl())
                .available(menuItem.isAvailable())
                .ingredientSufficient(ingredientSufficient)
                .itemType(menuItem.getItemType())
                .comboKind(menuItem.getComboKind())
                .build();
    }
}
