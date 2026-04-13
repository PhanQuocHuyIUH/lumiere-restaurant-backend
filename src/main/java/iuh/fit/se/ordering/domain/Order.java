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
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

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
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", columnDefinition = "order_status_enum", nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.CREATED;

    @Column(name = "total_amount", nullable = false)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "confirmed_by")
    private Long confirmedById;

    @Column(name = "served_by")
    private Long servedById;

    @Column(name = "note")
    private String note;

    @Column(name = "split_bill_allowed", nullable = false)
    @Builder.Default
    private boolean splitBillAllowed = false;

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
            this.totalAmount = BigDecimal.ZERO;
        }
    }

    public void updateTotalAmount(BigDecimal totalAmount) {
        BigDecimal normalized = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainException("Order total amount cannot be negative");
        }
        this.totalAmount = normalized;
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
