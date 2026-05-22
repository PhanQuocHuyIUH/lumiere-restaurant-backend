-- =====================================================
-- V9: Customer "Request payment" feature.
--      Adds payment.payment_requests so a customer at a
--      QR-table can signal the cashier without going
--      through the support channel.
-- =====================================================

CREATE TYPE payment_request_status_enum AS ENUM (
    'REQUESTED',
    'ACKNOWLEDGED',
    'COMPLETED',
    'CANCELLED'
);

CREATE TYPE payment_request_method_enum AS ENUM (
    'CASH',
    'TRANSFER'
);

CREATE TABLE payment.payment_requests (
    id                BIGINT                       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id          BIGINT                       NOT NULL REFERENCES ordering.orders(id) ON DELETE CASCADE,
    table_code        VARCHAR(32)                  NOT NULL,
    qr_session_id     VARCHAR(64),
    preferred_method  payment_request_method_enum  NOT NULL,
    status            payment_request_status_enum  NOT NULL DEFAULT 'REQUESTED',
    acknowledged_by   BIGINT                       REFERENCES identity.staff(id) ON DELETE SET NULL,
    cancelled_reason  TEXT,
    created_at        TIMESTAMPTZ                  NOT NULL DEFAULT NOW(),
    acknowledged_at   TIMESTAMPTZ,
    completed_at      TIMESTAMPTZ
);

CREATE INDEX idx_payment_requests_order              ON payment.payment_requests(order_id);
CREATE INDEX idx_payment_requests_table_code         ON payment.payment_requests(table_code);
CREATE INDEX idx_payment_requests_status_created     ON payment.payment_requests(status, created_at DESC);

-- Only one active (REQUESTED | ACKNOWLEDGED) PaymentRequest is allowed per order.
-- This guarantees we never duplicate-create when the customer hits the button twice.
CREATE UNIQUE INDEX uniq_payment_requests_active_per_order
    ON payment.payment_requests(order_id)
    WHERE status IN ('REQUESTED', 'ACKNOWLEDGED');
