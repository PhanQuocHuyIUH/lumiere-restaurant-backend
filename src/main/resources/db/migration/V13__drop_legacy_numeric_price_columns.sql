-- V13: Drop all legacy NUMERIC(12,2) price columns superseded by BIGINT _vnd equivalents (V9–V11).
-- All Java entities already map exclusively to the _vnd columns; the old columns are dead weight.
--
-- Special handling for ordering.order_items:
--   • "subtotal" is GENERATED ALWAYS AS (quantity * unit_price) STORED — must be dropped before
--     "unit_price" (which it depends on).
--   • "subtotal_vnd" was added as a plain BIGINT in V9 and back-filled once.  The entity marks it
--     insertable=false/updatable=false with @Generated so Hibernate expects the DB to maintain it.
--     We therefore replace it with a proper GENERATED ALWAYS AS (unit_price_vnd * quantity) STORED
--     expression so the value stays correct on every INSERT/UPDATE without application logic.

BEGIN;

-- ─── menu.menu_items ──────────────────────────────────────────────────────────
-- price NUMERIC(12,2) → superseded by price_vnd BIGINT (V9)
ALTER TABLE menu.menu_items
    DROP COLUMN price;

-- ─── ordering.orders ──────────────────────────────────────────────────────────
-- total_amount NUMERIC(12,2) → superseded by total_amount_vnd BIGINT (V9)
ALTER TABLE ordering.orders
    DROP COLUMN total_amount;

-- ─── ordering.order_items ─────────────────────────────────────────────────────
-- Step 1: drop the GENERATED column first (depends on unit_price)
-- subtotal GENERATED ALWAYS AS (quantity * unit_price) → superseded by subtotal_vnd BIGINT (V9)
ALTER TABLE ordering.order_items
    DROP COLUMN subtotal;

-- Step 2: now safe to drop the base column
-- unit_price NUMERIC(12,2) → superseded by unit_price_vnd BIGINT (V9)
ALTER TABLE ordering.order_items
    DROP COLUMN unit_price;

-- Step 3: convert subtotal_vnd from a plain backfilled BIGINT into a proper generated column so
-- that Hibernate's @Generated annotation (insertable=false, updatable=false) works correctly on
-- every future INSERT/UPDATE without any application-level maintenance.
ALTER TABLE ordering.order_items
    DROP COLUMN subtotal_vnd;

ALTER TABLE ordering.order_items
    ADD COLUMN subtotal_vnd BIGINT
        GENERATED ALWAYS AS (unit_price_vnd * quantity) STORED;

-- ─── shift.cashier_shifts ─────────────────────────────────────────────────────
-- opening_total NUMERIC(12,2) → superseded by opening_total_vnd BIGINT (V9)
-- closing_total NUMERIC(12,2) → superseded by closing_total_vnd BIGINT (V9)
ALTER TABLE shift.cashier_shifts
    DROP COLUMN opening_total,
    DROP COLUMN closing_total;

-- ─── payment.payments ─────────────────────────────────────────────────────────
-- amount NUMERIC(12,2) → superseded by amount_vnd BIGINT (V9)
-- Note: payment.payment_transactions.amount is intentionally NOT touched — it was never migrated.
ALTER TABLE payment.payments
    DROP COLUMN amount;

-- ─── payment.refunds ──────────────────────────────────────────────────────────
-- amount NUMERIC(12,2) → superseded by amount_vnd BIGINT (V9)
ALTER TABLE payment.refunds
    DROP COLUMN amount;

COMMIT;
