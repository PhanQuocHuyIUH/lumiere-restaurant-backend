package iuh.fit.se.kitchen.listener;

import iuh.fit.se.kitchen.application.KitchenService;
import iuh.fit.se.shared.event.OrderConfirmedEvent;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class KitchenOrderEventListener {

    private final KitchenService kitchenService;

    public KitchenOrderEventListener(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    @Async
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        kitchenService.createTasksForOrder(event);
    }
}
