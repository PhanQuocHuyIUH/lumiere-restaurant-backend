-- ==============================================================================
-- V11: Seed Inventory & Recipes
-- ==============================================================================

-- 1. Thêm nguyên liệu vào kho (inventory.ingredients)
INSERT INTO inventory.ingredients (name, unit, current_qty, low_stock_threshold)
VALUES
    -- Thịt (G)
    ('Thăn bò Black Angus', 'G', 10000, 2000),
    ('Sườn bò', 'G', 15000, 3000),
    ('Ức vịt', 'G', 8000, 1500),
    ('Đùi gà', 'G', 12000, 2500),
    ('Sườn cừu', 'G', 5000, 1000),
    ('Dăm bông', 'G', 3000, 500),
    ('Gan ngỗng', 'G', 2000, 500),
    ('Lõi thăn bò', 'G', 8000, 1500),
    ('Thịt bò băm', 'G', 10000, 2000),
    ('Đùi vịt', 'G', 6000, 1000),
    ('Thịt nguội tổng hợp', 'G', 5000, 1000),
    
    -- Hải sản (G)
    ('Cua xanh Cà Mau', 'G', 5000, 1000),
    ('Hàu tươi', 'UNIT', 500, 100),
    ('Sò điệp', 'G', 4000, 800),
    ('Tôm hùm xanh', 'G', 3000, 500),
    ('Cá hồi', 'G', 10000, 2000),
    ('Cá tuyết', 'G', 5000, 1000),
    ('Cá chẽm', 'G', 8000, 1500),
    ('Nghêu tươi', 'G', 15000, 3000),
    ('Tôm sú', 'G', 10000, 2000),
    ('Ốc Pháp', 'UNIT', 1000, 200),
    ('Mực', 'G', 8000, 1500),
    
    -- Rau củ & Tinh bột (G)
    ('Khoai tây', 'G', 50000, 10000),
    ('Hành tây', 'G', 20000, 4000),
    ('Tỏi', 'G', 5000, 1000),
    ('Cà chua', 'G', 30000, 5000),
    ('Bông cải xanh baby', 'G', 10000, 2000),
    ('Măng tây', 'G', 8000, 1500),
    ('Rau Rocket', 'G', 5000, 1000),
    ('Ớt chuông', 'G', 10000, 2000),
    ('Nấm truffle', 'G', 500, 100),
    ('Nấm mỡ', 'G', 8000, 1500),
    ('Bột mì', 'G', 50000, 10000),
    ('Mì spaghetti', 'G', 20000, 4000),
    ('Lá lasagna', 'G', 10000, 2000),
    ('Gạo arborio', 'G', 15000, 3000),
    ('Bánh mì brioche', 'UNIT', 200, 40),
    ('Bánh mì men chua', 'UNIT', 150, 30),
    ('Xà lách xanh', 'G', 10000, 2000),
    ('Rau củ Địa Trung Hải', 'G', 10000, 2000),
    ('Bắp ngô', 'G', 5000, 1000),
    ('Đào tươi', 'G', 5000, 1000),
    ('Hạt óc chó', 'G', 2000, 400),
    ('Ngò tây (parsley)', 'G', 2000, 400),
    ('Bột panko (breadcrumb)', 'G', 5000, 1000),
    ('Khoai tây gnocchi', 'G', 8000, 1500),
    ('Fettuccine tươi', 'G', 8000, 1500),
    ('Cồi sò điệp', 'G', 3000, 500),
    
    -- Bơ sữa & Phô mai (G/ML)
    ('Bơ nhạt', 'G', 15000, 3000),
    ('Phô mai Parmesan', 'G', 5000, 1000),
    ('Phô mai Emmental', 'G', 4000, 800),
    ('Phô mai Burrata', 'UNIT', 100, 20),
    ('Phô mai Mascarpone', 'G', 6000, 1000),
    ('Phô mai dê', 'G', 3000, 500),
    ('Phô mai Cheddar', 'G', 5000, 1000),
    ('Kem tươi', 'ML', 20000, 4000),
    ('Kem dừa', 'ML', 5000, 1000),
    ('Trứng gà', 'UNIT', 500, 100),
    ('Sữa tươi', 'ML', 20000, 4000),
    ('Kem vani', 'ML', 5000, 1000),
    
    -- Gia vị & Khác (G/ML/UNIT)
    ('Dầu olive', 'ML', 10000, 2000),
    ('Dầu truffle', 'ML', 1000, 200),
    ('Rượu vang đỏ', 'ML', 50000, 10000),
    ('Rượu vang trắng', 'ML', 30000, 6000),
    ('Rượu Port', 'ML', 5000, 1000),
    ('Grand Marnier', 'ML', 2000, 400),
    ('Nutella', 'G', 5000, 1000),
    ('Xoài', 'G', 15000, 3000),
    ('Táo', 'G', 10000, 2000),
    ('Chanh dây', 'G', 5000, 1000),
    ('Cam tươi', 'UNIT', 500, 100),
    ('Cà phê hạt', 'G', 10000, 2000),
    ('Vodka Smirnoff Black', 'ML', 5000, 1000),
    ('Kahlúa', 'ML', 3000, 500),
    ('Champagne', 'ML', 15000, 3000),
    ('Bia Kronenbourg 1664 Blanc', 'ML', 30000, 6000),
    ('Muối tinh', 'G', 10000, 2000),
    ('Tiêu đen', 'G', 2000, 400),
    ('Đường nâu', 'G', 5000, 1000),
    ('Đường trắng', 'G', 10000, 2000),
    ('Trà túi lọc', 'UNIT', 1000, 200),
    ('Nước khoáng', 'UNIT', 2000, 400),
    ('Giấm balsamic', 'ML', 2000, 400),
    ('Mù tạt vàng (Dijon)', 'G', 2000, 400),
    ('Hương thảo (rosemary)', 'G', 500, 100),
    ('Caramel', 'G', 3000, 500),
    ('Bột cacao', 'G', 2000, 400),
    ('Socola đen', 'G', 3000, 500),
    ('Bánh quy savoiardi', 'G', 3000, 500),
    ('Nước cao gà/bò', 'ML', 20000, 4000),
    ('Mỡ vịt', 'ML', 5000, 1000),
    ('Rượu vang trắng Champagne', 'ML', 5000, 1000)
ON CONFLICT (name) WHERE deleted_at IS NULL DO NOTHING;

-- 2. Thêm công thức món ăn (menu.menu_item_ingredients)
DO $$
DECLARE
    v_item_id BIGINT;
    v_ing_id BIGINT;
BEGIN
    -- ============================================================
    -- KHAI VỊ & SALAD
    -- ============================================================

    -- Món: Cua Xanh Cà Mau & Hàu Tươi
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Cua Xanh Cà Mau & Hàu Tươi' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Cua xanh Cà Mau' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 80) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Hàu tươi' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 2) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bơ nhạt' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 20) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Gan Ngỗng Áp Chảo
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Gan Ngỗng Áp Chảo' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Gan ngỗng' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 100) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Rượu Port' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 30) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Sò Điệp Hấp & Quenelle
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Sò Điệp Hấp & Quenelle' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Sò điệp' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 120) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Rau Rocket' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 30) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Ốc Pháp Nướng
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Ốc Pháp Nướng' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Ốc Pháp' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 6) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bơ nhạt' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 30) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Tỏi' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 15) ON CONFLICT DO NOTHING;

        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Ngò tây (parsley)' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 5) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Thịt Bò Carpaccio
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Thịt Bò Carpaccio' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Thăn bò Black Angus' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 120) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Phô mai Parmesan' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 30) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Rau Rocket' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 40) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Dầu olive' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 20) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Khay Thịt Nguội & Phô Mai Tổng Hợp
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Khay Thịt Nguội & Phô Mai Tổng Hợp' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Thịt nguội tổng hợp' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 150) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Phô mai Parmesan' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 50) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Phô mai dê' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 50) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bánh mì men chua' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 2) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Xà Lách Burrata
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Xà Lách Burrata' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Phô mai Burrata' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 1) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Cà chua' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 80) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Đào tươi' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 60) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Thịt nguội tổng hợp' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 50) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Giấm balsamic' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 15) ON CONFLICT DO NOTHING;
    END IF;

    -- ============================================================
    -- SÚP
    -- ============================================================

    -- Món: Súp Nấm Truffle
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Súp Nấm Truffle' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Nấm mỡ' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 150) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Nấm truffle' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 5) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Kem tươi' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 50) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Súp Hành Tây Phô Mai Đút Lò
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Súp Hành Tây Phô Mai Đút Lò' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Hành tây' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 200) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Nước cao gà/bò' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 300) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Phô mai Emmental' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 60) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bánh mì men chua' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 1) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bơ nhạt' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 20) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Súp Tôm Hùm & Tôm Sú
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Súp Tôm Hùm & Tôm Sú' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Tôm hùm xanh' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 80) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Tôm sú' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 80) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Kem tươi' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 50) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Cà chua' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 50) ON CONFLICT DO NOTHING;
    END IF;

    -- ============================================================
    -- MÓN CHÍNH - BÒ & CỪU
    -- ============================================================

    -- Món: Thăn Bò Tartar
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Thăn Bò Tartar' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Thăn bò Black Angus' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 150) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Khoai tây' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 100) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Thăn Bò Bít Tết (Steak Frites)
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Thăn Bò Bít Tết (Steak Frites)' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Thăn bò Black Angus' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 200) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Khoai tây' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 150) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bơ nhạt' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 30) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Bò Wellington
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Bò Wellington' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Lõi thăn bò' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 250) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Nấm mỡ' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 100) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bột mì' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 50) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bơ nhạt' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 40) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Nấm truffle' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 5) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Măng tây' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 80) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Khoai tây' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 120) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Sườn Cừu New Zealand Nướng
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Sườn Cừu New Zealand Nướng' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Sườn cừu' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 300) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Khoai tây gnocchi' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 150) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Rượu vang đỏ' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 80) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Hương thảo (rosemary)' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 5) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Sườn Bò Nướng Côte de Boeuf (Cho 2 người)
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Sườn Bò Nướng Côte de Boeuf (Cho 2 người)' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Sườn bò' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 800) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Khoai tây' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 200) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bơ nhạt' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 50) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Xà lách xanh' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 80) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Burger Bò The Bistro
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Burger Bò The Bistro' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Thịt bò băm' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 125) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bánh mì brioche' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 1) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Khoai tây' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 100) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Cà chua' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 30) ON CONFLICT DO NOTHING;
    END IF;

    -- ============================================================
    -- MÓN CHÍNH - GIA CẦM & HEO
    -- ============================================================

    -- Món: Ức Vịt Áp Chảo Sốt Cam
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Ức Vịt Áp Chảo Sốt Cam' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Ức vịt' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 200) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Cam tươi' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 1) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Khoai tây' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 100) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Đùi Gà Rút Xương Nướng Chậm
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Đùi Gà Rút Xương Nướng Chậm' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Đùi gà' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 250) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Khoai tây' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 150) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bơ nhạt' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 20) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Tỏi' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 15) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Đùi Vịt Hầm Confit
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Đùi Vịt Hầm Confit' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Đùi vịt' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 250) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Mỡ vịt' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 200) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Táo' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 80) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Hương thảo (rosemary)' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 5) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Khoai tây' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 150) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Gà Cordon Bleu
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Gà Cordon Bleu' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Đùi gà' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 200) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Dăm bông' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 50) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Phô mai Emmental' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 50) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bột panko (breadcrumb)' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 60) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Trứng gà' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 1) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Xà lách xanh' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 60) ON CONFLICT DO NOTHING;
    END IF;

    -- ============================================================
    -- MÓN CHÍNH - HẢI SẢN
    -- ============================================================

    -- Món: Cá Hồi Áp Chảo
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Cá Hồi Áp Chảo' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Cá hồi' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 180) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Khoai tây' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 120) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Dầu olive' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 15) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Cá Tuyết Patagonia Nóng Lạnh
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Cá Tuyết Patagonia Nóng Lạnh' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Cá tuyết' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 200) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bơ nhạt' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 40) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Rượu vang trắng' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 30) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Ngò tây (parsley)' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 5) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Tôm Hùm Xanh
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Tôm Hùm Xanh' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Tôm hùm xanh' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 500) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bắp ngô' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 80) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bơ nhạt' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 30) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Cá Chẽm Sốt Mù Tạt
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Cá Chẽm Sốt Mù Tạt' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Cá chẽm' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 200) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bông cải xanh baby' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 80) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Mù tạt vàng (Dijon)' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 20) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Kem tươi' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 60) ON CONFLICT DO NOTHING;
    END IF;

    -- ============================================================
    -- MÌ Ý & CƠM Ý
    -- ============================================================

    -- Món: Spaghetti Nghêu Vongole
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Spaghetti Nghêu Vongole' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Mì spaghetti' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 120) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Nghêu tươi' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 250) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Tỏi' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 10) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Rượu vang trắng' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 30) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Lasagna Bò & Cà Chua
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Lasagna Bò & Cà Chua' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Lá lasagna' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 100) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Thịt bò băm' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 150) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Cà chua' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 100) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Phô mai Parmesan' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 50) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Phô mai Emmental' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 50) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Fettuccine Phô Mai Dê
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Fettuccine Phô Mai Dê' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Fettuccine tươi' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 120) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Phô mai dê' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 80) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Ớt chuông' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 60) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Hạt óc chó' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 30) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Kem tươi' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 50) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Risotto Hải Sản
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Risotto Hải Sản' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Gạo arborio' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 100) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Tôm sú' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 80) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Mực' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 60) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Nghêu tươi' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 80) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Cồi sò điệp' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 60) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Rượu vang trắng' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 50) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Phô mai Parmesan' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 30) ON CONFLICT DO NOTHING;
    END IF;

    -- ============================================================
    -- MÓN ĂN KÈM & BÁNH MÌ
    -- ============================================================

    -- Món: Bánh Mì Croque Monsieur
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Bánh Mì Croque Monsieur' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bánh mì men chua' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 2) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Dăm bông' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 60) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Phô mai Emmental' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 60) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bơ nhạt' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 15) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Burger Chay Phô Mai Dê
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Burger Chay Phô Mai Dê' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bánh mì brioche' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 1) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Phô mai dê' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 60) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Phô mai Cheddar' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 40) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Cà chua' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 40) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Xà lách xanh' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 30) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Khoai tây' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 100) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Rau Củ Hầm Ratatouille
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Rau Củ Hầm Ratatouille' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Rau củ Địa Trung Hải' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 200) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Cà chua' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 100) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Dầu olive' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 20) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Tỏi' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 10) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Khoai Tây Chiên
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Khoai Tây Chiên' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Khoai tây' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 200) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Muối tinh' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 5) ON CONFLICT DO NOTHING;
    END IF;

    -- ============================================================
    -- TRÁNG MIỆNG
    -- ============================================================

    -- Món: Bánh Crepe Suzette
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Bánh Crepe Suzette' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bột mì' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 50) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Trứng gà' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 2) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Sữa tươi' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 100) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Cam tươi' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 1) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Grand Marnier' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 30) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Kem vani' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 60) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bơ nhạt' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 20) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Bánh Profiteroles Nhân Nutella
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Bánh Profiteroles Nhân Nutella' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bột mì' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 60) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bơ nhạt' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 50) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Trứng gà' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 3) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Nutella' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 60) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Kem tươi' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 80) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Xoài Nướng Kiểu Việt
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Xoài Nướng Kiểu Việt' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Xoài' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 200) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Chanh dây' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 50) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Kem dừa' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 60) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Bánh Tart Táo Úp Ngược (Tarte Tatin)
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Bánh Tart Táo Úp Ngược (Tarte Tatin)' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Táo' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 200) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Caramel' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 60) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bột mì' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 80) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bơ nhạt' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 60) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Kem vani' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 60) ON CONFLICT DO NOTHING;
    END IF;

    -- Món: Tiramisu Truyền Thống
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Tiramisu Truyền Thống' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Phô mai Mascarpone' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 80) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Cà phê hạt' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 15) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Kahlúa' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 10) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Đường nâu' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 20) ON CONFLICT DO NOTHING;

        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bánh quy savoiardi' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 60) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Trứng gà' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 2) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bột cacao' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 10) ON CONFLICT DO NOTHING;
    END IF;

    -- ============================================================
    -- THỨC UỐNG & RƯỢU VANG
    -- ============================================================

    -- Thức uống: Set Cà Phê Gourmand
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Set Cà Phê Gourmand' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Cà phê hạt' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 18) ON CONFLICT DO NOTHING;
    END IF;

    -- Thức uống: Cocktail Espresso Martini
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Cocktail Espresso Martini' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Vodka Smirnoff Black' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 45) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Kahlúa' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 15) ON CONFLICT DO NOTHING;
        
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Cà phê hạt' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 15) ON CONFLICT DO NOTHING;
    END IF;

    -- Thức uống: Champagne Beaumont des Crayères
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Champagne Beaumont des Crayères' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Champagne' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 150) ON CONFLICT DO NOTHING;
    END IF;

    -- Thức uống: Trà & Trà Thảo Mộc
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Trà & Trà Thảo Mộc' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Trà túi lọc' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 1) ON CONFLICT DO NOTHING;
    END IF;

    -- Thức uống: Nước Khoáng Thiên Nhiên
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Nước Khoáng Thiên Nhiên' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Nước khoáng' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 1) ON CONFLICT DO NOTHING;
    END IF;

    -- Thức uống: Rượu Vang Đỏ Château Merlet (Bordeaux) - Ly
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Rượu Vang Đỏ Château Merlet (Bordeaux) - Ly' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Rượu vang đỏ' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 150) ON CONFLICT DO NOTHING;
    END IF;

    -- Thức uống: Rượu Vang Trắng Sauvignon Blanc - Ly
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Rượu Vang Trắng Sauvignon Blanc - Ly' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Rượu vang trắng' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 150) ON CONFLICT DO NOTHING;
    END IF;

    -- Thức uống: Bia Tươi Kronenbourg 1664 Blanc
    SELECT id INTO v_item_id FROM menu.menu_items WHERE name = 'Bia Tươi Kronenbourg 1664 Blanc' LIMIT 1;
    IF v_item_id IS NOT NULL THEN
        SELECT id INTO v_ing_id FROM inventory.ingredients WHERE name = 'Bia Kronenbourg 1664 Blanc' LIMIT 1;
        INSERT INTO menu.menu_item_ingredients(menu_item_id, ingredient_id, quantity) VALUES (v_item_id, v_ing_id, 330) ON CONFLICT DO NOTHING;
    END IF;

END $$;