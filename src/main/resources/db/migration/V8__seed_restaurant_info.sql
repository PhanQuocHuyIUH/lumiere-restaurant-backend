-- =====================================================
-- V8: Seed restaurant identity into system_settings so the
--      invoice receipt can print contact info without any
--      schema change. Idempotent via ON CONFLICT.
-- =====================================================

INSERT INTO system_settings (key, value, updated_at) VALUES
    ('restaurant.name',    'Lumière Restaurant',                                       NOW()),
    ('restaurant.address', '12 Nguyễn Văn Bảo, Phường 4, Quận Gò Vấp, TP.HCM',          NOW()),
    ('restaurant.hotline', '0935004922',                                                NOW())
ON CONFLICT (key) DO UPDATE
    SET value      = EXCLUDED.value,
        updated_at = NOW();
