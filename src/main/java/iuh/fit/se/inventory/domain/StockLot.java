package iuh.fit.se.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stock_lots", schema = "inventory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class StockLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ingredient_id", nullable = false)
    private Long ingredientId;

    @Column(name = "initial_qty", nullable = false)
    private BigDecimal initialQty;

    @Column(name = "remaining_qty", nullable = false)
    private BigDecimal remainingQty;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "imported_at", nullable = false, updatable = false)
    private Instant importedAt;

    @Column(name = "note")
    private String note;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    protected void onCreate() {
        if (this.importedAt == null) {
            this.importedAt = Instant.now();
        }
        if (this.remainingQty == null) {
            this.remainingQty = this.initialQty;
        }
    }

    public static StockLot importLot(Long ingredientId, BigDecimal qty, LocalDate expiryDate, String note) {
        return StockLot.builder()
                .ingredientId(ingredientId)
                .initialQty(qty)
                .remainingQty(qty)
                .expiryDate(expiryDate)
                .note(note)
                .build();
    }

    /** Consume up to {@code requested}; returns the amount actually deducted from this lot. */
    public BigDecimal consume(BigDecimal requested) {
        BigDecimal take = requested.min(this.remainingQty);
        this.remainingQty = this.remainingQty.subtract(take);
        return take;
    }

    public void wasteAll() {
        this.remainingQty = BigDecimal.ZERO;
    }

    public boolean isDepleted() {
        return this.remainingQty.signum() <= 0;
    }
}
