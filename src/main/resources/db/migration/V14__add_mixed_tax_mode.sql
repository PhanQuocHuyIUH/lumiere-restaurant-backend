-- V14: Allow MIXED tax_mode on orders and payments.
--
-- Background: prior to V14, orders.tax_mode and payments.tax_mode only allowed
-- NO_TAX / EXCLUSIVE / INCLUSIVE.  After the single-source-of-tax refactor,
-- the order-level tax_mode is derived from item-level configs.  When an order
-- contains items with different tax modes the derived value is MIXED, which
-- signals "see order_items for per-line breakdown" rather than implying a
-- uniform rate.  The monetary amounts (subtotal_amount_vnd, tax_amount_vnd,
-- total_amount_vnd) are always correct regardless of this field.

BEGIN;

-- orders
ALTER TABLE ordering.orders
    DROP CONSTRAINT IF EXISTS ck_orders_tax_mode,
    ADD CONSTRAINT ck_orders_tax_mode
        CHECK (tax_mode IN ('NO_TAX', 'EXCLUSIVE', 'INCLUSIVE', 'MIXED'));

-- payments (mirrors orders for audit purposes)
ALTER TABLE payment.payments
    DROP CONSTRAINT IF EXISTS ck_payments_tax_mode,
    ADD CONSTRAINT ck_payments_tax_mode
        CHECK (tax_mode IN ('NO_TAX', 'EXCLUSIVE', 'INCLUSIVE', 'MIXED'));

COMMIT;
