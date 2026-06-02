-- =====================================================
-- V13: Table grouping (gộp bàn)
-- Allows joining multiple physical tables into 1 logical group for parties
-- too large for a single table. All orders under the group share one bill,
-- billed at the group's master_table.
-- =====================================================

CREATE TYPE table_group_status_enum AS ENUM ('OPEN', 'CLOSED');

CREATE TABLE table_mgmt.table_groups (
    id              BIGINT                  GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    master_table_id BIGINT                  NOT NULL REFERENCES table_mgmt.tables(id),
    status          table_group_status_enum NOT NULL DEFAULT 'OPEN',
    created_at      TIMESTAMPTZ             NOT NULL DEFAULT NOW(),
    closed_at       TIMESTAMPTZ,
    note            TEXT
);

CREATE TABLE table_mgmt.table_group_members (
    group_id   BIGINT      NOT NULL REFERENCES table_mgmt.table_groups(id) ON DELETE CASCADE,
    table_id   BIGINT      NOT NULL REFERENCES table_mgmt.tables(id),
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (group_id, table_id)
);

CREATE INDEX idx_table_groups_status        ON table_mgmt.table_groups(status);
CREATE INDEX idx_table_group_members_table  ON table_mgmt.table_group_members(table_id);

-- Order ↔ group link. Nullable: orders not in a group continue to bill per-table.
ALTER TABLE ordering.orders
    ADD COLUMN table_group_id BIGINT REFERENCES table_mgmt.table_groups(id);

CREATE INDEX idx_orders_table_group ON ordering.orders(table_group_id) WHERE table_group_id IS NOT NULL;
