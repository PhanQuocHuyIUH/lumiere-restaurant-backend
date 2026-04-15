-- Remove all MoMo historical data and schema support from payment module.

WITH momo_payment_ids AS (
    SELECT id
    FROM payment.payments
    WHERE provider::text = 'MOMO'
       OR payment_method::text IN ('MOMO_ATM', 'MOMO_CREDIT')
)
DELETE FROM payment.payment_webhooks
WHERE provider::text = 'MOMO'
   OR payment_id IN (SELECT id FROM momo_payment_ids);

WITH momo_payment_ids AS (
    SELECT id
    FROM payment.payments
    WHERE provider::text = 'MOMO'
       OR payment_method::text IN ('MOMO_ATM', 'MOMO_CREDIT')
)
DELETE FROM payment.refunds
WHERE payment_id IN (SELECT id FROM momo_payment_ids);

WITH momo_payment_ids AS (
    SELECT id
    FROM payment.payments
    WHERE provider::text = 'MOMO'
       OR payment_method::text IN ('MOMO_ATM', 'MOMO_CREDIT')
)
DELETE FROM payment.payments
WHERE id IN (SELECT id FROM momo_payment_ids);

DELETE FROM payment.payment_transactions
WHERE provider::text = 'MOMO';

ALTER TABLE payment.payments
    DROP CONSTRAINT IF EXISTS ck_payment_method_provider;

ALTER TABLE payment.payments
    DROP COLUMN IF EXISTS momo_order_id,
    DROP COLUMN IF EXISTS momo_request_id,
    DROP COLUMN IF EXISTS momo_pay_url,
    DROP COLUMN IF EXISTS momo_deeplink,
    DROP COLUMN IF EXISTS momo_qr_code_url;

ALTER TYPE payment_provider_enum RENAME TO payment_provider_enum_old;
CREATE TYPE payment_provider_enum AS ENUM ('VNPAY', 'VIETQR', 'CASH');

ALTER TABLE payment.payments
    ALTER COLUMN provider TYPE payment_provider_enum
    USING provider::text::payment_provider_enum;

ALTER TABLE payment.payment_webhooks
    ALTER COLUMN provider TYPE payment_provider_enum
    USING provider::text::payment_provider_enum;

ALTER TABLE payment.payment_transactions
    ALTER COLUMN provider TYPE payment_provider_enum
    USING provider::text::payment_provider_enum;

DROP TYPE payment_provider_enum_old;

ALTER TYPE payment_method_enum RENAME TO payment_method_enum_old;
CREATE TYPE payment_method_enum AS ENUM ('QR_CODE', 'VNPAY_ATM', 'CASH');

ALTER TABLE payment.payments
    ALTER COLUMN payment_method TYPE payment_method_enum
    USING payment_method::text::payment_method_enum;

DROP TYPE payment_method_enum_old;

ALTER TABLE payment.payments
    ADD CONSTRAINT ck_payment_method_provider CHECK (
        (payment_method = 'QR_CODE' AND provider IN ('VNPAY', 'VIETQR')) OR
        (payment_method = 'VNPAY_ATM' AND provider = 'VNPAY') OR
        (payment_method = 'CASH' AND provider = 'CASH')
    );
