-- =====================================================
-- V14: Group payment anchor
-- A "group payment" settles every order in a table group with ONE payment
-- record (one bill, one VNPay QR). The anchor Payment carries the group total
-- and is linked to the group via table_group_id; the success cascade then
-- marks every member order PAID and closes the group.
-- Normal per-table payments leave this column NULL.
-- =====================================================

ALTER TABLE payment.payments
    ADD COLUMN table_group_id BIGINT REFERENCES table_mgmt.table_groups(id);

CREATE INDEX idx_payments_table_group ON payment.payments(table_group_id) WHERE table_group_id IS NOT NULL;
