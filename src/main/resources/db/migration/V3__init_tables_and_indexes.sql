-- =====================================================
-- V3: Create all tables and indexes.
-- =====================================================

-- ─────────────────────────────────────────────
-- SYSTEM SETTINGS (public schema)
-- ─────────────────────────────────────────────
CREATE TABLE system_settings (
    key        VARCHAR(100) PRIMARY KEY,
    value      TEXT         NOT NULL,
    updated_at TIMESTAMPTZ,
    updated_by BIGINT
);

-- ─────────────────────────────────────────────
-- IDENTITY
-- ─────────────────────────────────────────────
CREATE TABLE identity.staff (
    id            BIGINT            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(255)      NOT NULL,
    role          staff_role_enum   NOT NULL,
    username      VARCHAR(100)      NOT NULL UNIQUE,
    password_hash TEXT              NOT NULL,
    status        staff_status_enum NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ       NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ,
    deleted_at    TIMESTAMPTZ
);
CREATE INDEX idx_staff_username ON identity.staff(username) WHERE deleted_at IS NULL;
CREATE INDEX idx_staff_role     ON identity.staff(role)     WHERE status = 'ACTIVE';

-- ─────────────────────────────────────────────
-- MENU
-- ─────────────────────────────────────────────
CREATE TABLE menu.menu_categories (
    id            BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(255)  NOT NULL,
    description   TEXT,
    display_order INT           NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ,
    deleted_at    TIMESTAMPTZ
);

CREATE TABLE menu.menu_items (
    id                BIGINT              GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_id       BIGINT              NOT NULL REFERENCES menu.menu_categories(id) ON DELETE RESTRICT,
    name              VARCHAR(255)        NOT NULL,
    description       TEXT,
    price_vnd         BIGINT              NOT NULL CHECK (price_vnd >= 0),
    cook_time         INT                 CHECK (cook_time >= 0),
    image_url         TEXT,
    image_public_id   VARCHAR(255),
    is_available      BOOLEAN             NOT NULL DEFAULT TRUE,
    item_type         menu_item_type_enum NOT NULL DEFAULT 'SINGLE',
    combo_kind        combo_kind_enum,
    item_tax_mode     VARCHAR(20)         CHECK (item_tax_mode IN ('NO_TAX', 'EXCLUSIVE', 'INCLUSIVE')),
    item_tax_rate_bps INT                 CHECK (item_tax_rate_bps >= 0),
    created_at        TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ,
    deleted_at        TIMESTAMPTZ,
    CONSTRAINT ck_menu_items_combo_kind CHECK (
        (item_type = 'SINGLE' AND combo_kind IS NULL)
        OR
        (item_type = 'COMBO'  AND combo_kind IS NOT NULL)
    )
);
CREATE INDEX idx_menu_items_category  ON menu.menu_items(category_id);
CREATE INDEX idx_menu_items_available ON menu.menu_items(is_available)  WHERE deleted_at IS NULL;
CREATE INDEX idx_menu_items_item_type ON menu.menu_items(item_type)     WHERE deleted_at IS NULL;

CREATE TABLE menu.combo_fixed_components (
    id                BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    combo_item_id     BIGINT      NOT NULL REFERENCES menu.menu_items(id) ON DELETE CASCADE,
    component_item_id BIGINT      NOT NULL REFERENCES menu.menu_items(id) ON DELETE RESTRICT,
    quantity          INT         NOT NULL CHECK (quantity > 0),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_combo_fixed_component UNIQUE (combo_item_id, component_item_id),
    CONSTRAINT ck_combo_fixed_no_self   CHECK  (combo_item_id <> component_item_id)
);
CREATE INDEX idx_combo_fixed_combo     ON menu.combo_fixed_components(combo_item_id);
CREATE INDEX idx_combo_fixed_component ON menu.combo_fixed_components(component_item_id);

CREATE TABLE menu.combo_pick_slots (
    id            BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    combo_item_id BIGINT       NOT NULL REFERENCES menu.menu_items(id) ON DELETE CASCADE,
    name          VARCHAR(255) NOT NULL,
    min_select    INT          NOT NULL DEFAULT 0 CHECK (min_select >= 0),
    max_select    INT          NOT NULL            CHECK (max_select >= 0),
    display_order INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_combo_pick_slot_min_max CHECK (min_select <= max_select)
);
CREATE INDEX idx_combo_pick_slots_combo ON menu.combo_pick_slots(combo_item_id);

CREATE TABLE menu.combo_pick_slot_items (
    id           BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slot_id      BIGINT      NOT NULL REFERENCES menu.combo_pick_slots(id) ON DELETE CASCADE,
    menu_item_id BIGINT      NOT NULL REFERENCES menu.menu_items(id)       ON DELETE RESTRICT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_combo_pick_slot_item UNIQUE (slot_id, menu_item_id)
);
CREATE INDEX idx_combo_pick_slot_items_slot ON menu.combo_pick_slot_items(slot_id);
CREATE INDEX idx_combo_pick_slot_items_item ON menu.combo_pick_slot_items(menu_item_id);

-- ─────────────────────────────────────────────
-- INVENTORY
-- ─────────────────────────────────────────────
CREATE TABLE inventory.ingredients (
    id                  BIGINT               GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                VARCHAR(255)         NOT NULL,
    unit                ingredient_unit_enum NOT NULL,
    current_qty         NUMERIC(12,2)        NOT NULL DEFAULT 0 CHECK (current_qty >= 0),
    low_stock_threshold NUMERIC(12,2)        NOT NULL DEFAULT 0 CHECK (low_stock_threshold >= 0),
    image_url           TEXT,
    created_at          TIMESTAMPTZ          NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    deleted_at          TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_ingredients_name ON inventory.ingredients(name) WHERE deleted_at IS NULL;
CREATE INDEX idx_ingredients_low_stock
    ON inventory.ingredients(current_qty, low_stock_threshold)
    WHERE deleted_at IS NULL;

CREATE TABLE inventory.stock_transactions (
    id              BIGINT              GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ingredient_id   BIGINT              NOT NULL REFERENCES inventory.ingredients(id),
    txn_type        stock_txn_type_enum NOT NULL,
    quantity_before NUMERIC(12,2)       NOT NULL,
    quantity_change NUMERIC(12,2)       NOT NULL,
    quantity_after  NUMERIC(12,2)       NOT NULL,
    note            TEXT,
    performed_by    BIGINT              REFERENCES identity.staff(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ         NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_stock_txn_ingredient ON inventory.stock_transactions(ingredient_id);
CREATE INDEX idx_stock_txn_created    ON inventory.stock_transactions(created_at DESC);

CREATE TABLE menu.menu_item_ingredients (
    id            BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    menu_item_id  BIGINT        NOT NULL REFERENCES menu.menu_items(id)       ON DELETE CASCADE,
    ingredient_id BIGINT        NOT NULL REFERENCES inventory.ingredients(id) ON DELETE RESTRICT,
    quantity      NUMERIC(12,2) NOT NULL CHECK (quantity > 0),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_menu_item_ingredient UNIQUE (menu_item_id, ingredient_id)
);
CREATE INDEX idx_recipe_item       ON menu.menu_item_ingredients(menu_item_id);
CREATE INDEX idx_recipe_ingredient ON menu.menu_item_ingredients(ingredient_id);

-- ─────────────────────────────────────────────
-- TABLE MANAGEMENT
-- ─────────────────────────────────────────────
CREATE TABLE table_mgmt.tables (
    id         BIGINT            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    floor      INT               NOT NULL CHECK (floor > 0),
    table_no   INT               NOT NULL CHECK (table_no > 0),
    table_code VARCHAR(16)       GENERATED ALWAYS AS (floor::text || '-' || LPAD(table_no::text, 3, '0')) STORED,
    capacity   INT               NOT NULL DEFAULT 4 CHECK (capacity > 0),
    status     table_status_enum NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMPTZ       NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_tables_floor_table_no UNIQUE (floor, table_no),
    CONSTRAINT uq_tables_table_code     UNIQUE (table_code)
);
CREATE INDEX idx_tables_status ON table_mgmt.tables(status)     WHERE deleted_at IS NULL;
CREATE INDEX idx_tables_code   ON table_mgmt.tables(table_code) WHERE deleted_at IS NULL;

CREATE TABLE table_mgmt.table_qr_codes (
    id                     BIGINT              GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    table_id               BIGINT              NOT NULL UNIQUE REFERENCES table_mgmt.tables(id) ON DELETE CASCADE,
    qr_key                 VARCHAR(120)        NOT NULL UNIQUE,
    qr_image_url           TEXT,
    qr_image_public_id     VARCHAR(255),
    status                 qr_code_status_enum NOT NULL DEFAULT 'ACTIVE',
    rotate_after           TIMESTAMPTZ,
    last_issued_session_at TIMESTAMPTZ,
    created_at             TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ,
    disabled_at            TIMESTAMPTZ
);
CREATE INDEX idx_table_qr_codes_status       ON table_mgmt.table_qr_codes(status);
CREATE INDEX idx_table_qr_codes_rotate_after ON table_mgmt.table_qr_codes(rotate_after) WHERE rotate_after IS NOT NULL;

CREATE TABLE table_mgmt.qr_sessions (
    id             BIGINT                 GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id     VARCHAR(64)            NOT NULL UNIQUE,
    qr_code_id     BIGINT                 NOT NULL REFERENCES table_mgmt.table_qr_codes(id) ON DELETE CASCADE,
    table_id       BIGINT                 NOT NULL REFERENCES table_mgmt.tables(id)          ON DELETE CASCADE,
    issued_to      VARCHAR(120),
    status         qr_session_status_enum NOT NULL DEFAULT 'ACTIVE',
    issued_at      TIMESTAMPTZ            NOT NULL DEFAULT NOW(),
    expires_at     TIMESTAMPTZ            NOT NULL,
    revoked_at     TIMESTAMPTZ,
    last_access_at TIMESTAMPTZ,
    CONSTRAINT ck_qr_session_expiry CHECK (expires_at > issued_at)
);
CREATE INDEX idx_qr_sessions_table_status ON table_mgmt.qr_sessions(table_id, status);
CREATE INDEX idx_qr_sessions_expires_at   ON table_mgmt.qr_sessions(expires_at);

-- ─────────────────────────────────────────────
-- SHIFT
-- ─────────────────────────────────────────────
CREATE TABLE shift.cashier_shifts (
    id                BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cashier_id        BIGINT        NOT NULL REFERENCES identity.staff(id) ON DELETE RESTRICT,
    opened_by         BIGINT        REFERENCES identity.staff(id) ON DELETE SET NULL,
    closed_by         BIGINT        REFERENCES identity.staff(id) ON DELETE SET NULL,
    opened_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    closed_at         TIMESTAMPTZ,
    opening_total_vnd BIGINT        NOT NULL CHECK (opening_total_vnd >= 0),
    closing_total_vnd BIGINT                 CHECK (closing_total_vnd >= 0),
    notes             TEXT,
    closing_notes     TEXT,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ,
    deleted_at        TIMESTAMPTZ,
    CONSTRAINT uq_cashier_shift_concurrent UNIQUE (cashier_id, opened_at)
);
CREATE INDEX idx_cashier_shifts_cashier_id ON shift.cashier_shifts(cashier_id)             WHERE deleted_at IS NULL;
CREATE INDEX idx_cashier_shifts_opened_at  ON shift.cashier_shifts(opened_at DESC)         WHERE deleted_at IS NULL;
CREATE INDEX idx_cashier_shifts_closed_at  ON shift.cashier_shifts(closed_at)              WHERE closed_at IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_cashier_shifts_active     ON shift.cashier_shifts(cashier_id, closed_at)  WHERE closed_at IS NULL AND deleted_at IS NULL;
CREATE INDEX idx_cashier_shifts_opened_by  ON shift.cashier_shifts(opened_by)              WHERE opened_by IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_cashier_shifts_closed_by  ON shift.cashier_shifts(closed_by)              WHERE closed_by IS NOT NULL AND deleted_at IS NULL;

-- ─────────────────────────────────────────────
-- ORDERING
-- ─────────────────────────────────────────────
CREATE TABLE ordering.orders (
    id                   BIGINT            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    table_id             BIGINT            NOT NULL REFERENCES table_mgmt.tables(id) ON DELETE RESTRICT,
    status               order_status_enum NOT NULL DEFAULT 'CREATED',
    total_amount_vnd     BIGINT            NOT NULL DEFAULT 0 CHECK (total_amount_vnd >= 0),
    subtotal_amount_vnd  BIGINT            NOT NULL DEFAULT 0 CHECK (subtotal_amount_vnd >= 0),
    tax_amount_vnd       BIGINT            NOT NULL DEFAULT 0 CHECK (tax_amount_vnd >= 0),
    tax_mode             VARCHAR(20)       NOT NULL DEFAULT 'NO_TAX'
                             CHECK (tax_mode IN ('NO_TAX', 'EXCLUSIVE', 'INCLUSIVE', 'MIXED')),
    tax_rate_bps         INT               NOT NULL DEFAULT 0   CHECK (tax_rate_bps >= 0),
    tax_snapshot_at      TIMESTAMPTZ,
    tax_snapshot_by      BIGINT            REFERENCES identity.staff(id) ON DELETE SET NULL,
    tax_snapshot_version INT               NOT NULL DEFAULT 1   CHECK (tax_snapshot_version >= 1),
    confirmed_by         BIGINT            REFERENCES identity.staff(id) ON DELETE SET NULL,
    served_by            BIGINT            REFERENCES identity.staff(id) ON DELETE SET NULL,
    note                 TEXT,
    created_at           TIMESTAMPTZ       NOT NULL DEFAULT NOW(),
    confirmed_at         TIMESTAMPTZ,
    ready_at             TIMESTAMPTZ,
    served_at            TIMESTAMPTZ,
    paid_at              TIMESTAMPTZ,
    cancelled_at         TIMESTAMPTZ
);
CREATE INDEX idx_orders_table_status      ON ordering.orders(table_id, status);
CREATE INDEX idx_orders_created_at        ON ordering.orders(created_at DESC);
CREATE INDEX idx_orders_status_created_at ON ordering.orders(status, created_at DESC);
CREATE INDEX idx_orders_active            ON ordering.orders(table_id, created_at DESC)
    WHERE status NOT IN ('PAID', 'CANCELLED');
CREATE UNIQUE INDEX uq_orders_single_active_per_table ON ordering.orders(table_id)
    WHERE status IN ('CREATED', 'CONFIRMED', 'PREPARING', 'READY', 'SERVED');

CREATE TABLE ordering.order_revisions (
    id               BIGINT               GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id         BIGINT               NOT NULL REFERENCES ordering.orders(id) ON DELETE CASCADE,
    revision_number  INT                  NOT NULL CHECK (revision_number > 0),
    revision_source  revision_source_enum NOT NULL,
    created_by_staff BIGINT               REFERENCES identity.staff(id)                   ON DELETE SET NULL,
    qr_session_id    VARCHAR(64)          REFERENCES table_mgmt.qr_sessions(session_id)   ON DELETE SET NULL,
    created_at       TIMESTAMPTZ          NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_order_revision_source CHECK (
        (revision_source = 'CUSTOMER_QR' AND qr_session_id    IS NOT NULL)
        OR
        (revision_source = 'STAFF'       AND created_by_staff IS NOT NULL)
    ),
    CONSTRAINT uq_order_revision UNIQUE (order_id, revision_number)
);
CREATE INDEX idx_order_revisions_created_at ON ordering.order_revisions(created_at DESC);

CREATE TABLE ordering.order_items (
    id                   BIGINT                 GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    revision_id          BIGINT                 NOT NULL REFERENCES ordering.order_revisions(id) ON DELETE CASCADE,
    menu_item_id         BIGINT                 REFERENCES menu.menu_items(id) ON DELETE RESTRICT,
    quantity             INT                    NOT NULL CHECK (quantity > 0),
    unit_price_vnd       BIGINT                 NOT NULL CHECK (unit_price_vnd >= 0),
    subtotal_vnd         BIGINT                 GENERATED ALWAYS AS (unit_price_vnd * quantity) STORED,
    unit_tax_mode        VARCHAR(20),
    unit_tax_rate_bps    INT,
    net_subtotal_vnd     BIGINT,
    tax_subtotal_vnd     BIGINT,
    note                 TEXT,
    status               order_item_status_enum NOT NULL DEFAULT 'PENDING',
    parent_order_item_id BIGINT                 REFERENCES ordering.order_items(id) ON DELETE CASCADE,
    is_billable          BOOLEAN                NOT NULL DEFAULT TRUE,
    is_combo_parent      BOOLEAN                NOT NULL DEFAULT FALSE,
    combo_snapshot       JSONB,
    created_at           TIMESTAMPTZ            NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_order_items_combo_parent CHECK (
        is_combo_parent = FALSE OR parent_order_item_id IS NULL
    ),
    CONSTRAINT ck_order_items_child_not_billable CHECK (
        parent_order_item_id IS NULL
        OR (parent_order_item_id IS NOT NULL AND is_billable = FALSE AND is_combo_parent = FALSE)
    )
);
CREATE INDEX idx_order_items_revision          ON ordering.order_items(revision_id);
CREATE INDEX idx_order_items_menu_item         ON ordering.order_items(menu_item_id);
CREATE INDEX idx_order_items_parent            ON ordering.order_items(parent_order_item_id);
CREATE INDEX idx_order_items_created_at        ON ordering.order_items(created_at DESC);
CREATE INDEX idx_order_items_status_created_at ON ordering.order_items(status, created_at DESC);

-- ─────────────────────────────────────────────
-- KITCHEN
-- ─────────────────────────────────────────────
CREATE TABLE kitchen.kitchen_tasks (
    id                  BIGINT                   GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_item_id       BIGINT                   NOT NULL UNIQUE REFERENCES ordering.order_items(id) ON DELETE CASCADE,
    order_id            BIGINT,
    table_id            BIGINT,
    menu_item_id        BIGINT,
    menu_item_name      TEXT,
    menu_item_image_url TEXT,
    quantity            INT,
    order_item_note     TEXT,
    order_note          TEXT,
    expected_cook_time  INT,
    status              kitchen_task_status_enum NOT NULL DEFAULT 'CREATED',
    started_at          TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ              NOT NULL DEFAULT NOW(),
    actual_cook_seconds INT GENERATED ALWAYS AS (
        CASE
            WHEN started_at IS NOT NULL AND completed_at IS NOT NULL
            THEN EXTRACT(EPOCH FROM (completed_at - started_at))::INT
            ELSE NULL
        END
    ) STORED
);
CREATE INDEX idx_kitchen_tasks_active            ON kitchen.kitchen_tasks(status) WHERE status IN ('CREATED', 'COOKING');
CREATE INDEX idx_kitchen_tasks_created_at        ON kitchen.kitchen_tasks(created_at DESC);
CREATE INDEX idx_kitchen_tasks_status_created_at ON kitchen.kitchen_tasks(status, created_at DESC);
CREATE INDEX idx_kitchen_tasks_order_id          ON kitchen.kitchen_tasks(order_id);
CREATE INDEX idx_kitchen_tasks_menu_item_id      ON kitchen.kitchen_tasks(menu_item_id);

CREATE TABLE kitchen.kitchen_batches (
    id                       BIGINT                    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    menu_item_id             BIGINT                    NOT NULL REFERENCES menu.menu_items(id) ON DELETE RESTRICT,
    quantity                 INT                       NOT NULL CHECK (quantity > 0),
    status                   kitchen_batch_status_enum NOT NULL DEFAULT 'SUGGESTED',
    source                   batch_source_enum         NOT NULL DEFAULT 'AI',
    ai_confidence            NUMERIC(4,2)              CHECK (ai_confidence BETWEEN 0 AND 1),
    estimated_saving_minutes INT                       CHECK (estimated_saving_minutes >= 0),
    batch_note               TEXT,
    started_at               TIMESTAMPTZ,
    completed_at             TIMESTAMPTZ,
    created_at               TIMESTAMPTZ               NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_kitchen_batches_active            ON kitchen.kitchen_batches(status) WHERE status NOT IN ('DONE');
CREATE INDEX idx_kitchen_batches_created_at        ON kitchen.kitchen_batches(created_at DESC);
CREATE INDEX idx_kitchen_batches_status_created_at ON kitchen.kitchen_batches(status, created_at DESC);

-- batch_id is NOT unique: one batch can hold multiple tasks.
CREATE TABLE kitchen.kitchen_batch_items (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    batch_id        BIGINT NOT NULL REFERENCES kitchen.kitchen_batches(id) ON DELETE CASCADE,
    kitchen_task_id BIGINT NOT NULL UNIQUE REFERENCES kitchen.kitchen_tasks(id) ON DELETE CASCADE
);
CREATE INDEX idx_batch_items_batch ON kitchen.kitchen_batch_items(batch_id);

CREATE TABLE kitchen.batch_performance (
    id               BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    batch_id         BIGINT      NOT NULL UNIQUE REFERENCES kitchen.kitchen_batches(id) ON DELETE CASCADE,
    baseline_minutes INT         NOT NULL CHECK (baseline_minutes > 0),
    actual_minutes   INT                  CHECK (actual_minutes > 0),
    saving_minutes   INT GENERATED ALWAYS AS (
        CASE
            WHEN actual_minutes IS NOT NULL THEN baseline_minutes - actual_minutes
            ELSE NULL
        END
    ) STORED,
    recorded_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_batch_perf_batch              ON kitchen.batch_performance(batch_id);
CREATE INDEX idx_batch_performance_recorded_at ON kitchen.batch_performance(recorded_at DESC);

-- ─────────────────────────────────────────────
-- PAYMENT
-- ─────────────────────────────────────────────
CREATE TABLE payment.payments (
    id                      BIGINT                GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id                BIGINT                NOT NULL REFERENCES ordering.orders(id)      ON DELETE RESTRICT,
    shift_id                BIGINT                         REFERENCES shift.cashier_shifts(id) ON DELETE SET NULL,
    amount_vnd              BIGINT                NOT NULL CHECK (amount_vnd > 0),
    subtotal_amount_vnd     BIGINT                NOT NULL DEFAULT 0 CHECK (subtotal_amount_vnd >= 0),
    tax_amount_vnd          BIGINT                NOT NULL DEFAULT 0 CHECK (tax_amount_vnd >= 0),
    tax_mode                VARCHAR(20)           NOT NULL DEFAULT 'NO_TAX'
                                CHECK (tax_mode IN ('NO_TAX', 'EXCLUSIVE', 'INCLUSIVE', 'MIXED')),
    tax_rate_bps            INT                   NOT NULL DEFAULT 0 CHECK (tax_rate_bps >= 0),
    payment_method          payment_method_enum   NOT NULL,
    provider                payment_provider_enum NOT NULL,
    status                  payment_status_enum   NOT NULL DEFAULT 'PENDING',
    CONSTRAINT ck_payment_method_provider CHECK (
        (payment_method = 'QR_CODE'   AND provider IN ('VNPAY', 'VIETQR')) OR
        (payment_method = 'VNPAY_ATM' AND provider = 'VNPAY')              OR
        (payment_method = 'CASH'      AND provider = 'CASH')
    ),
    provider_transaction_id VARCHAR(255),
    provider_response_code  VARCHAR(20),
    vnp_tmn_code            VARCHAR(20),
    vnp_bank_tran_no        VARCHAR(255),
    vnp_transaction_status  VARCHAR(10),
    vnp_pay_date            VARCHAR(14),
    bank_code               VARCHAR(50),
    card_type               VARCHAR(20),
    qr_content              TEXT,
    qr_expires_at           TIMESTAMPTZ,
    provider_payload        JSONB,
    cashier_id              BIGINT                         REFERENCES identity.staff(id) ON DELETE SET NULL,
    created_at              TIMESTAMPTZ           NOT NULL DEFAULT NOW(),
    paid_at                 TIMESTAMPTZ,
    failed_at               TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_payment_order_success       ON payment.payments(order_id) WHERE status = 'SUCCESS';
CREATE INDEX idx_payments_order                    ON payment.payments(order_id);
CREATE INDEX idx_payments_status                   ON payment.payments(status);
CREATE INDEX idx_payments_status_created_at        ON payment.payments(status, created_at DESC);
CREATE INDEX idx_payments_provider_txn             ON payment.payments(provider_transaction_id) WHERE provider_transaction_id IS NOT NULL;
CREATE INDEX idx_payments_created_at               ON payment.payments(created_at DESC);
CREATE INDEX idx_payments_shift_id                 ON payment.payments(shift_id) WHERE shift_id IS NOT NULL;

CREATE TABLE payment.payment_webhooks (
    id              BIGINT                GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payment_id      BIGINT                REFERENCES payment.payments(id) ON DELETE SET NULL,
    provider        payment_provider_enum NOT NULL,
    http_method     VARCHAR(10)           NOT NULL CHECK (http_method IN ('GET', 'POST')),
    raw_payload     JSONB                 NOT NULL,
    signature       VARCHAR(512),
    signature_valid BOOLEAN,
    status          webhook_status_enum   NOT NULL DEFAULT 'PENDING',
    retry_count     INT                   NOT NULL DEFAULT 0 CHECK (retry_count >= 0),
    received_at     TIMESTAMPTZ           NOT NULL DEFAULT NOW(),
    processed_at    TIMESTAMPTZ,
    error_message   TEXT
);
CREATE INDEX idx_webhooks_payment                    ON payment.payment_webhooks(payment_id) WHERE payment_id IS NOT NULL;
CREATE INDEX idx_webhooks_status                     ON payment.payment_webhooks(provider, status);
CREATE INDEX idx_webhooks_received                   ON payment.payment_webhooks(received_at DESC);
CREATE INDEX idx_webhooks_payload_gin                ON payment.payment_webhooks USING GIN (raw_payload);
CREATE INDEX idx_webhooks_provider_status_received   ON payment.payment_webhooks(provider, status, received_at DESC);

CREATE TABLE payment.payment_transactions (
    id                      BIGINT                GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payment_id              BIGINT                NOT NULL REFERENCES payment.payments(id) ON DELETE CASCADE,
    provider                payment_provider_enum NOT NULL,
    transaction_type        txn_type_enum         NOT NULL,
    provider_transaction_id VARCHAR(255),
    amount                  NUMERIC(12,2)         CHECK (amount > 0),
    status                  txn_status_enum       NOT NULL,
    raw_request             JSONB,
    raw_response            JSONB,
    http_status_code        INT,
    duration_ms             INT,
    created_at              TIMESTAMPTZ           NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_payment_txn_payment        ON payment.payment_transactions(payment_id);
CREATE INDEX idx_payment_txn_created        ON payment.payment_transactions(created_at DESC);
CREATE INDEX idx_payment_txn_status_created ON payment.payment_transactions(status, created_at DESC);

CREATE TABLE payment.refunds (
    id                 BIGINT             GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payment_id         BIGINT             NOT NULL REFERENCES payment.payments(id) ON DELETE RESTRICT,
    amount_vnd         BIGINT             NOT NULL CHECK (amount_vnd > 0),
    reason             TEXT,
    status             refund_status_enum NOT NULL DEFAULT 'INITIATED',
    provider_refund_id VARCHAR(255),
    requested_by       BIGINT             REFERENCES identity.staff(id) ON DELETE SET NULL,
    raw_request        JSONB,
    raw_response       JSONB,
    created_at         TIMESTAMPTZ        NOT NULL DEFAULT NOW(),
    completed_at       TIMESTAMPTZ
);
CREATE INDEX idx_refunds_payment ON payment.refunds(payment_id);
CREATE INDEX idx_refunds_status  ON payment.refunds(status);

-- ─────────────────────────────────────────────
-- ANALYTICS
-- ─────────────────────────────────────────────
CREATE TABLE analytics.order_events (
    id           BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id     BIGINT       NOT NULL REFERENCES ordering.orders(id) ON DELETE CASCADE,
    event_type   VARCHAR(100) NOT NULL,
    event_source VARCHAR(50),
    actor_id     BIGINT,
    actor_type   VARCHAR(50),
    metadata     JSONB,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_order_events_order        ON analytics.order_events(order_id);
CREATE INDEX idx_order_events_type         ON analytics.order_events(event_type);
CREATE INDEX idx_order_events_created_at   ON analytics.order_events(created_at DESC);
CREATE INDEX idx_order_events_type_created ON analytics.order_events(event_type, created_at DESC);
CREATE INDEX idx_order_events_metadata_gin ON analytics.order_events USING GIN (metadata);

-- ─────────────────────────────────────────────
-- SUPPORT
-- ─────────────────────────────────────────────
CREATE TABLE support.support_requests (
    id                   BIGINT                      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    message              TEXT                        NOT NULL,
    table_code           VARCHAR(16)                 NOT NULL,
    qr_session_id        VARCHAR(64),
    status               support_request_status_enum NOT NULL DEFAULT 'CREATED',
    assigned_to_staff_id BIGINT,
    created_at           TIMESTAMPTZ                 NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ,
    deleted_at           TIMESTAMPTZ,
    CONSTRAINT fk_support_request_staff
        FOREIGN KEY (assigned_to_staff_id) REFERENCES identity.staff(id) ON DELETE SET NULL,
    CONSTRAINT fk_support_request_qr_session
        FOREIGN KEY (qr_session_id) REFERENCES table_mgmt.qr_sessions(session_id) ON DELETE SET NULL
);
CREATE INDEX idx_support_requests_table_code     ON support.support_requests(table_code)           WHERE deleted_at IS NULL;
CREATE INDEX idx_support_requests_status         ON support.support_requests(status)               WHERE deleted_at IS NULL;
CREATE INDEX idx_support_requests_created_at     ON support.support_requests(created_at DESC)      WHERE deleted_at IS NULL;
CREATE INDEX idx_support_requests_assigned_staff ON support.support_requests(assigned_to_staff_id) WHERE deleted_at IS NULL;
