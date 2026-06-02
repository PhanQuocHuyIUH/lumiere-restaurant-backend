-- =====================================================
-- V11: KitchenTask.ordered_at
-- ordered_at = order.confirmed_at (waiter/customer placed order).
-- created_at is when the kitchen row was inserted (after AFTER_COMMIT listener).
-- ordered_at is the SLA anchor for "time customer has been waiting".
-- Backfill: existing rows use created_at as best-effort approximation.
-- =====================================================

ALTER TABLE kitchen.kitchen_tasks
    ADD COLUMN ordered_at TIMESTAMPTZ;

UPDATE kitchen.kitchen_tasks
SET ordered_at = created_at
WHERE ordered_at IS NULL;

ALTER TABLE kitchen.kitchen_tasks
    ALTER COLUMN ordered_at SET NOT NULL;

CREATE INDEX idx_kitchen_tasks_ordered_at ON kitchen.kitchen_tasks(ordered_at DESC);
