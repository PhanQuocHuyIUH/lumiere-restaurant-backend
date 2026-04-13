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
    id             BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_id    BIGINT        NOT NULL REFERENCES menu.menu_categories(id) ON DELETE RESTRICT,
    name           VARCHAR(255)  NOT NULL,
    description    TEXT,
    price          NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    cook_time      INT           CHECK (cook_time >= 0),
    image_url      TEXT,
    is_available   BOOLEAN       NOT NULL DEFAULT TRUE,
    kitchen_label  VARCHAR(255),
    kitchen_note   TEXT,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ,
    deleted_at     TIMESTAMPTZ
);
CREATE INDEX idx_menu_items_category  ON menu.menu_items(category_id);
CREATE INDEX idx_menu_items_available ON menu.menu_items(is_available) WHERE deleted_at IS NULL;

CREATE TABLE table_mgmt.tables (
    id            BIGINT            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    floor         INT               NOT NULL CHECK (floor > 0),
    table_no      INT               NOT NULL CHECK (table_no > 0),
    table_code    VARCHAR(16)       GENERATED ALWAYS AS (floor::text || '-' || LPAD(table_no::text, 3, '0')) STORED,
    capacity      INT               NOT NULL DEFAULT 4 CHECK (capacity > 0),
    status        table_status_enum NOT NULL DEFAULT 'AVAILABLE',
    created_at    TIMESTAMPTZ       NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ,
    deleted_at    TIMESTAMPTZ,
    CONSTRAINT uq_tables_floor_table_no UNIQUE (floor, table_no),
    CONSTRAINT uq_tables_table_code UNIQUE (table_code)
);
CREATE INDEX idx_tables_status ON table_mgmt.tables(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_tables_code   ON table_mgmt.tables(table_code) WHERE deleted_at IS NULL;

CREATE TABLE table_mgmt.table_qr_codes (
    id                     BIGINT              GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    table_id               BIGINT              NOT NULL UNIQUE REFERENCES table_mgmt.tables(id) ON DELETE CASCADE,
    qr_key                 VARCHAR(120)        NOT NULL UNIQUE,
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
    table_id       BIGINT                 NOT NULL REFERENCES table_mgmt.tables(id) ON DELETE CASCADE,
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

CREATE TABLE shared.idempotency_keys (
    id              BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    idem_key        VARCHAR(200)  NOT NULL,
    module          VARCHAR(50)   NOT NULL,
    operation       VARCHAR(100)  NOT NULL,
    response_status INT,
    response_body   JSONB,
    expires_at      TIMESTAMPTZ   NOT NULL DEFAULT (NOW() + INTERVAL '24 hours'),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_idempotency_scope UNIQUE (module, operation, idem_key)
);
CREATE INDEX idx_idem_scope   ON shared.idempotency_keys(module, operation, idem_key);
CREATE INDEX idx_idem_expires ON shared.idempotency_keys(expires_at);

CREATE TABLE ordering.orders (
    id            BIGINT            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    table_id      BIGINT            NOT NULL REFERENCES table_mgmt.tables(id) ON DELETE RESTRICT,
    status        order_status_enum NOT NULL DEFAULT 'CREATED',
    total_amount  NUMERIC(12,2)     NOT NULL DEFAULT 0 CHECK (total_amount >= 0),
    confirmed_by  BIGINT            REFERENCES identity.staff(id) ON DELETE SET NULL,
    served_by     BIGINT            REFERENCES identity.staff(id) ON DELETE SET NULL,
    note          TEXT,
    split_bill_allowed BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ       NOT NULL DEFAULT NOW(),
    confirmed_at  TIMESTAMPTZ,
    ready_at      TIMESTAMPTZ,
    served_at     TIMESTAMPTZ,
    paid_at       TIMESTAMPTZ,
    cancelled_at  TIMESTAMPTZ
);
CREATE INDEX idx_orders_table_status ON ordering.orders(table_id, status);
CREATE INDEX idx_orders_created_at   ON ordering.orders(created_at DESC);
CREATE INDEX idx_orders_active       ON ordering.orders(table_id, created_at DESC) WHERE status NOT IN ('PAID', 'CANCELLED');
CREATE UNIQUE INDEX uq_orders_single_active_per_table
    ON ordering.orders(table_id)
    WHERE status IN ('CREATED', 'CONFIRMED', 'PREPARING', 'READY', 'SERVED');

CREATE TABLE ordering.order_revisions (
    id               BIGINT               GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id         BIGINT               NOT NULL REFERENCES ordering.orders(id) ON DELETE CASCADE,
    revision_number  INT                  NOT NULL CHECK (revision_number > 0),
    revision_source  revision_source_enum NOT NULL,
    created_by_staff BIGINT               REFERENCES identity.staff(id) ON DELETE SET NULL,
    qr_session_id    VARCHAR(64)          REFERENCES table_mgmt.qr_sessions(session_id) ON DELETE SET NULL,
    created_at       TIMESTAMPTZ          NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_order_revision_source CHECK (
        (revision_source = 'CUSTOMER_QR' AND qr_session_id IS NOT NULL)
        OR
        (revision_source = 'STAFF' AND created_by_staff IS NOT NULL)
    ),
    CONSTRAINT uq_order_revision UNIQUE (order_id, revision_number)
);

CREATE TABLE ordering.order_items (
    id                  BIGINT                 GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    revision_id         BIGINT                 NOT NULL REFERENCES ordering.order_revisions(id) ON DELETE CASCADE,
    menu_item_id        BIGINT                 REFERENCES menu.menu_items(id) ON DELETE RESTRICT,
    quantity            INT                    NOT NULL CHECK (quantity > 0),
    unit_price          NUMERIC(12,2)          NOT NULL CHECK (unit_price >= 0),
    subtotal            NUMERIC(12,2)          GENERATED ALWAYS AS (quantity * unit_price) STORED,
    note                TEXT,
    status              order_item_status_enum NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMPTZ            NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_order_items_revision  ON ordering.order_items(revision_id);
CREATE INDEX idx_order_items_menu_item ON ordering.order_items(menu_item_id);


CREATE TABLE kitchen.kitchen_tasks (
    id              BIGINT                   GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_item_id   BIGINT                   NOT NULL UNIQUE REFERENCES ordering.order_items(id) ON DELETE CASCADE,
    status          kitchen_task_status_enum NOT NULL DEFAULT 'CREATED',
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    actual_cook_seconds INT GENERATED ALWAYS AS (
        CASE
            WHEN started_at IS NOT NULL AND completed_at IS NOT NULL
            THEN EXTRACT(EPOCH FROM (completed_at - started_at))::INT
            ELSE NULL
        END
    ) STORED
);
CREATE INDEX idx_kitchen_tasks_active ON kitchen.kitchen_tasks(status) WHERE status IN ('CREATED', 'COOKING');

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
    created_at               TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_kitchen_batches_active ON kitchen.kitchen_batches(status) WHERE status NOT IN ('DONE');

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
    actual_minutes   INT         CHECK (actual_minutes > 0),
    saving_minutes   INT GENERATED ALWAYS AS (
        CASE
            WHEN actual_minutes IS NOT NULL THEN baseline_minutes - actual_minutes
            ELSE NULL
        END
    ) STORED,
    recorded_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_batch_perf_batch ON kitchen.batch_performance(batch_id);

CREATE TABLE payment.payments (
    id              BIGINT                 GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id        BIGINT                 NOT NULL REFERENCES ordering.orders(id) ON DELETE RESTRICT,
    amount          NUMERIC(12,2)          NOT NULL CHECK (amount > 0),
    payment_method  payment_method_enum    NOT NULL,
    provider        payment_provider_enum  NOT NULL,
    status          payment_status_enum    NOT NULL DEFAULT 'PENDING',
    CONSTRAINT ck_payment_method_provider CHECK (
        (payment_method = 'QR_CODE'      AND provider IN ('MOMO', 'VNPAY', 'VIETQR')) OR
        (payment_method IN ('MOMO_ATM', 'MOMO_CREDIT') AND provider = 'MOMO') OR
        (payment_method = 'VNPAY_ATM'    AND provider = 'VNPAY') OR
        (payment_method = 'CASH'         AND provider = 'CASH')
    ),
    idempotency_key          VARCHAR(200)  NOT NULL UNIQUE,
    provider_transaction_id  VARCHAR(255),
    provider_response_code   VARCHAR(20),
    vnp_tmn_code             VARCHAR(20),
    vnp_bank_tran_no         VARCHAR(255),
    vnp_transaction_status   VARCHAR(10),
    vnp_pay_date             VARCHAR(14),
    bank_code                VARCHAR(50),
    card_type                VARCHAR(20),
    momo_order_id            VARCHAR(200),
    momo_request_id          VARCHAR(200),
    momo_pay_url             TEXT,
    momo_deeplink            TEXT,
    momo_qr_code_url         TEXT,
    qr_content               TEXT,
    qr_expires_at            TIMESTAMPTZ,
    provider_payload         JSONB,
    cashier_id               BIGINT REFERENCES identity.staff(id) ON DELETE SET NULL,
    created_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    paid_at      TIMESTAMPTZ,
    failed_at    TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_payment_order_success ON payment.payments(order_id) WHERE status = 'SUCCESS';
CREATE INDEX idx_payments_order        ON payment.payments(order_id);
CREATE INDEX idx_payments_status       ON payment.payments(status);
CREATE INDEX idx_payments_idempotency  ON payment.payments(idempotency_key);
CREATE INDEX idx_payments_provider_txn ON payment.payments(provider_transaction_id) WHERE provider_transaction_id IS NOT NULL;
CREATE INDEX idx_payments_created_at   ON payment.payments(created_at DESC);

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
CREATE INDEX idx_webhooks_payment     ON payment.payment_webhooks(payment_id) WHERE payment_id IS NOT NULL;
CREATE INDEX idx_webhooks_status      ON payment.payment_webhooks(provider, status);
CREATE INDEX idx_webhooks_received    ON payment.payment_webhooks(received_at DESC);
CREATE INDEX idx_webhooks_payload_gin ON payment.payment_webhooks USING GIN (raw_payload);

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
CREATE INDEX idx_payment_txn_payment ON payment.payment_transactions(payment_id);
CREATE INDEX idx_payment_txn_created ON payment.payment_transactions(created_at DESC);

CREATE TABLE payment.refunds (
    id                  BIGINT             GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payment_id          BIGINT             NOT NULL REFERENCES payment.payments(id) ON DELETE RESTRICT,
    amount              NUMERIC(12,2)      NOT NULL CHECK (amount > 0),
    reason              TEXT,
    status              refund_status_enum NOT NULL DEFAULT 'INITIATED',
    idempotency_key     VARCHAR(200)       NOT NULL UNIQUE,
    provider_refund_id  VARCHAR(255),
    requested_by        BIGINT             REFERENCES identity.staff(id) ON DELETE SET NULL,
    raw_request         JSONB,
    raw_response        JSONB,
    created_at          TIMESTAMPTZ        NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ
);
CREATE INDEX idx_refunds_payment ON payment.refunds(payment_id);
CREATE INDEX idx_refunds_status  ON payment.refunds(status);

CREATE TABLE analytics.order_events (
    id            BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id      BIGINT        NOT NULL REFERENCES ordering.orders(id) ON DELETE CASCADE,
    event_type    VARCHAR(100)  NOT NULL,
    event_source  VARCHAR(50),
    actor_id      BIGINT,
    actor_type    VARCHAR(50),
    metadata      JSONB,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_order_events_order        ON analytics.order_events(order_id);
CREATE INDEX idx_order_events_type         ON analytics.order_events(event_type);
CREATE INDEX idx_order_events_created_at   ON analytics.order_events(created_at DESC);
CREATE INDEX idx_order_events_metadata_gin ON analytics.order_events USING GIN (metadata);
