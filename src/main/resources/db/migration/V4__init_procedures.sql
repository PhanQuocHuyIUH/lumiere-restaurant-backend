-- Utility procedures for table management and ordering totals.

-- Set a table status by table_code and update updated_at.
CREATE OR REPLACE PROCEDURE table_mgmt.sp_set_table_status(
    IN p_table_code VARCHAR,
    IN p_status table_status_enum
)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE table_mgmt.tables
    SET status = p_status,
        updated_at = NOW()
    WHERE table_code = p_table_code
      AND deleted_at IS NULL;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Table not found or deleted: %', p_table_code;
    END IF;
END;
$$;

-- Rotate (or create) QR key for a table and force it back to ACTIVE state.
CREATE OR REPLACE PROCEDURE table_mgmt.sp_rotate_table_qr(
    IN p_table_code VARCHAR,
    IN p_qr_key VARCHAR
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_table_id BIGINT;
BEGIN
    IF p_qr_key IS NULL OR BTRIM(p_qr_key) = '' THEN
        RAISE EXCEPTION 'QR key is required';
    END IF;

    SELECT id
    INTO v_table_id
    FROM table_mgmt.tables
    WHERE table_code = p_table_code
      AND deleted_at IS NULL;

    IF v_table_id IS NULL THEN
        RAISE EXCEPTION 'Table not found or deleted: %', p_table_code;
    END IF;

    INSERT INTO table_mgmt.table_qr_codes (table_id, qr_key, status, created_at, updated_at)
    VALUES (v_table_id, p_qr_key, 'ACTIVE', NOW(), NOW())
    ON CONFLICT (table_id)
    DO UPDATE SET qr_key = EXCLUDED.qr_key,
                  status = 'ACTIVE',
                  disabled_at = NULL,
                  updated_at = NOW();
END;
$$;

-- Expire all ACTIVE QR sessions whose expires_at is in the past.
CREATE OR REPLACE PROCEDURE table_mgmt.sp_expire_qr_sessions()
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE table_mgmt.qr_sessions
    SET status = 'EXPIRED',
        revoked_at = COALESCE(revoked_at, NOW())
    WHERE status = 'ACTIVE'
      AND expires_at < NOW();
END;
$$;

-- Recompute order total_amount from the latest revision's order_items subtotal sum.
CREATE OR REPLACE PROCEDURE ordering.sp_refresh_order_total(
    IN p_order_id BIGINT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_latest_revision_id BIGINT;
    v_total NUMERIC(12,2);
BEGIN
    SELECT id
    INTO v_latest_revision_id
    FROM ordering.order_revisions
    WHERE order_id = p_order_id
    ORDER BY revision_number DESC
    LIMIT 1;

    IF v_latest_revision_id IS NULL THEN
        v_total := 0;
    ELSE
        SELECT COALESCE(SUM(subtotal), 0)
        INTO v_total
        FROM ordering.order_items
        WHERE revision_id = v_latest_revision_id;
    END IF;

    UPDATE ordering.orders
    SET total_amount = v_total
    WHERE id = p_order_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order not found: %', p_order_id;
    END IF;
END;
$$;

-- Delete expired idempotency key records to keep the table small.
CREATE OR REPLACE PROCEDURE shared.sp_cleanup_expired_idempotency_keys()
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM shared.idempotency_keys
    WHERE expires_at < NOW();
END;
$$;