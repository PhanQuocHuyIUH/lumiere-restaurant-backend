-- Create shift schema and cashier_shifts table
CREATE SCHEMA IF NOT EXISTS shift;

CREATE TABLE shift.cashier_shifts (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cashier_id BIGINT NOT NULL REFERENCES identity.staff(id) ON DELETE RESTRICT,
    opened_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    closed_at TIMESTAMPTZ,
    opening_total NUMERIC(12,2) NOT NULL CHECK (opening_total >= 0),
    closing_total NUMERIC(12,2) CHECK (closing_total >= 0),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_cashier_shift_concurrent UNIQUE (cashier_id, opened_at)
);

CREATE INDEX idx_cashier_shifts_cashier_id ON shift.cashier_shifts(cashier_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_cashier_shifts_opened_at ON shift.cashier_shifts(opened_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_cashier_shifts_closed_at ON shift.cashier_shifts(closed_at) WHERE closed_at IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_cashier_shifts_active ON shift.cashier_shifts(cashier_id, closed_at) WHERE closed_at IS NULL AND deleted_at IS NULL;
