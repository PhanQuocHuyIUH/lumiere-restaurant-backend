package iuh.fit.se.kitchen.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "kitchen_batch_items", schema = "kitchen")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class KitchenBatchItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "kitchen_task_id", nullable = false, unique = true)
    private Long kitchenTaskId;

    public static KitchenBatchItem assign(Long batchId, Long kitchenTaskId) {
        return KitchenBatchItem.builder()
                .batchId(batchId)
                .kitchenTaskId(kitchenTaskId)
                .build();
    }
}
