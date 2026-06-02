-- =====================================================
-- V10: Add employee_code to identity.staff
-- Format: EMP + 4-digit zero-padded id (e.g. EMP0001).
-- Backfill existing rows from id, then enforce NOT NULL + UNIQUE.
-- =====================================================

ALTER TABLE identity.staff
    ADD COLUMN employee_code VARCHAR(20);

UPDATE identity.staff
SET employee_code = 'EMP' || LPAD(id::text, 4, '0')
WHERE employee_code IS NULL;

ALTER TABLE identity.staff
    ALTER COLUMN employee_code SET NOT NULL,
    ADD CONSTRAINT uq_staff_employee_code UNIQUE (employee_code);

CREATE INDEX idx_staff_employee_code ON identity.staff(employee_code) WHERE deleted_at IS NULL;
