package iuh.fit.se.kitchen.job;

import iuh.fit.se.kitchen.domain.KitchenBatch;
import iuh.fit.se.kitchen.domain.KitchenBatchStatus;
import iuh.fit.se.kitchen.infrastructure.KitchenBatchItemRepository;
import iuh.fit.se.kitchen.infrastructure.KitchenBatchRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class KitchenBatchCleanupJob {

    private final KitchenBatchRepository kitchenBatchRepository;
    private final KitchenBatchItemRepository kitchenBatchItemRepository;

    @Scheduled(fixedRateString = "${lumiere.kitchen.batch.cleanup.rate:7200000}")
    @Transactional
    public void cleanupStaleSuggestedBatches() {
        Instant cutoffTime = Instant.now().minus(90, ChronoUnit.MINUTES);
        List<KitchenBatch> staleBatches = kitchenBatchRepository.findAllByStatusAndCreatedAtBefore(
                KitchenBatchStatus.SUGGESTED, cutoffTime
        );

        if (staleBatches.isEmpty()) {
            return;
        }

        List<Long> batchIds = staleBatches.stream()
                .map(KitchenBatch::getId)
                .toList();

        log.info("Purging {} stale SUGGESTED kitchen batches older than 90 minutes", batchIds.size());

        kitchenBatchItemRepository.deleteByBatchIdIn(batchIds);
        kitchenBatchRepository.deleteAllById(batchIds);
    }
}
