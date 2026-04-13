package iuh.fit.se.kitchen.domain;

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
@Table(name = "kitchen_batches", schema = "kitchen")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class KitchenBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "menu_item_id", nullable = false)
    private Long menuItemId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", columnDefinition = "kitchen_batch_status_enum", nullable = false)
    @Builder.Default
    private KitchenBatchStatus status = KitchenBatchStatus.SUGGESTED;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "source", columnDefinition = "batch_source_enum", nullable = false)
    @Builder.Default
    private BatchSource source = BatchSource.AI;

    @Column(name = "ai_confidence")
    private BigDecimal aiConfidence;

    @Column(name = "estimated_saving_minutes")
    private Integer estimatedSavingMinutes;

    @Column(name = "batch_note")
    private String batchNote;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = KitchenBatchStatus.SUGGESTED;
        }
        if (this.source == null) {
            this.source = BatchSource.AI;
        }
        validateData();
    }

    public static KitchenBatch suggestByAi(
            Long menuItemId,
            Integer quantity,
            BigDecimal aiConfidence,
            Integer estimatedSavingMinutes,
            String batchNote
    ) {
        return KitchenBatch.builder()
                .menuItemId(menuItemId)
                .quantity(quantity)
                .source(BatchSource.AI)
                .status(KitchenBatchStatus.SUGGESTED)
                .aiConfidence(aiConfidence)
                .estimatedSavingMinutes(estimatedSavingMinutes)
                .batchNote(batchNote)
                .build();
    }

    public static KitchenBatch createManual(Long menuItemId, Integer quantity, String batchNote) {
        return KitchenBatch.builder()
                .menuItemId(menuItemId)
                .quantity(quantity)
                .source(BatchSource.MANUAL)
                .status(KitchenBatchStatus.SUGGESTED)
                .batchNote(batchNote)
                .build();
    }

    public void confirm() {
        KitchenBatchStateMachine.validate(this.status, KitchenBatchStatus.CONFIRMED);
        this.status = KitchenBatchStatus.CONFIRMED;
    }

    public void start() {
        KitchenBatchStateMachine.validate(this.status, KitchenBatchStatus.IN_PROGRESS);
        this.status = KitchenBatchStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
    }

    public void complete() {
        KitchenBatchStateMachine.validate(this.status, KitchenBatchStatus.DONE);
        this.status = KitchenBatchStatus.DONE;
        this.completedAt = Instant.now();
    }

    public void updateNote(String batchNote) {
        this.batchNote = batchNote;
    }

    private void validateData() {
        if (this.quantity == null || this.quantity < 1) {
            throw new DomainException("Batch quantity must be greater than zero");
        }
        if (this.estimatedSavingMinutes != null && this.estimatedSavingMinutes < 0) {
            throw new DomainException("Estimated saving minutes must not be negative");
        }
        if (this.aiConfidence != null
                && (this.aiConfidence.compareTo(BigDecimal.ZERO) < 0
                || this.aiConfidence.compareTo(BigDecimal.ONE) > 0)) {
            throw new DomainException("AI confidence must be between 0 and 1");
        }
    }
}
