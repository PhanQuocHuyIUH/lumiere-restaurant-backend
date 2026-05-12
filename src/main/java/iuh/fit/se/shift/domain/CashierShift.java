package iuh.fit.se.shift.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import iuh.fit.se.shared.domain.Money;
import java.time.Instant;

@Entity
@Table(name = "cashier_shifts", schema = "shift")
public class CashierShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cashier_id", nullable = false)
    private Long cashierId;

    @Column(name = "opened_by")
    private Long openedBy;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt = Instant.now();

    @Column(name = "closed_at")
    private Instant closedAt;

    @Embedded
    @AttributeOverride(name = "vndAmount", column = @Column(name = "opening_total_vnd", nullable = false))
    private Money openingTotal;

    @Embedded
    @AttributeOverride(name = "vndAmount", column = @Column(name = "closing_total_vnd"))
    private Money closingTotal;

    private String notes;

    @Column(name = "closing_notes")
    private String closingNotes;

    @Column(name = "closed_by")
    private Long closedBy;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public CashierShift() {}

    public CashierShift(Long cashierId, BigDecimal openingTotal, String notes, Long openedBy) {
        this.cashierId = cashierId;
        this.openingTotal = Money.of(openingTotal);
        this.notes = notes;
        this.openedBy = openedBy;
    }

    public Long getId() { return id; }
    public Long getCashierId() { return cashierId; }
    public Long getOpenedBy() { return openedBy; }
    public Instant getOpenedAt() { return openedAt; }
    public Instant getClosedAt() { return closedAt; }
    public BigDecimal getOpeningTotal() { return BigDecimal.valueOf(openingTotal == null ? 0L : openingTotal.toLong()); }
    public BigDecimal getClosingTotal() { return BigDecimal.valueOf(closingTotal == null ? 0L : closingTotal.toLong()); }
    public String getNotes() { return notes; }
    public String getClosingNotes() { return closingNotes; }
    public Long getClosedBy() { return closedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }

    public void close(BigDecimal closingTotal, Instant closedAt, Long closedBy, String closingNotes) {
        this.closingTotal = Money.of(closingTotal);
        this.closedAt = closedAt;
        this.closedBy = closedBy;
        this.closingNotes = closingNotes;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
