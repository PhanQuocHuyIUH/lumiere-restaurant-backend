-- Drop idempotency tables as idempotency has been moved from database to Redis

DROP TABLE IF EXISTS billing.billing_idempotency_keys CASCADE;
DROP TABLE IF EXISTS kitchen.kitchen_idempotency_keys CASCADE;
DROP TABLE IF EXISTS ordering.idempotency_keys CASCADE;
