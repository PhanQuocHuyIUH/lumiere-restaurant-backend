package iuh.fit.se.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

@Entity
@Table(name = "ingredients", schema = "inventory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "unit", columnDefinition = "ingredient_unit_enum", nullable = false)
    private IngredientUnit unit;

    @Column(name = "current_qty", nullable = false)
    @Builder.Default
    private BigDecimal currentQty = BigDecimal.ZERO;

    @Column(name = "low_stock_threshold", nullable = false)
    @Builder.Default
    private BigDecimal lowStockThreshold = BigDecimal.ZERO;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void updateDetails(String name, IngredientUnit unit, BigDecimal lowStockThreshold, String imageUrl) {
        this.name = name;
        this.unit = unit;
        this.lowStockThreshold = lowStockThreshold;
        this.imageUrl = imageUrl;
    }

    public void importStock(BigDecimal addedQty) {
        this.currentQty = this.currentQty.add(addedQty);
    }

    public void adjust(BigDecimal newQty) {
        this.currentQty = newQty;
    }

    public boolean isLowStock() {
        return this.currentQty.compareTo(this.lowStockThreshold) <= 0;
    }

    public void softDelete() {
        if (this.deletedAt != null) {
            return;
        }
        this.deletedAt = Instant.now();
    }
}
