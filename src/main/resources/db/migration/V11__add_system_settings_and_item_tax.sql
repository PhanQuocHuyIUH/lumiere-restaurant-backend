-- V11: Global system settings + per-item tax override + subtotal-level tax breakdown on order_items

-- 1. system_settings: generic key-value store for global configuration
CREATE TABLE system_settings (
    key        VARCHAR(100) PRIMARY KEY,
    value      TEXT         NOT NULL,
    updated_at TIMESTAMPTZ,
    updated_by BIGINT
);

-- Default: 8% INCLUSIVE (admin inputs gross price; backend INCLUSIVE-reverse-extracts net+tax)
INSERT INTO system_settings (key, value) VALUES
    ('tax.mode',     'INCLUSIVE'),
    ('tax.rate-bps', '800');

-- 2. Per-item tax override on menu_items (nullable = use global config)
ALTER TABLE menu.menu_items
    ADD COLUMN item_tax_mode     VARCHAR(20)
        CHECK (item_tax_mode IN ('NO_TAX', 'EXCLUSIVE', 'INCLUSIVE')),
    ADD COLUMN item_tax_rate_bps INT
        CHECK (item_tax_rate_bps >= 0);

-- Convert existing menu item prices: old price was net → new gross = ROUND(net * 1.08)
-- This makes price_vnd represent the gross (tax-inclusive) amount going forward.
UPDATE menu.menu_items
SET price_vnd = ROUND(price_vnd * 1.08)
WHERE deleted_at IS NULL;

-- 3. Per-item tax breakdown on order_items — stored at SUBTOTAL level (not per-unit)
--    Rationale: ROUND(grossUnit * qty) avoids off-by-one vs ROUND(unit) * qty.
--    unit_tax_mode/rate: item-level override captured at item-add time (null = global was used)
--    net_subtotal_vnd / tax_subtotal_vnd: PricingEngine.snapshot(grossUnit*qty) result
ALTER TABLE ordering.order_items
    ADD COLUMN unit_tax_mode     VARCHAR(20),
    ADD COLUMN unit_tax_rate_bps INT,
    ADD COLUMN net_subtotal_vnd  BIGINT,
    ADD COLUMN tax_subtotal_vnd  BIGINT;

-- Backfill existing order_items as NO_TAX (prices were net before this migration)
UPDATE ordering.order_items
SET unit_tax_mode     = 'NO_TAX',
    unit_tax_rate_bps = 0,
    net_subtotal_vnd  = subtotal_vnd,
    tax_subtotal_vnd  = 0;
