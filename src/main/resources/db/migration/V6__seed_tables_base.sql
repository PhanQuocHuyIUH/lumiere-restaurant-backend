-- Seed base restaurant tables with floor-aware identity.
-- table_code is generated from (floor, table_no): e.g. (1,1) => 1-001.

INSERT INTO table_mgmt.tables (floor, table_no, capacity, status)
VALUES
    (1, 1, 4, 'AVAILABLE'),
    (1, 2, 4, 'AVAILABLE'),
    (1, 3, 4, 'AVAILABLE'),
    (1, 4, 6, 'AVAILABLE'),
    (2, 1, 4, 'AVAILABLE'),
    (2, 2, 4, 'AVAILABLE'),
    (2, 3, 4, 'AVAILABLE'),
    (2, 4, 6, 'AVAILABLE')
ON CONFLICT (floor, table_no) DO NOTHING;