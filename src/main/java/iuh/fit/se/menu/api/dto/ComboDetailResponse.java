package iuh.fit.se.menu.api.dto;

import iuh.fit.se.menu.domain.ComboKind;
import iuh.fit.se.menu.domain.MenuItem;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ComboDetailResponse {

    private Long id;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private ComboKind comboKind;

    private List<FixedComponent> fixedComponents;
    private List<PickSlot> pickSlots;

    @Getter
    @Builder
    public static class FixedComponent {
        private Integer quantity;
        private ComboItemSummary item;
    }

    @Getter
    @Builder
    public static class PickSlot {
        private Long id;
        private String name;
        private Integer minSelect;
        private Integer maxSelect;
        private Integer displayOrder;
        private List<ComboItemSummary> allowedItems;
    }

    @Getter
    @Builder
    public static class ComboItemSummary {
        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private String imageUrl;
        private boolean available;

        public static ComboItemSummary from(MenuItem item) {
            if (item == null) return null;
            return ComboItemSummary.builder()
                    .id(item.getId())
                    .name(item.getName())
                    .description(item.getDescription())
                    .price(item.getPrice() == null ? BigDecimal.ZERO : item.getPrice().toBigDecimal())
                    .imageUrl(item.getImageUrl())
                    .available(item.isAvailable())
                    .build();
        }
    }
}
