-- Seed menu data for testing.

INSERT INTO menu.menu_categories (name, description, display_order)
SELECT v.name, v.description, v.display_order
FROM (
    VALUES
        ('Khai Vi', 'Mon khai vi nhe', 1),
        ('Mon Chinh', 'Mon chinh cho bua an', 2),
        ('Trang Mieng', 'Mon trang mieng', 3),
        ('Do Uong', 'Do uong lanh va nong', 4)
) AS v(name, description, display_order)
WHERE NOT EXISTS (
    SELECT 1
    FROM menu.menu_categories c
    WHERE c.name = v.name
      AND c.deleted_at IS NULL
);

INSERT INTO menu.menu_items (
    category_id,
    name,
    description,
    price,
    cook_time,
    image_url,
    is_available,
    kitchen_label,
    kitchen_note
)
SELECT
    c.id,
    v.item_name,
    v.item_description,
    v.price,
    v.cook_time,
    v.image_url,
    v.is_available,
    v.kitchen_label,
    v.kitchen_note
FROM (
    VALUES
        ('Khai Vi', 'Goi Cuon Tom Thit', '2 cuon goi cuon tom thit va rau song', 65000::NUMERIC(12,2), 8,  NULL, TRUE,  'COLD', 'Ra mon kem nuoc cham'),
        ('Khai Vi', 'Sup Bi Do',         'Sup bi do kem banh mi gion',           55000::NUMERIC(12,2), 10, NULL, TRUE,  'HOT',  'Khong qua nong'),
        ('Mon Chinh', 'Bo Luc Lac',      'Bo luc lac an kem khoai tay chien',    185000::NUMERIC(12,2), 20, NULL, TRUE, 'HOT',  'Do chinh medium'),
        ('Mon Chinh', 'Ca Hoi Ap Chao',  'Ca hoi ap chao sot bo toi',            205000::NUMERIC(12,2), 18, NULL, TRUE, 'HOT',  'Kiem tra xuong'),
        ('Mon Chinh', 'Mi Y Hai San',    'Mi y sot kem hai san',                 165000::NUMERIC(12,2), 15, NULL, TRUE, 'HOT',  'Giup giu nong truoc khi phuc vu'),
        ('Trang Mieng', 'Tiramisu',      'Banh tiramisu vi cafe',                 75000::NUMERIC(12,2), 0,  NULL, TRUE, 'DESSERT', 'Phuc vu lanh'),
        ('Trang Mieng', 'Panna Cotta',   'Panna cotta vi vani sot dau',           70000::NUMERIC(12,2), 0,  NULL, TRUE, 'DESSERT', 'Trang tri trai cay'),
        ('Do Uong', 'Tra Dao Cam Sa',    'Tra dao cam sa mat lanh',               45000::NUMERIC(12,2), 3,  NULL, TRUE, 'BAR',  'It da neu khach yeu cau'),
        ('Do Uong', 'Espresso',          'Cafe espresso 1 shot',                  40000::NUMERIC(12,2), 2,  NULL, TRUE, 'BAR',  'Phuc vu nong'),
        ('Do Uong', 'Nuoc Suoi',         'Nuoc suoi chai 500ml',                  20000::NUMERIC(12,2), 0,  NULL, TRUE, 'BAR',  'Phuc vu kem da')
) AS v(
    category_name,
    item_name,
    item_description,
    price,
    cook_time,
    image_url,
    is_available,
    kitchen_label,
    kitchen_note
)
JOIN menu.menu_categories c
    ON c.name = v.category_name
   AND c.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM menu.menu_items mi
    JOIN menu.menu_categories mc ON mc.id = mi.category_id
    WHERE mi.name = v.item_name
      AND mc.name = v.category_name
      AND mi.deleted_at IS NULL
);