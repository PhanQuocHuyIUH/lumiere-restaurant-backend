package iuh.fit.se.inventory.domain;

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
@Table(name = "stock_transactions", schema = "inventory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class StockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ingredient_id", nullable = false)
    private Long ingredientId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "txn_type", columnDefinition = "stock_txn_type_enum", nullable = false)
    private StockTxnType txnType;

    @Column(name = "quantity_before", nullable = false)
    private BigDecimal quantityBefore;

    @Column(name = "quantity_change", nullable = false)
    private BigDecimal quantityChange;

    @Column(name = "quantity_after", nullable = false)
    private BigDecimal quantityAfter;

    @Column(name = "note")
    private String note;

    @Column(name = "performed_by")
    private Long performedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public static StockTransaction record(
            Long ingredientId,
            StockTxnType txnType,
            BigDecimal quantityBefore,
            BigDecimal quantityChange,
            BigDecimal quantityAfter,
            String note,
            Long performedBy
    ) {
        return StockTransaction.builder()
                .ingredientId(ingredientId)
                .txnType(txnType)
                .quantityBefore(quantityBefore)
                .quantityChange(quantityChange)
                .quantityAfter(quantityAfter)
                .note(note)
                .performedBy(performedBy)
                .build();
    }
}
