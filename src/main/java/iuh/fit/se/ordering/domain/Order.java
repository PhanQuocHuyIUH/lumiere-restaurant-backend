package iuh.fit.se.ordering.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import iuh.fit.se.shared.domain.Money;
import iuh.fit.se.shared.domain.TaxMode;
import iuh.fit.se.shared.util.MoneyHelper;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "orders", schema = "ordering")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", columnDefinition = "order_status_enum", nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.CREATED;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "vndAmount", column = @Column(name = "total_amount_vnd", nullable = false))
    })
    @Builder.Default
    private Money totalAmount = Money.ofVnd(0L);

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "vndAmount", column = @Column(name = "subtotal_amount_vnd", nullable = false))
    })
    @Builder.Default
    private Money subtotalAmount = Money.ofVnd(0L);

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "vndAmount", column = @Column(name = "tax_amount_vnd", nullable = false))
    })
    @Builder.Default
    private Money taxAmount = Money.ofVnd(0L);

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_mode", nullable = false)
    @Builder.Default
    private TaxMode taxMode = TaxMode.NO_TAX;

    @Column(name = "tax_rate_bps", nullable = false)
    @Builder.Default
    private Integer taxRateBps = 0;

    @Column(name = "tax_snapshot_at")
    private Instant taxSnapshotAt;

    @Column(name = "tax_snapshot_by")
    private Long taxSnapshotById;

    @Column(name = "tax_snapshot_version", nullable = false)
    @Builder.Default
    private Integer taxSnapshotVersion = 1;

    @Column(name = "confirmed_by")
    private Long confirmedById;

    @Column(name = "served_by")
    private Long servedById;

    @Column(name = "note")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "ready_at")
    private Instant readyAt;

    @Column(name = "served_at")
    private Instant servedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = OrderStatus.CREATED;
        }
        if (this.totalAmount == null) {
            this.totalAmount = Money.ofVnd(0L);
        }
        if (this.subtotalAmount == null) {
            this.subtotalAmount = this.totalAmount;
        }
        if (this.taxAmount == null) {
            this.taxAmount = Money.ofVnd(0L);
        }
        if (this.taxMode == null) {
            this.taxMode = TaxMode.NO_TAX;
        }
        if (this.taxRateBps == null) {
            this.taxRateBps = 0;
        }
        if (this.taxSnapshotVersion == null || this.taxSnapshotVersion < 1) {
            this.taxSnapshotVersion = 1;
        }
    }

    public void updateTotalAmount(Money totalAmount) {
        if (totalAmount == null) {
            totalAmount = Money.ofVnd(0L);
        }
        MoneyHelper.validateMoneyAmount(totalAmount.toLong());
        this.totalAmount = totalAmount;
    }

    public void updateTotalAmount(BigDecimal totalAmount) {
        Money m = Money.of(totalAmount);
        updateTotalAmount(m);
    }

    public void applyPricingSnapshot(
            Money subtotalAmount,
            Money taxAmount,
            Money totalAmount,
            TaxMode taxMode,
            Integer taxRateBps,
            Long snapshotById
    ) {
        Money safeSubtotal = subtotalAmount == null ? Money.ofVnd(0L) : subtotalAmount;
        Money safeTax = taxAmount == null ? Money.ofVnd(0L) : taxAmount;
        Money safeTotal = totalAmount == null ? Money.ofVnd(0L) : totalAmount;

        MoneyHelper.validateMoneyAmount(safeSubtotal.toLong());
        MoneyHelper.validateMoneyAmount(safeTax.toLong());
        MoneyHelper.validateMoneyAmount(safeTotal.toLong());

        this.subtotalAmount = safeSubtotal;
        this.taxAmount = safeTax;
        this.totalAmount = safeTotal;
        this.taxMode = taxMode == null ? TaxMode.NO_TAX : taxMode;
        this.taxRateBps = taxRateBps == null ? 0 : Math.max(0, taxRateBps);

        if (snapshotById != null && this.taxSnapshotAt == null) {
            this.taxSnapshotAt = Instant.now();
            this.taxSnapshotById = snapshotById;
            if (this.taxSnapshotVersion == null || this.taxSnapshotVersion < 1) {
                this.taxSnapshotVersion = 1;
            }
        }
    }

    public boolean hasTaxSnapshot() {
        return this.taxSnapshotAt != null;
    }

    public void updateNote(String note) {
        this.note = note;
    }

    public void confirm(Long staffId) {
        OrderStateMachine.validate(this.status, OrderStatus.CONFIRMED);
        this.status = OrderStatus.CONFIRMED;
        this.confirmedById = staffId;
        this.confirmedAt = Instant.now();
    }

    public void startPreparing() {
        OrderStateMachine.validate(this.status, OrderStatus.PREPARING);
        this.status = OrderStatus.PREPARING;
    }

    public void markReady() {
        OrderStateMachine.validate(this.status, OrderStatus.READY);
        this.status = OrderStatus.READY;
        this.readyAt = Instant.now();
    }

    public void markServed(Long staffId) {
        OrderStateMachine.validate(this.status, OrderStatus.SERVED);
        this.status = OrderStatus.SERVED;
        this.servedById = staffId;
        this.servedAt = Instant.now();
    }

    public void reopenForAdditionalItems() {
        OrderStateMachine.validate(this.status, OrderStatus.CONFIRMED);
        this.status = OrderStatus.CONFIRMED;
    }

    public void pay() {
        OrderStateMachine.validate(this.status, OrderStatus.PAID);
        this.status = OrderStatus.PAID;
        this.paidAt = Instant.now();
    }

    public void cancel() {
        OrderStateMachine.validate(this.status, OrderStatus.CANCELLED);
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = Instant.now();
    }
}
