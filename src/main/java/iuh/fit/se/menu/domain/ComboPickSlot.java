package iuh.fit.se.menu.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "combo_pick_slots", schema = "menu")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ComboPickSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "combo_item_id", nullable = false)
    private Long comboItemId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "min_select", nullable = false)
    private Integer minSelect;

    @Column(name = "max_select", nullable = false)
    private Integer maxSelect;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public static ComboPickSlot create(
            Long comboItemId,
            String name,
            Integer minSelect,
            Integer maxSelect,
            Integer displayOrder
    ) {
        return ComboPickSlot.builder()
                .comboItemId(comboItemId)
                .name(name)
                .minSelect(minSelect)
                .maxSelect(maxSelect)
                .displayOrder(displayOrder)
                .build();
    }
}

