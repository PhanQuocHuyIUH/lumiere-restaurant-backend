package iuh.fit.se.ordering.domain;

import iuh.fit.se.shared.exception.DomainException;
import jakarta.persistence.Column;
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
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.generator.EventType;

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

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "subtotal", insertable = false, updatable = false)
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    private BigDecimal subtotal;

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
            String note
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
                .unitPrice(unitPrice)
                .note(note)
                .status(OrderItemStatus.PENDING)
                .build();
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
            return this.subtotal;
        }
        return this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
    }
}
