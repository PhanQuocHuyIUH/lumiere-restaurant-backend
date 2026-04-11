package iuh.fit.se.shared.event;

import lombok.Getter;

@Getter
public final class BatchDoneEvent extends DomainEvent {

    private final Long batchId;
    private final Integer savingMinutes;

    public BatchDoneEvent(Long batchId, Integer savingMinutes) {
        this.batchId = batchId;
        this.savingMinutes = savingMinutes;
    }
}
