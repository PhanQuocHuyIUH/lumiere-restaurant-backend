package iuh.fit.se.menu.api.dto.manager;

import iuh.fit.se.menu.domain.ComboKind;
import iuh.fit.se.menu.domain.MenuItem;
import iuh.fit.se.menu.domain.MenuItemType;
import iuh.fit.se.shared.domain.TaxMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MenuItemManagerDetailResponse {

    private Long id;
    private Long categoryId;
    private String name;
    private String description;
    private BigDecimal price;
    private TaxMode itemTaxMode;
    private Integer itemTaxRateBps;
    private Integer cookTime;
    private String imageUrl;
    private boolean available;

    private Instant createdAt;
    private Instant updatedAt;
    private MenuItemType itemType;
    private ComboKind comboKind;

    private FixedCombo fixedCombo;
    private PickCombo pickCombo;

    @Getter
    @Builder
    public static class FixedCombo {
        private List<FixedComponent> components;
    }

    @Getter
    @Builder
    public static class FixedComponent {
        private Long menuItemId;
        private Integer quantity;
    }

    @Getter
    @Builder
    public static class PickCombo {
        private List<PickSlot> slots;
    }

    @Getter
    @Builder
    public static class PickSlot {
        private Long id;
        private String name;
        private Integer minSelect;
        private Integer maxSelect;
        private Integer displayOrder;
        private List<Long> allowedItemIds;
    }

    public static MenuItemManagerDetailResponse fromBase(MenuItem item) {
        return MenuItemManagerDetailResponse.builder()
                .id(item.getId())
                .categoryId(item.getCategoryId())
                .name(item.getName())
                .description(item.getDescription())
            .price(item.getPrice() == null ? BigDecimal.ZERO : item.getPrice().toBigDecimal())
                .itemTaxMode(item.getItemTaxMode())
                .itemTaxRateBps(item.getItemTaxRateBps())
                .cookTime(item.getCookTime())
                .imageUrl(item.getImageUrl())
                .available(item.isAvailable())

                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .itemType(item.getItemType())
                .comboKind(item.getComboKind())
                .build();
    }
}



