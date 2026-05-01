-- Create support schema and support_requests table
CREATE SCHEMA IF NOT EXISTS support;

CREATE TABLE support.support_requests (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    message TEXT NOT NULL,
    table_code VARCHAR(16) NOT NULL,
    qr_session_id VARCHAR(64),
    status support_request_status_enum NOT NULL DEFAULT 'CREATED',
    assigned_to_staff_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_support_request_staff FOREIGN KEY (assigned_to_staff_id) REFERENCES identity.staff(id) ON DELETE SET NULL,
    CONSTRAINT fk_support_request_qr_session FOREIGN KEY (qr_session_id) REFERENCES table_mgmt.qr_sessions(session_id) ON DELETE SET NULL
);

CREATE INDEX idx_support_requests_table_code ON support.support_requests(table_code) WHERE deleted_at IS NULL;
CREATE INDEX idx_support_requests_status ON support.support_requests(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_support_requests_created_at ON support.support_requests(created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_support_requests_assigned_staff ON support.support_requests(assigned_to_staff_id) WHERE deleted_at IS NULL;
