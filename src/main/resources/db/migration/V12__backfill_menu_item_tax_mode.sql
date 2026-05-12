-- V12: Backfill item_tax_mode for existing menu items
-- V11 raised price_vnd to gross (×1.08) but left item_tax_mode NULL.
-- Set explicitly to INCLUSIVE so mode is not reliant on global default.
UPDATE menu.menu_items
SET item_tax_mode     = 'INCLUSIVE',
    item_tax_rate_bps = 800
WHERE deleted_at IS NULL
  AND item_tax_mode IS NULL;
