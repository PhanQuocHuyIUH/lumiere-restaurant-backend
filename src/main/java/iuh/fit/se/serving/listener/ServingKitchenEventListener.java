package iuh.fit.se.serving.listener;

import iuh.fit.se.serving.application.ServingService;
import iuh.fit.se.shared.event.KitchenTaskDoneEvent;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ServingKitchenEventListener {

    private final ServingService servingService;

    public ServingKitchenEventListener(ServingService servingService) {
        this.servingService = servingService;
    }

    @Async
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onKitchenTaskDone(KitchenTaskDoneEvent event) {
        servingService.handleKitchenTaskDone(event.getOrderId());
    }
}
