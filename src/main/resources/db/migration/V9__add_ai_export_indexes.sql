CREATE INDEX IF NOT EXISTS idx_orders_status_created_at
    ON ordering.orders(status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_order_items_created_at
    ON ordering.order_items(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_order_items_status_created_at
    ON ordering.order_items(status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_order_revisions_created_at
    ON ordering.order_revisions(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_payments_status_created_at
    ON payment.payments(status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_payment_txn_status_created
    ON payment.payment_transactions(status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_webhooks_provider_status_received
    ON payment.payment_webhooks(provider, status, received_at DESC);

CREATE INDEX IF NOT EXISTS idx_order_events_type_created
    ON analytics.order_events(event_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_kitchen_tasks_created_at
    ON kitchen.kitchen_tasks(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_kitchen_tasks_status_created_at
    ON kitchen.kitchen_tasks(status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_kitchen_batches_created_at
    ON kitchen.kitchen_batches(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_kitchen_batches_status_created_at
    ON kitchen.kitchen_batches(status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_batch_performance_recorded_at
    ON kitchen.batch_performance(recorded_at DESC);
