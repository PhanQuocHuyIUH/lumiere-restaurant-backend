-- =====================================================
-- V12: Inventory lots + expiry tracking
-- Adds batch/lot tracking so stock movements track expiry per import,
-- not just the aggregate Ingredient.current_qty.
-- FEFO (First-Expired-First-Out) consumption uses (ingredient_id, expiry_date ASC).
-- =====================================================

ALTER TYPE stock_txn_type_enum ADD VALUE IF NOT EXISTS 'CONSUME';
ALTER TYPE stock_txn_type_enum ADD VALUE IF NOT EXISTS 'WASTE_EXPIRED';

CREATE TABLE inventory.stock_lots (
    id              BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ingredient_id   BIGINT          NOT NULL REFERENCES inventory.ingredients(id),
    initial_qty     NUMERIC(14,3)   NOT NULL CHECK (initial_qty > 0),
    remaining_qty   NUMERIC(14,3)   NOT NULL CHECK (remaining_qty >= 0),
    expiry_date     DATE            NOT NULL,
    imported_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    note            TEXT,
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_stock_lots_ingredient_expiry
    ON inventory.stock_lots(ingredient_id, expiry_date)
    WHERE remaining_qty > 0 AND deleted_at IS NULL;

CREATE INDEX idx_stock_lots_expiring
    ON inventory.stock_lots(expiry_date)
    WHERE remaining_qty > 0 AND deleted_at IS NULL;

-- Backfill: each ingredient currently with stock gets a single "legacy" lot
-- expiring 30 days from now (best-effort — managers can adjust afterwards).
INSERT INTO inventory.stock_lots (ingredient_id, initial_qty, remaining_qty, expiry_date, imported_at, note)
SELECT id, current_qty, current_qty, (CURRENT_DATE + INTERVAL '30 days')::date, NOW(),
       'Legacy lot backfilled by V12 migration'
FROM inventory.ingredients
WHERE deleted_at IS NULL AND current_qty > 0;
