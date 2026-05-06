-- =====================================================
-- V5: Seed 60 physical restaurant tables across 3 floors.
-- Layout: 35 × 2-seat, 15 × 4-seat, 10 × 6-seat.
-- Each floor has 20 tables.
--   Floor 1: 12×2p (1-12)  + 5×4p (13-17) + 3×6p (18-20)
--   Floor 2: 12×2p (1-12)  + 5×4p (13-17) + 3×6p (18-20)
--   Floor 3: 11×2p (1-11)  + 5×4p (12-16) + 4×6p (17-20)
-- =====================================================

INSERT INTO table_mgmt.tables (floor, table_no, capacity, status)
VALUES
    -- Floor 1
    (1,  1, 2, 'AVAILABLE'), (1,  2, 2, 'AVAILABLE'), (1,  3, 2, 'AVAILABLE'),
    (1,  4, 2, 'AVAILABLE'), (1,  5, 2, 'AVAILABLE'), (1,  6, 2, 'AVAILABLE'),
    (1,  7, 2, 'AVAILABLE'), (1,  8, 2, 'AVAILABLE'), (1,  9, 2, 'AVAILABLE'),
    (1, 10, 2, 'AVAILABLE'), (1, 11, 2, 'AVAILABLE'), (1, 12, 2, 'AVAILABLE'),
    (1, 13, 4, 'AVAILABLE'), (1, 14, 4, 'AVAILABLE'), (1, 15, 4, 'AVAILABLE'),
    (1, 16, 4, 'AVAILABLE'), (1, 17, 4, 'AVAILABLE'),
    (1, 18, 6, 'AVAILABLE'), (1, 19, 6, 'AVAILABLE'), (1, 20, 6, 'AVAILABLE'),

    -- Floor 2
    (2,  1, 2, 'AVAILABLE'), (2,  2, 2, 'AVAILABLE'), (2,  3, 2, 'AVAILABLE'),
    (2,  4, 2, 'AVAILABLE'), (2,  5, 2, 'AVAILABLE'), (2,  6, 2, 'AVAILABLE'),
    (2,  7, 2, 'AVAILABLE'), (2,  8, 2, 'AVAILABLE'), (2,  9, 2, 'AVAILABLE'),
    (2, 10, 2, 'AVAILABLE'), (2, 11, 2, 'AVAILABLE'), (2, 12, 2, 'AVAILABLE'),
    (2, 13, 4, 'AVAILABLE'), (2, 14, 4, 'AVAILABLE'), (2, 15, 4, 'AVAILABLE'),
    (2, 16, 4, 'AVAILABLE'), (2, 17, 4, 'AVAILABLE'),
    (2, 18, 6, 'AVAILABLE'), (2, 19, 6, 'AVAILABLE'), (2, 20, 6, 'AVAILABLE'),

    -- Floor 3
    (3,  1, 2, 'AVAILABLE'), (3,  2, 2, 'AVAILABLE'), (3,  3, 2, 'AVAILABLE'),
    (3,  4, 2, 'AVAILABLE'), (3,  5, 2, 'AVAILABLE'), (3,  6, 2, 'AVAILABLE'),
    (3,  7, 2, 'AVAILABLE'), (3,  8, 2, 'AVAILABLE'), (3,  9, 2, 'AVAILABLE'),
    (3, 10, 2, 'AVAILABLE'), (3, 11, 2, 'AVAILABLE'),
    (3, 12, 4, 'AVAILABLE'), (3, 13, 4, 'AVAILABLE'), (3, 14, 4, 'AVAILABLE'),
    (3, 15, 4, 'AVAILABLE'), (3, 16, 4, 'AVAILABLE'),
    (3, 17, 6, 'AVAILABLE'), (3, 18, 6, 'AVAILABLE'), (3, 19, 6, 'AVAILABLE'), (3, 20, 6, 'AVAILABLE')
ON CONFLICT (floor, table_no) DO NOTHING;
