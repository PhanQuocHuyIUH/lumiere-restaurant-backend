-- V9: Add BIGINT currency columns (VND) and backfill from NUMERIC(12,2)

BEGIN;

ALTER TABLE menu.menu_items ADD COLUMN price_vnd BIGINT;
UPDATE menu.menu_items SET price_vnd = ROUND(price)::BIGINT WHERE price IS NOT NULL;
ALTER TABLE menu.menu_items ALTER COLUMN price_vnd SET NOT NULL;

ALTER TABLE ordering.orders ADD COLUMN total_amount_vnd BIGINT;
UPDATE ordering.orders SET total_amount_vnd = ROUND(total_amount)::BIGINT WHERE total_amount IS NOT NULL;
ALTER TABLE ordering.orders ALTER COLUMN total_amount_vnd SET NOT NULL;

ALTER TABLE ordering.order_items ADD COLUMN unit_price_vnd BIGINT;
ALTER TABLE ordering.order_items ADD COLUMN subtotal_vnd BIGINT;
UPDATE ordering.order_items SET unit_price_vnd = ROUND(unit_price)::BIGINT, subtotal_vnd = ROUND(subtotal)::BIGINT WHERE unit_price IS NOT NULL OR subtotal IS NOT NULL;
ALTER TABLE ordering.order_items ALTER COLUMN unit_price_vnd SET NOT NULL;
ALTER TABLE ordering.order_items ALTER COLUMN subtotal_vnd SET NOT NULL;

ALTER TABLE shift.cashier_shifts ADD COLUMN opening_total_vnd BIGINT;
ALTER TABLE shift.cashier_shifts ADD COLUMN closing_total_vnd BIGINT;
UPDATE shift.cashier_shifts SET opening_total_vnd = ROUND(opening_total)::BIGINT WHERE opening_total IS NOT NULL;
UPDATE shift.cashier_shifts SET closing_total_vnd = ROUND(closing_total)::BIGINT WHERE closing_total IS NOT NULL;
ALTER TABLE shift.cashier_shifts ALTER COLUMN opening_total_vnd SET NOT NULL;

ALTER TABLE payment.payments ADD COLUMN amount_vnd BIGINT;
UPDATE payment.payments SET amount_vnd = ROUND(amount)::BIGINT WHERE amount IS NOT NULL;
ALTER TABLE payment.payments ALTER COLUMN amount_vnd SET NOT NULL;

ALTER TABLE payment.refunds ADD COLUMN amount_vnd BIGINT;
UPDATE payment.refunds SET amount_vnd = ROUND(amount)::BIGINT WHERE amount IS NOT NULL;
ALTER TABLE payment.refunds ALTER COLUMN amount_vnd SET NOT NULL;

COMMIT;
