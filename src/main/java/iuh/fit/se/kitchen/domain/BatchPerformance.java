package iuh.fit.se.kitchen.domain;

import iuh.fit.se.shared.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "batch_performance", schema = "kitchen")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BatchPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false, unique = true)
    private Long batchId;

    @Column(name = "baseline_minutes", nullable = false)
    private Integer baselineMinutes;

    @Column(name = "actual_minutes")
    private Integer actualMinutes;

    @Column(name = "saving_minutes", insertable = false, updatable = false)
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    private Integer savingMinutes;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @PrePersist
    protected void onCreate() {
        if (this.recordedAt == null) {
            this.recordedAt = Instant.now();
        }
        validateMinutes();
    }

    public static BatchPerformance create(Long batchId, Integer baselineMinutes) {
        return BatchPerformance.builder()
                .batchId(batchId)
                .baselineMinutes(baselineMinutes)
                .build();
    }

    public void recordActualMinutes(Integer actualMinutes) {
        this.actualMinutes = actualMinutes;
        validateMinutes();
    }

    private void validateMinutes() {
        if (this.baselineMinutes == null || this.baselineMinutes < 1) {
            throw new DomainException("Baseline minutes must be greater than zero");
        }
        if (this.actualMinutes != null && this.actualMinutes < 1) {
            throw new DomainException("Actual minutes must be greater than zero");
        }
    }
}
