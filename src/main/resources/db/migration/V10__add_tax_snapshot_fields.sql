-- V10: Add tax snapshot and pricing fields for orders/payments

BEGIN;

ALTER TABLE ordering.orders ADD COLUMN subtotal_amount_vnd BIGINT;
ALTER TABLE ordering.orders ADD COLUMN tax_amount_vnd BIGINT;
ALTER TABLE ordering.orders ADD COLUMN tax_mode VARCHAR(20);
ALTER TABLE ordering.orders ADD COLUMN tax_rate_bps INT;
ALTER TABLE ordering.orders ADD COLUMN tax_snapshot_at TIMESTAMPTZ;
ALTER TABLE ordering.orders ADD COLUMN tax_snapshot_by BIGINT REFERENCES identity.staff(id) ON DELETE SET NULL;
ALTER TABLE ordering.orders ADD COLUMN tax_snapshot_version INT;

UPDATE ordering.orders
SET subtotal_amount_vnd = COALESCE(total_amount_vnd, 0),
    tax_amount_vnd = 0,
    tax_mode = 'NO_TAX',
    tax_rate_bps = 0,
    tax_snapshot_version = 1
WHERE subtotal_amount_vnd IS NULL
   OR tax_amount_vnd IS NULL
   OR tax_mode IS NULL
   OR tax_rate_bps IS NULL
   OR tax_snapshot_version IS NULL;

ALTER TABLE ordering.orders ALTER COLUMN subtotal_amount_vnd SET NOT NULL;
ALTER TABLE ordering.orders ALTER COLUMN tax_amount_vnd SET NOT NULL;
ALTER TABLE ordering.orders ALTER COLUMN tax_mode SET NOT NULL;
ALTER TABLE ordering.orders ALTER COLUMN tax_rate_bps SET NOT NULL;
ALTER TABLE ordering.orders ALTER COLUMN tax_snapshot_version SET NOT NULL;

ALTER TABLE ordering.orders
    ADD CONSTRAINT ck_orders_tax_mode
        CHECK (tax_mode IN ('NO_TAX', 'EXCLUSIVE', 'INCLUSIVE')),
    ADD CONSTRAINT ck_orders_tax_non_negative
        CHECK (subtotal_amount_vnd >= 0 AND tax_amount_vnd >= 0 AND tax_rate_bps >= 0),
    ADD CONSTRAINT ck_orders_tax_snapshot_version
        CHECK (tax_snapshot_version >= 1);

ALTER TABLE payment.payments ADD COLUMN subtotal_amount_vnd BIGINT;
ALTER TABLE payment.payments ADD COLUMN tax_amount_vnd BIGINT;
ALTER TABLE payment.payments ADD COLUMN tax_mode VARCHAR(20);
ALTER TABLE payment.payments ADD COLUMN tax_rate_bps INT;

UPDATE payment.payments
SET subtotal_amount_vnd = COALESCE(amount_vnd, 0),
    tax_amount_vnd = 0,
    tax_mode = 'NO_TAX',
    tax_rate_bps = 0
WHERE subtotal_amount_vnd IS NULL
   OR tax_amount_vnd IS NULL
   OR tax_mode IS NULL
   OR tax_rate_bps IS NULL;

ALTER TABLE payment.payments ALTER COLUMN subtotal_amount_vnd SET NOT NULL;
ALTER TABLE payment.payments ALTER COLUMN tax_amount_vnd SET NOT NULL;
ALTER TABLE payment.payments ALTER COLUMN tax_mode SET NOT NULL;
ALTER TABLE payment.payments ALTER COLUMN tax_rate_bps SET NOT NULL;

ALTER TABLE payment.payments
    ADD CONSTRAINT ck_payments_tax_mode
        CHECK (tax_mode IN ('NO_TAX', 'EXCLUSIVE', 'INCLUSIVE')),
    ADD CONSTRAINT ck_payments_tax_non_negative
        CHECK (subtotal_amount_vnd >= 0 AND tax_amount_vnd >= 0 AND tax_rate_bps >= 0);

COMMIT;
