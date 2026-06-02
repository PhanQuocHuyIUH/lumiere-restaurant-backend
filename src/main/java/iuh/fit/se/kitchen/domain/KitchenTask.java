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

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "table_id")
    private Long tableId;

    @Column(name = "menu_item_id")
    private Long menuItemId;

    @Column(name = "menu_item_name")
    private String menuItemName;

    @Column(name = "menu_item_image_url")
    private String menuItemImageUrl;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "order_item_note")
    private String orderItemNote;

    @Column(name = "order_note")
    private String orderNote;

    @Column(name = "expected_cook_time")
    private Integer expectedCookTime;

    @Column(name = "ordered_at", nullable = false)
    private Instant orderedAt;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", columnDefinition = "kitchen_task_status_enum", nullable = false)
    @Builder.Default
    private KitchenTaskStatus status = KitchenTaskStatus.CREATED;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "actual_cook_seconds", insertable = false, updatable = false)
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    private Integer actualCookSeconds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = KitchenTaskStatus.CREATED;
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.orderedAt == null) {
            this.orderedAt = this.createdAt;
        }
    }

    public static KitchenTask create(Long orderItemId) {
        return KitchenTask.builder()
                .orderItemId(orderItemId)
                .status(KitchenTaskStatus.CREATED)
                .build();
    }

    public static KitchenTask create(
            Long orderId,
            Long tableId,
            Long orderItemId,
            Long menuItemId,
            String menuItemName,
            String menuItemImageUrl,
            Integer quantity,
            String orderItemNote,
            String orderNote,
            Integer expectedCookTime,
            Instant orderedAt
    ) {
        return KitchenTask.builder()
                .orderId(orderId)
                .tableId(tableId)
                .orderItemId(orderItemId)
                .menuItemId(menuItemId)
                .menuItemName(menuItemName)
                .menuItemImageUrl(menuItemImageUrl)
                .quantity(quantity)
                .orderItemNote(orderItemNote)
                .orderNote(orderNote)
                .expectedCookTime(expectedCookTime)
                .orderedAt(orderedAt)
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

    public void reassignTable(Long newTableId) {
        this.tableId = newTableId;
    }
}
