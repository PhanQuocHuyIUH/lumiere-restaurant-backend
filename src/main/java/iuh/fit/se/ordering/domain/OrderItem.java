package iuh.fit.se.ordering.domain;

import iuh.fit.se.shared.exception.DomainException;
import com.fasterxml.jackson.databind.JsonNode;
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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "order_items", schema = "ordering")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "revision_id", nullable = false)
    private Long revisionId;

    @Column(name = "menu_item_id")
    private Long menuItemId;

    @Column(name = "parent_order_item_id")
    private Long parentOrderItemId;

    @Column(name = "is_billable", nullable = false)
    @Builder.Default
    private boolean billable = true;

    @Column(name = "is_combo_parent", nullable = false)
    @Builder.Default
    private boolean comboParent = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "combo_snapshot", columnDefinition = "jsonb")
    private JsonNode comboSnapshot;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "vndAmount", column = @Column(name = "unit_price_vnd", nullable = false))
    })
    private Money unitPrice;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "vndAmount", column = @Column(name = "subtotal_vnd", insertable = false, updatable = false))
    })
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    private Money subtotal;

    @Column(name = "unit_tax_mode", length = 20)
    @Enumerated(EnumType.STRING)
    private TaxMode unitTaxMode;

    @Column(name = "unit_tax_rate_bps")
    private Integer unitTaxRateBps;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "vndAmount", column = @Column(name = "net_subtotal_vnd"))
    })
    private Money netSubtotal;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "vndAmount", column = @Column(name = "tax_subtotal_vnd"))
    })
    private Money taxSubtotal;

    @Column(name = "note")
    private String note;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", columnDefinition = "order_item_status_enum", nullable = false)
    @Builder.Default
    private OrderItemStatus status = OrderItemStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = OrderItemStatus.PENDING;
        }
    }

        public static OrderItem create(
            Long revisionId,
            Long menuItemId,
            Integer quantity,
            BigDecimal unitPrice,
            String note,
            TaxMode itemTaxMode,
            Integer itemTaxRateBps
    ) {
        if (quantity == null || quantity < 1) {
            throw new DomainException("Quantity must be greater than zero");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainException("Unit price must not be negative");
        }

        return OrderItem.builder()
            .revisionId(revisionId)
            .menuItemId(menuItemId)
            .quantity(quantity)
            .unitPrice(Money.of(unitPrice))
            .unitTaxMode(itemTaxMode)
            .unitTaxRateBps(itemTaxRateBps)
            .note(note)
            .status(OrderItemStatus.PENDING)
            .build();
    }

    public void applyTaxBreakdown(Money netSubtotal, Money taxSubtotal, TaxMode taxMode, int rateBps) {
        this.netSubtotal = netSubtotal;
        this.taxSubtotal = taxSubtotal;
        this.unitTaxMode = taxMode;
        this.unitTaxRateBps = rateBps;
    }

    public static OrderItem createComboParent(
            Long revisionId,
            Long comboMenuItemId,
            Integer quantity,
            BigDecimal comboUnitPrice,
            JsonNode comboSnapshot,
            String note,
            TaxMode itemTaxMode,
            Integer itemTaxRateBps
    ) {
        OrderItem parent = create(revisionId, comboMenuItemId, quantity, comboUnitPrice, note, itemTaxMode, itemTaxRateBps);
        parent.billable = true;
        parent.comboParent = true;
        parent.comboSnapshot = comboSnapshot;
        return parent;
    }

    public static OrderItem createComboChild(
            Long revisionId,
            Long parentOrderItemId,
            Long componentMenuItemId,
            Integer quantity,
            String note
    ) {
        OrderItem child = create(revisionId, componentMenuItemId, quantity, BigDecimal.ZERO, note, null, null);
        child.billable = false;
        child.comboParent = false;
        child.parentOrderItemId = parentOrderItemId;
        return child;
    }

    public void startPreparing() {
        OrderItemStateMachine.validate(this.status, OrderItemStatus.PREPARING);
        this.status = OrderItemStatus.PREPARING;
    }

    public void markDone() {
        OrderItemStateMachine.validate(this.status, OrderItemStatus.DONE);
        this.status = OrderItemStatus.DONE;
    }

    public void markServed() {
        OrderItemStateMachine.validate(this.status, OrderItemStatus.SERVED);
        this.status = OrderItemStatus.SERVED;
    }

    public void cancel() {
        OrderItemStateMachine.validate(this.status, OrderItemStatus.CANCELLED);
        this.status = OrderItemStatus.CANCELLED;
    }

    public BigDecimal calculateSubtotal() {
        if (this.subtotal != null) {
            return BigDecimal.valueOf(this.subtotal.toLong());
        }
        return BigDecimal.valueOf(this.unitPrice.toLong()).multiply(BigDecimal.valueOf(this.quantity));
    }
}
