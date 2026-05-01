package iuh.fit.se.shift.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cashier_shifts", schema = "shift")
public class CashierShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cashier_id", nullable = false)
    private Long cashierId;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt = Instant.now();

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "opening_total", nullable = false)
    private BigDecimal openingTotal;

    @Column(name = "closing_total")
    private BigDecimal closingTotal;

    private String notes;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public CashierShift() {}

    public CashierShift(Long cashierId, BigDecimal openingTotal, String notes) {
        this.cashierId = cashierId;
        this.openingTotal = openingTotal;
        this.notes = notes;
    }

    public Long getId() { return id; }
    public Long getCashierId() { return cashierId; }
    public Instant getOpenedAt() { return openedAt; }
    public Instant getClosedAt() { return closedAt; }
    public BigDecimal getOpeningTotal() { return openingTotal; }
    public BigDecimal getClosingTotal() { return closingTotal; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }

    public void close(BigDecimal closingTotal) {
        this.closingTotal = closingTotal;
        this.closedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
