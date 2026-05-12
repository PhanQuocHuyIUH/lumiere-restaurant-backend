package iuh.fit.se.menu.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import iuh.fit.se.shared.domain.Money;
import iuh.fit.se.shared.domain.TaxMode;
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
@Table(name = "menu_items", schema = "menu")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "vndAmount", column = @Column(name = "price_vnd", nullable = false))
    })
    private Money price;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "item_type", columnDefinition = "menu_item_type_enum", nullable = false)
    @Builder.Default
    private MenuItemType itemType = MenuItemType.SINGLE;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "combo_kind", columnDefinition = "combo_kind_enum")
    private ComboKind comboKind;

    @Column(name = "cook_time")
    private Integer cookTime;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "image_public_id")
    private String imagePublicId;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private boolean available = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_tax_mode", length = 20)
    private TaxMode itemTaxMode;

    @Column(name = "item_tax_rate_bps")
    private Integer itemTaxRateBps;

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

    public void updateBasics(
            Long categoryId,
            String name,
            String description,
            BigDecimal price,
            Integer cookTime,
            String imageUrl
    ) {
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.price = Money.of(price);
        this.cookTime = cookTime;
        this.imageUrl = imageUrl;
    }

    public void updateType(MenuItemType itemType, ComboKind comboKind) {
        this.itemType = itemType == null ? MenuItemType.SINGLE : itemType;
        this.comboKind = this.itemType == MenuItemType.COMBO ? comboKind : null;
    }



    public void updateImage(String imageUrl, String imagePublicId) {
        this.imageUrl = imageUrl;
        this.imagePublicId = imagePublicId;
    }

    public void clearImage() {
        this.imageUrl = null;
        this.imagePublicId = null;
    }

    public void markAvailable() {
        this.available = true;
    }

    public void markUnavailable() {
        this.available = false;
    }

    public void softDelete() {
        if (this.deletedAt != null) {
            return;
        }
        this.available = false;
        this.deletedAt = Instant.now();
    }

    public void updateCookTime(Integer newCookTime) {
        this.cookTime = newCookTime;
    }

    public void updateTaxOverride(TaxMode taxMode, Integer taxRateBps) {
        this.itemTaxMode = taxMode;
        this.itemTaxRateBps = (taxRateBps != null) ? Math.max(0, taxRateBps) : null;
    }
}