package iuh.fit.se.kitchen.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "kitchen_tasks", schema = "kitchen")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class KitchenTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_item_id", nullable = false, unique = true)
    private Long orderItemId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", columnDefinition = "kitchen_task_status_enum", nullable = false)
    @Builder.Default
    private KitchenTaskStatus status = KitchenTaskStatus.CREATED;

    @Column(name = "staff_note")
    private String staffNote;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "actual_cook_seconds", insertable = false, updatable = false)
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    private Integer actualCookSeconds;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = KitchenTaskStatus.CREATED;
        }
    }

    public static KitchenTask create(Long orderItemId) {
        return KitchenTask.builder()
                .orderItemId(orderItemId)
                .status(KitchenTaskStatus.CREATED)
                .build();
    }

    public void startCooking() {
        KitchenTaskStateMachine.validate(this.status, KitchenTaskStatus.COOKING);
        this.status = KitchenTaskStatus.COOKING;
        this.startedAt = Instant.now();
    }

    public void complete() {
        KitchenTaskStateMachine.validate(this.status, KitchenTaskStatus.DONE);
        this.status = KitchenTaskStatus.DONE;
        this.completedAt = Instant.now();
    }

    public void cancel() {
        KitchenTaskStateMachine.validate(this.status, KitchenTaskStatus.CANCELLED);
        this.status = KitchenTaskStatus.CANCELLED;
        this.completedAt = Instant.now();
    }

    public void updateStaffNote(String staffNote) {
        this.staffNote = staffNote;
    }
}
