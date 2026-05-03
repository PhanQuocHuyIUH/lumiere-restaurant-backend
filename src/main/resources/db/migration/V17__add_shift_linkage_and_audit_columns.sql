ALTER TABLE shift.cashier_shifts
    ADD COLUMN IF NOT EXISTS opened_by BIGINT REFERENCES identity.staff(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS closed_by BIGINT REFERENCES identity.staff(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS closing_notes TEXT;

CREATE INDEX IF NOT EXISTS idx_cashier_shifts_opened_by
    ON shift.cashier_shifts(opened_by)
    WHERE opened_by IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_cashier_shifts_closed_by
    ON shift.cashier_shifts(closed_by)
    WHERE closed_by IS NOT NULL AND deleted_at IS NULL;

ALTER TABLE payment.payments
    ADD COLUMN IF NOT EXISTS shift_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_schema = 'payment'
          AND table_name = 'payments'
          AND constraint_name = 'fk_payments_shift_id'
    ) THEN
        ALTER TABLE payment.payments
            ADD CONSTRAINT fk_payments_shift_id
            FOREIGN KEY (shift_id)
            REFERENCES shift.cashier_shifts(id)
            ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_payments_shift_id
    ON payment.payments(shift_id)
    WHERE shift_id IS NOT NULL;
