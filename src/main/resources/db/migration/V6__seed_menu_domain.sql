-- =====================================================
-- V6: Seed menu domain — categories, items, combos,
--     inventory ingredients, and item recipes.
-- =====================================================

-- ─────────────────────────────────────────────
-- 1. MENU CATEGORIES
-- ─────────────────────────────────────────────
INSERT INTO menu.menu_categories (name, description, display_order)
SELECT v.name, v.description, v.display_order
FROM (VALUES
    ('Khai Vị & Salad',          'Danh muc Khai Vị & Salad',          1),
    ('Súp',                       'Danh muc Súp',                       2),
    ('Món Chính - Bò & Cừu',      'Danh muc Món Chính - Bò & Cừu',      3),
    ('Món Chính - Gia Cầm & Heo', 'Danh muc Món Chính - Gia Cầm & Heo', 4),
    ('Món Chính - Hải Sản',       'Danh muc Món Chính - Hải Sản',       5),
    ('Mì Ý & Cơm Ý',              'Danh muc Mì Ý & Cơm Ý',              6),
    ('Món Ăn Kèm & Bánh Mì',      'Danh muc Món Ăn Kèm & Bánh Mì',      7),
    ('Tráng Miệng',               'Danh muc Tráng Miệng',               8),
    ('Thức Uống & Rượu Vang',     'Danh muc Thức Uống & Rượu Vang',     9),
    ('Combo & Set',               'Danh mục các món Combo và Set',       10)
) AS v(name, description, display_order)
WHERE NOT EXISTS (
    SELECT 1 FROM menu.menu_categories c
    WHERE c.name = v.name AND c.deleted_at IS NULL
);

-- ─────────────────────────────────────────────
-- 2. SINGLE MENU ITEMS
-- ─────────────────────────────────────────────
INSERT INTO menu.menu_items (
    category_id, name, description, price, cook_time,
    image_url, image_public_id, is_available
)
SELECT c.id, v.item_name, v.item_description, v.price, v.cook_time,
       v.image_url, v.image_public_id, v.is_available
FROM (VALUES
    -- Khai Vị & Salad
    ('Khai Vị & Salad', 'Cua Xanh Cà Mau & Hàu Tươi',
     'Thịt cua xanh, hàu tươi, bơ nghiền, thạch cua, ngò rí và kem cà chua chua cay thanh mát.',
     550000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782422/lumiere/menu-items/rlu6yzybbuwrutnl6aj8.jpg',
     'lumiere/menu-items/rlu6yzybbuwrutnl6aj8', TRUE),
    ('Khai Vị & Salad', 'Gan Ngỗng Áp Chảo',
     'Gan ngỗng áp chảo xém mặt béo ngậy, dùng kèm bọt chuối, rau củ theo mùa và sốt rượu Port.',
     550000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782423/lumiere/menu-items/bg9jkesb9mcxms5fikzj.jpg',
     'lumiere/menu-items/bg9jkesb9mcxms5fikzj', TRUE),
    ('Khai Vị & Salad', 'Sò Điệp Hấp & Quenelle',
     'Sò điệp hấp và sò điệp viên ''quenelle'', sốt pesto hạt phỉ rau tên lửa và bọt nghêu.',
     495000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782424/lumiere/menu-items/ltik87mzo9hunxqqcloz.jpg',
     'lumiere/menu-items/ltik87mzo9hunxqqcloz', TRUE),
    ('Khai Vị & Salad', 'Ốc Pháp Nướng',
     'Ốc Pháp nướng trong lò với bơ tỏi và rau ngò tây thơm lừng.',
     220000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782425/lumiere/menu-items/wahwmulp6ufntl0603ms.jpg',
     'lumiere/menu-items/wahwmulp6ufntl0603ms', TRUE),
    ('Khai Vị & Salad', 'Thịt Bò Carpaccio',
     'Thịt bò sống thái lát mỏng, dùng kèm phô mai Parmesan và rau Rocket.',
     250000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782426/lumiere/menu-items/ahescnlrs1ejkmasllec.jpg',
     'lumiere/menu-items/ahescnlrs1ejkmasllec', TRUE),
    ('Khai Vị & Salad', 'Khay Thịt Nguội & Phô Mai Tổng Hợp',
     'Sự kết hợp hoàn hảo giữa các loại thịt nguội thủ công và phô mai nhập khẩu do Bếp trưởng tuyển chọn.',
     490000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782427/lumiere/menu-items/xrhocyf6mtcgupv0ezmb.jpg',
     'lumiere/menu-items/xrhocyf6mtcgupv0ezmb', TRUE),
    ('Khai Vị & Salad', 'Xà Lách Burrata',
     'Phô mai Burrata tươi dùng kèm cà chua bi, đào, thịt nguội và sốt giấm balsamic.',
     390000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782428/lumiere/menu-items/lmvdbk1ablbekaoi7yw7.jpg',
     'lumiere/menu-items/lmvdbk1ablbekaoi7yw7', TRUE),

    -- Súp
    ('Súp', 'Súp Nấm Truffle',
     'Súp nấm Truffle cao cấp nấu theo công thức huyền thoại của Paul Bocuse.',
     395000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782429/lumiere/menu-items/tpcg9ltjatypxw0nzv6n.jpg',
     'lumiere/menu-items/tpcg9ltjatypxw0nzv6n', TRUE),
    ('Súp', 'Súp Hành Tây Phô Mai Đút Lò',
     'Súp hành tây truyền thống nước dùng bò ngọt thanh, phủ phô mai Emmental và bánh mì nướng giòn.',
     270000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782430/lumiere/menu-items/eb53vxvxph5sbkmz4cgc.jpg',
     'lumiere/menu-items/eb53vxvxph5sbkmz4cgc', TRUE),
    ('Súp', 'Súp Tôm Hùm & Tôm Sú',
     'Súp hải sản đậm đà chiết xuất từ vỏ tôm hùm và tôm sú tươi.',
     290000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782431/lumiere/menu-items/rzxxsukkdrezrybxenug.jpg',
     'lumiere/menu-items/rzxxsukkdrezrybxenug', TRUE),

    -- Bò & Cừu
    ('Món Chính - Bò & Cừu', 'Thăn Bò Tartar',
     'Thăn bò sống Black Angus xay nhuyễn kiểu Pháp, trộn gia vị, dùng kèm khoai tây chiên và xà lách tươi.',
     550000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782431/lumiere/menu-items/lndb1ble4tznmmg4mnsc.jpg',
     'lumiere/menu-items/lndb1ble4tznmmg4mnsc', TRUE),
    ('Món Chính - Bò & Cừu', 'Thăn Bò Bít Tết (Steak Frites)',
     'Thăn bò nướng vừa chín tới, dùng kèm sốt bơ Charmélcia đặc biệt, khoai tây chiên giòn và xà lách.',
     650000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782432/lumiere/menu-items/whs8mikqfvxlobbpctiw.jpg',
     'lumiere/menu-items/whs8mikqfvxlobbpctiw', TRUE),
    ('Món Chính - Bò & Cừu', 'Bò Wellington',
     'Lõi thăn bò cuộn nấm và bánh ngàn lớp đút lò, dùng kèm khoai tây nghiền nấm truffle, măng tây và sốt Périgourdine.',
     950000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782433/lumiere/menu-items/yiqht0hi66uamul6ltin.jpg',
     'lumiere/menu-items/yiqht0hi66uamul6ltin', TRUE),
    ('Món Chính - Bò & Cừu', 'Sườn Cừu New Zealand Nướng',
     'Sườn cừu nướng tảng, dùng kèm khoai tây gnocchi, mứt cừu và sốt vang đỏ.',
     780000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782434/lumiere/menu-items/njbq35s85gulrwixxkgd.jpg',
     'lumiere/menu-items/njbq35s85gulrwixxkgd', TRUE),
    ('Món Chính - Bò & Cừu', 'Sườn Bò Nướng Côte de Boeuf (Cho 2 người)',
     '800g thịt sườn bò nướng cháy cạnh, dùng kèm sốt Charmélcia, khoai tây chiên và xà lách tươi.',
     1850000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782435/lumiere/menu-items/plvptqumz60amtnondbn.jpg',
     'lumiere/menu-items/plvptqumz60amtnondbn', TRUE),
    ('Món Chính - Bò & Cừu', 'Burger Bò The Bistro',
     'Bánh mì brioche mềm, 125g thịt bò băm tươi nướng, xà lách, cà chua, sốt mayonnaise Charmélcia, kèm khoai tây chiên.',
     350000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782435/lumiere/menu-items/ozmd38gapz8uqdwdemzc.jpg',
     'lumiere/menu-items/ozmd38gapz8uqdwdemzc', TRUE),

    -- Gia Cầm & Heo
    ('Món Chính - Gia Cầm & Heo', 'Ức Vịt Áp Chảo Sốt Cam',
     'Ức vịt ủ khô áp chảo da giòn, thịt hồng đào mềm mọng, dùng kèm khoai tây chiên và sốt cam chua ngọt.',
     480000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782436/lumiere/menu-items/l0maekay9jemlfbdy6as.jpg',
     'lumiere/menu-items/l0maekay9jemlfbdy6as', TRUE),
    ('Món Chính - Gia Cầm & Heo', 'Đùi Gà Rút Xương Nướng Chậm',
     'Đùi gà nướng chậm mềm tan, dùng kèm khoai tây bi xào bơ tỏi.',
     280000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782437/lumiere/menu-items/muaef1fc1vdgwhhome2a.jpg',
     'lumiere/menu-items/muaef1fc1vdgwhhome2a', TRUE),
    ('Món Chính - Gia Cầm & Heo', 'Đùi Vịt Hầm Confit',
     'Đùi vịt Pháp hầm ngập mỡ truyền thống, dùng kèm táo hương thảo và khoai tây nghiền.',
     420000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782438/lumiere/menu-items/pmnjyguhc1rzcqfeycub.jpg',
     'lumiere/menu-items/pmnjyguhc1rzcqfeycub', TRUE),
    ('Món Chính - Gia Cầm & Heo', 'Gà Cordon Bleu',
     'Ức gà cuộn dăm bông và phô mai tẩm bột chiên giòn, dùng kèm xà lách xanh.',
     320000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782438/lumiere/menu-items/fgsayoxvcmeqke9uq0kk.jpg',
     'lumiere/menu-items/fgsayoxvcmeqke9uq0kk', TRUE),

    -- Hải Sản
    ('Món Chính - Hải Sản', 'Cá Hồi Áp Chảo',
     'Phi lê cá hồi áp chảo dùng kèm xà lách tươi, khoai tây nghiền vị mù tạt xanh và sốt Dashi.',
     490000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782439/lumiere/menu-items/pswcmstavk5un95ilhz2.jpg',
     'lumiere/menu-items/pswcmstavk5un95ilhz2', TRUE),
    ('Món Chính - Hải Sản', 'Cá Tuyết Patagonia Nóng Lạnh',
     'Cá tuyết dùng hai dải nhiệt nóng-lạnh, ăn kèm thảo mộc tươi và sốt bơ trắng.',
     650000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782440/lumiere/menu-items/qgypikggnb4hum2ewb1y.jpg',
     'lumiere/menu-items/qgypikggnb4hum2ewb1y', TRUE),
    ('Món Chính - Hải Sản', 'Tôm Hùm Xanh',
     'Tôm hùm xanh cao cấp dùng kèm bắp xông khói nghiền và sốt hắc mai biển chua nhẹ.',
     1450000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782441/lumiere/menu-items/dgkpipmgz1h2ikrow0a8.jpg',
     'lumiere/menu-items/dgkpipmgz1h2ikrow0a8', TRUE),
    ('Món Chính - Hải Sản', 'Cá Chẽm Sốt Mù Tạt',
     'Phi lê cá chẽm nướng dùng kèm bông cải xanh baby và sốt kem mù tạt vàng Velouté.',
     350000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782441/lumiere/menu-items/cbhrcccnscgjoiutogzv.jpg',
     'lumiere/menu-items/cbhrcccnscgjoiutogzv', TRUE),

    -- Mì Ý & Cơm Ý
    ('Mì Ý & Cơm Ý', 'Spaghetti Nghêu Vongole',
     'Mì Ý xào nghêu tươi, rượu vang trắng, tỏi và cà chua bi.',
     270000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782442/lumiere/menu-items/ux7jogzhumhdqc72xksm.jpg',
     'lumiere/menu-items/ux7jogzhumhdqc72xksm', TRUE),
    ('Mì Ý & Cơm Ý', 'Lasagna Bò & Cà Chua',
     'Mì Ý đút lò nhiều lớp với sốt thịt bò băm, cà chua tươi và phô mai đun chảy.',
     350000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782443/lumiere/menu-items/tqyojprmqldofjtnzfor.jpg',
     'lumiere/menu-items/tqyojprmqldofjtnzfor', TRUE),
    ('Mì Ý & Cơm Ý', 'Fettuccine Phô Mai Dê',
     'Mì dẹt tự làm trộn sốt phô mai dê béo ngậy, ớt chuông nướng và hạt óc chó.',
     280000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782444/lumiere/menu-items/qjirayxhinldnhtyx14g.jpg',
     'lumiere/menu-items/qjirayxhinldnhtyx14g', TRUE),
    ('Mì Ý & Cơm Ý', 'Risotto Hải Sản',
     'Cơm Ý nấu chậm với tôm, mực, nghêu và cồi sò điệp.',
     455000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782445/lumiere/menu-items/zqfs5qa2lemawe3nxdfd.jpg',
     'lumiere/menu-items/zqfs5qa2lemawe3nxdfd', TRUE),

    -- Món Ăn Kèm & Bánh Mì
    ('Món Ăn Kèm & Bánh Mì', 'Bánh Mì Croque Monsieur',
     'Bánh mì men chua nướng giòn kẹp dăm bông và phô mai chảy.',
     180000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782445/lumiere/menu-items/wecvefyy82umewzresls.jpg',
     'lumiere/menu-items/wecvefyy82umewzresls', TRUE),
    ('Món Ăn Kèm & Bánh Mì', 'Burger Chay Phô Mai Dê',
     'Bánh mì brioche, cà chua, xà lách, phô mai dê, phô mai cheddar, sốt mayonnaise, kèm khoai tây chiên.',
     290000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782446/lumiere/menu-items/lqnaf7cdf9c8vwiaqpat.jpg',
     'lumiere/menu-items/lqnaf7cdf9c8vwiaqpat', TRUE),
    ('Món Ăn Kèm & Bánh Mì', 'Rau Củ Hầm Ratatouille',
     'Các loại rau củ Địa Trung Hải hầm nhừ với sốt cà chua và dầu olive.',
     120000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782447/lumiere/menu-items/gxz1kynglhn1eh58l4hx.jpg',
     'lumiere/menu-items/gxz1kynglhn1eh58l4hx', TRUE),
    ('Món Ăn Kèm & Bánh Mì', 'Khoai Tây Chiên',
     'Khoai tây thái sợi chiên giòn tự làm.',
     90000.00::NUMERIC(12,2), 15,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782448/lumiere/menu-items/mlkhnwok3gbxfchtqyc4.jpg',
     'lumiere/menu-items/mlkhnwok3gbxfchtqyc4', TRUE),

    -- Tráng Miệng
    ('Tráng Miệng', 'Bánh Crepe Suzette',
     'Bánh crepe đốt rượu trực tiếp tại bàn, dùng kèm sốt cam tươi, rượu Grand Marnier và kem vani.',
     250000.00::NUMERIC(12,2), 5,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782449/lumiere/menu-items/oc2p7n3ighs7efaz2dxl.jpg',
     'lumiere/menu-items/oc2p7n3ighs7efaz2dxl', TRUE),
    ('Tráng Miệng', 'Bánh Profiteroles Nhân Nutella',
     'Bánh su nướng giòn nhân kem, rưới đẫm sốt socola Nutella ấm nóng và kem tươi Chantilly.',
     180000.00::NUMERIC(12,2), 5,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782450/lumiere/menu-items/jhsvyol9q6ey8vo9zdke.jpg',
     'lumiere/menu-items/jhsvyol9q6ey8vo9zdke', TRUE),
    ('Tráng Miệng', 'Xoài Nướng Kiểu Việt',
     'Xoài nướng và salad trái cây nhiệt đới, dùng kèm sốt chanh dây và kem dừa.',
     250000.00::NUMERIC(12,2), 5,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782450/lumiere/menu-items/qrdrjyf97j79lxgvfpmy.jpg',
     'lumiere/menu-items/qrdrjyf97j79lxgvfpmy', TRUE),
    ('Tráng Miệng', 'Bánh Tart Táo Úp Ngược (Tarte Tatin)',
     'Bánh tart táo phủ caramel nướng úp ngược, dùng kèm kem vani hạt.',
     220000.00::NUMERIC(12,2), 5,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782451/lumiere/menu-items/t66hp9hglvmbcrgnukty.jpg',
     'lumiere/menu-items/t66hp9hglvmbcrgnukty', TRUE),
    ('Tráng Miệng', 'Tiramisu Truyền Thống',
     'Bánh Tiramisu làm thủ công với phô mai Mascarpone và cà phê Espresso.',
     180000.00::NUMERIC(12,2), 5,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782452/lumiere/menu-items/rxkangetyxxafycxqgh5.jpg',
     'lumiere/menu-items/rxkangetyxxafycxqgh5', TRUE),

    -- Thức Uống & Rượu Vang
    ('Thức Uống & Rượu Vang', 'Set Cà Phê Gourmand',
     'Một ly cà phê Espresso dùng kèm một đĩa nhỏ gồm 3-4 loại bánh ngọt mini ngẫu nhiên trong ngày.',
     180000.00::NUMERIC(12,2), 3,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782453/lumiere/menu-items/fuif6w8tbgedukn5exfq.jpg',
     'lumiere/menu-items/fuif6w8tbgedukn5exfq', TRUE),
    ('Thức Uống & Rượu Vang', 'Cocktail Espresso Martini',
     'Sự pha trộn mạnh mẽ giữa rượu Vodka Smirnoff Black, Kahlúa, cà phê Espresso và đường nâu.',
     220000.00::NUMERIC(12,2), 3,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782454/lumiere/menu-items/aiqedlawww0auvntvcbb.jpg',
     'lumiere/menu-items/aiqedlawww0auvntvcbb', TRUE),
    ('Thức Uống & Rượu Vang', 'Champagne Beaumont des Crayères',
     'Rượu sâm banh Pháp cao cấp, hương vị trái cây tươi sáng và bọt khí mịn màng (Ly).',
     350000.00::NUMERIC(12,2), 3,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782455/lumiere/menu-items/ic6r2y62yg46a1r5z5xr.jpg',
     'lumiere/menu-items/ic6r2y62yg46a1r5z5xr', TRUE),
    ('Thức Uống & Rượu Vang', 'Trà & Trà Thảo Mộc',
     'Lựa chọn trà Ceylan, trà xanh bạc hà hoặc trà thảo mộc cỏ roi ngựa (Verveine).',
     80000.00::NUMERIC(12,2), 3,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782455/lumiere/menu-items/wew0aazdu8mygcq5c6hz.jpg',
     'lumiere/menu-items/wew0aazdu8mygcq5c6hz', TRUE),
    ('Thức Uống & Rượu Vang', 'Nước Khoáng Thiên Nhiên',
     'Nước khoáng thiên nhiên tinh khiết.',
     50000.00::NUMERIC(12,2), 3,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782456/lumiere/menu-items/lico0ltnbhhglox20ibt.jpg',
     'lumiere/menu-items/lico0ltnbhhglox20ibt', TRUE),
    ('Thức Uống & Rượu Vang', 'Rượu Vang Đỏ Château Merlet (Bordeaux) - Ly',
     'Rượu vang đỏ đậm đà từ vùng Bordeaux nước Pháp, kết hợp tuyệt vời với các món thịt đỏ.',
     200000.00::NUMERIC(12,2), 3,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782458/lumiere/menu-items/ydazvf6bgrvuijk6lcqk.jpg',
     'lumiere/menu-items/ydazvf6bgrvuijk6lcqk', TRUE),
    ('Thức Uống & Rượu Vang', 'Rượu Vang Trắng Sauvignon Blanc - Ly',
     'Vang trắng tươi mát, hương trái cây nhiệt đới, hoàn hảo khi dùng kèm các món hải sản.',
     180000.00::NUMERIC(12,2), 3,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782458/lumiere/menu-items/svefzmsuab5jgshvcskh.jpg',
     'lumiere/menu-items/svefzmsuab5jgshvcskh', TRUE),
    ('Thức Uống & Rượu Vang', 'Bia Tươi Kronenbourg 1664 Blanc',
     'Bia lúa mì cao cấp của Pháp với hương vị cam quýt và mùi thơm gia vị nhẹ nhàng.',
     90000.00::NUMERIC(12,2), 3,
     'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782459/lumiere/menu-items/day4jzzp3qk90onfwhsu.jpg',
     'lumiere/menu-items/day4jzzp3qk90onfwhsu', TRUE)
) AS v(category_name, item_name, item_description, price, cook_time, image_url, image_public_id, is_available)
JOIN menu.menu_categories c ON c.name = v.category_name AND c.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1 FROM menu.menu_items mi
    JOIN menu.menu_categories mc ON mc.id = mi.category_id
    WHERE mi.name = v.item_name AND mc.name = v.category_name AND mi.deleted_at IS NULL
);

-- ─────────────────────────────────────────────
-- 3. COMBO ITEMS
-- ─────────────────────────────────────────────

-- 3.1 FIXED combo: Seafood Duo Combo
INSERT INTO menu.menu_items (
    category_id, name, description, price, cook_time,
    image_url, image_public_id, is_available, item_type, combo_kind
)
SELECT c.id,
    'Seafood Duo Combo', 'Combo cố định gồm 2 món hải sản.',
    900000.00::NUMERIC(12,2), 15, NULL, NULL,
    TRUE, 'COMBO'::menu_item_type_enum, 'FIXED'::combo_kind_enum
FROM menu.menu_categories c
WHERE c.name = 'Combo & Set' AND c.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM menu.menu_items mi WHERE mi.name = 'Seafood Duo Combo' AND mi.deleted_at IS NULL
);

INSERT INTO menu.combo_fixed_components (combo_item_id, component_item_id, quantity)
SELECT combo.id, component.id, 1
FROM menu.menu_items combo
CROSS JOIN menu.menu_items component
WHERE combo.name = 'Seafood Duo Combo' AND combo.deleted_at IS NULL
  AND component.name IN ('Cá Hồi Áp Chảo', 'Cá Chẽm Sốt Mù Tạt') AND component.deleted_at IS NULL
ON CONFLICT (combo_item_id, component_item_id) DO NOTHING;

-- 3.2 PICK combo: Lunch Set (1 main + 1 drink)
INSERT INTO menu.menu_items (
    category_id, name, description, price, cook_time,
    image_url, image_public_id, is_available, item_type, combo_kind
)
SELECT c.id,
    'Lunch Set', 'Combo chọn món: 1 món chính + 1 thức uống.',
    520000.00::NUMERIC(12,2), 15, NULL, NULL,
    TRUE, 'COMBO'::menu_item_type_enum, 'PICK'::combo_kind_enum
FROM menu.menu_categories c
WHERE c.name = 'Combo & Set' AND c.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM menu.menu_items mi WHERE mi.name = 'Lunch Set' AND mi.deleted_at IS NULL
);

INSERT INTO menu.combo_pick_slots (combo_item_id, name, min_select, max_select, display_order)
SELECT combo.id, 'Main', 1, 1, 1
FROM menu.menu_items combo
WHERE combo.name = 'Lunch Set' AND combo.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM menu.combo_pick_slots s WHERE s.combo_item_id = combo.id AND s.name = 'Main'
);
INSERT INTO menu.combo_pick_slots (combo_item_id, name, min_select, max_select, display_order)
SELECT combo.id, 'Drink', 1, 1, 2
FROM menu.menu_items combo
WHERE combo.name = 'Lunch Set' AND combo.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM menu.combo_pick_slots s WHERE s.combo_item_id = combo.id AND s.name = 'Drink'
);

INSERT INTO menu.combo_pick_slot_items (slot_id, menu_item_id)
SELECT s.id, m.id
FROM menu.combo_pick_slots s
JOIN menu.menu_items combo ON s.combo_item_id = combo.id
CROSS JOIN menu.menu_items m
WHERE combo.name = 'Lunch Set' AND combo.deleted_at IS NULL
  AND s.name = 'Main'
  AND m.name IN ('Ức Vịt Áp Chảo Sốt Cam', 'Đùi Gà Rút Xương Nướng Chậm') AND m.deleted_at IS NULL
ON CONFLICT (slot_id, menu_item_id) DO NOTHING;

INSERT INTO menu.combo_pick_slot_items (slot_id, menu_item_id)
SELECT s.id, m.id
FROM menu.combo_pick_slots s
JOIN menu.menu_items combo ON s.combo_item_id = combo.id
CROSS JOIN menu.menu_items m
WHERE combo.name = 'Lunch Set' AND combo.deleted_at IS NULL
  AND s.name = 'Drink'
  AND m.name IN ('Trà & Trà Thảo Mộc', 'Nước Khoáng Thiên Nhiên') AND m.deleted_at IS NULL
ON CONFLICT (slot_id, menu_item_id) DO NOTHING;

-- ─────────────────────────────────────────────
-- 4. INVENTORY INGREDIENTS
-- ─────────────────────────────────────────────
INSERT INTO inventory.ingredients (name, unit, current_qty, low_stock_threshold)
VALUES
    -- Thịt
    ('Thăn bò Black Angus',    'G',    10000, 2000), ('Sườn bò',           'G',    15000, 3000),
    ('Ức vịt',                 'G',     8000, 1500), ('Đùi gà',            'G',    12000, 2500),
    ('Sườn cừu',               'G',     5000, 1000), ('Dăm bông',          'G',     3000,  500),
    ('Gan ngỗng',              'G',     2000,  500), ('Lõi thăn bò',       'G',     8000, 1500),
    ('Thịt bò băm',            'G',    10000, 2000), ('Đùi vịt',           'G',     6000, 1000),
    ('Thịt nguội tổng hợp',    'G',     5000, 1000),
    -- Hải sản
    ('Cua xanh Cà Mau',        'G',     5000, 1000), ('Hàu tươi',          'UNIT',   500,  100),
    ('Sò điệp',                'G',     4000,  800), ('Tôm hùm xanh',      'G',     3000,  500),
    ('Cá hồi',                 'G',    10000, 2000), ('Cá tuyết',          'G',     5000, 1000),
    ('Cá chẽm',                'G',     8000, 1500), ('Nghêu tươi',        'G',    15000, 3000),
    ('Tôm sú',                 'G',    10000, 2000), ('Ốc Pháp',           'UNIT', 1000,  200),
    ('Mực',                    'G',     8000, 1500),
    -- Rau củ & tinh bột
    ('Khoai tây',              'G',    50000,10000), ('Hành tây',          'G',    20000, 4000),
    ('Tỏi',                    'G',     5000, 1000), ('Cà chua',           'G',    30000, 5000),
    ('Bông cải xanh baby',     'G',    10000, 2000), ('Măng tây',          'G',     8000, 1500),
    ('Rau Rocket',             'G',     5000, 1000), ('Ớt chuông',         'G',    10000, 2000),
    ('Nấm truffle',            'G',      500,  100), ('Nấm mỡ',            'G',     8000, 1500),
    ('Bột mì',                 'G',    50000,10000), ('Mì spaghetti',      'G',    20000, 4000),
    ('Lá lasagna',             'G',    10000, 2000), ('Gạo arborio',       'G',    15000, 3000),
    ('Bánh mì brioche',        'UNIT',  200,   40), ('Bánh mì men chua',  'UNIT',  150,   30),
    ('Xà lách xanh',           'G',    10000, 2000), ('Rau củ Địa Trung Hải','G', 10000, 2000),
    ('Bắp ngô',                'G',     5000, 1000), ('Đào tươi',          'G',     5000, 1000),
    ('Hạt óc chó',             'G',     2000,  400), ('Ngò tây (parsley)', 'G',     2000,  400),
    ('Bột panko (breadcrumb)', 'G',     5000, 1000), ('Khoai tây gnocchi', 'G',     8000, 1500),
    ('Fettuccine tươi',        'G',     8000, 1500), ('Cồi sò điệp',      'G',     3000,  500),
    -- Bơ sữa & phô mai
    ('Bơ nhạt',                'G',    15000, 3000), ('Phô mai Parmesan',  'G',     5000, 1000),
    ('Phô mai Emmental',       'G',     4000,  800), ('Phô mai Burrata',   'UNIT',  100,   20),
    ('Phô mai Mascarpone',     'G',     6000, 1000), ('Phô mai dê',        'G',     3000,  500),
    ('Phô mai Cheddar',        'G',     5000, 1000), ('Kem tươi',          'ML',   20000, 4000),
    ('Kem dừa',                'ML',    5000, 1000), ('Trứng gà',          'UNIT',  500,  100),
    ('Sữa tươi',               'ML',   20000, 4000), ('Kem vani',          'ML',    5000, 1000),
    -- Gia vị & khác
    ('Dầu olive',              'ML',   10000, 2000), ('Dầu truffle',       'ML',    1000,  200),
    ('Rượu vang đỏ',           'ML',   50000,10000), ('Rượu vang trắng',   'ML',   30000, 6000),
    ('Rượu Port',              'ML',    5000, 1000), ('Grand Marnier',     'ML',    2000,  400),
    ('Nutella',                'G',     5000, 1000), ('Xoài',              'G',    15000, 3000),
    ('Táo',                    'G',    10000, 2000), ('Chanh dây',         'G',     5000, 1000),
    ('Cam tươi',               'UNIT',  500,  100), ('Cà phê hạt',        'G',    10000, 2000),
    ('Vodka Smirnoff Black',   'ML',    5000, 1000), ('Kahlúa',            'ML',    3000,  500),
    ('Champagne',              'ML',   15000, 3000), ('Bia Kronenbourg 1664 Blanc','ML',30000,6000),
    ('Muối tinh',              'G',    10000, 2000), ('Tiêu đen',          'G',     2000,  400),
    ('Đường nâu',              'G',     5000, 1000), ('Đường trắng',       'G',    10000, 2000),
    ('Trà túi lọc',            'UNIT', 1000,  200), ('Nước khoáng',       'UNIT', 2000,  400),
    ('Giấm balsamic',          'ML',    2000,  400), ('Mù tạt vàng (Dijon)','G',   2000,  400),
    ('Hương thảo (rosemary)',  'G',      500,  100), ('Caramel',           'G',     3000,  500),
    ('Bột cacao',              'G',     2000,  400), ('Socola đen',        'G',     3000,  500),
    ('Bánh quy savoiardi',     'G',     3000,  500), ('Nước cao gà/bò',   'ML',   20000, 4000),
    ('Mỡ vịt',                 'ML',    5000, 1000)
ON CONFLICT (name) WHERE deleted_at IS NULL DO NOTHING;

-- ─────────────────────────────────────────────
-- 5. RECIPES (menu_item_ingredients)
-- Bulk insert via a VALUES list joined to the live
-- menu_items and ingredients tables.
-- ─────────────────────────────────────────────
INSERT INTO menu.menu_item_ingredients (menu_item_id, ingredient_id, quantity)
SELECT mi.id, ing.id, v.qty::NUMERIC(12,2)
FROM (VALUES
    -- Cua Xanh Cà Mau & Hàu Tươi
    ('Cua Xanh Cà Mau & Hàu Tươi', 'Cua xanh Cà Mau', 80),
    ('Cua Xanh Cà Mau & Hàu Tươi', 'Hàu tươi',         2),
    ('Cua Xanh Cà Mau & Hàu Tươi', 'Bơ nhạt',         20),

    -- Gan Ngỗng Áp Chảo
    ('Gan Ngỗng Áp Chảo', 'Gan ngỗng', 100),
    ('Gan Ngỗng Áp Chảo', 'Rượu Port',  30),

    -- Sò Điệp Hấp & Quenelle
    ('Sò Điệp Hấp & Quenelle', 'Sò điệp',   120),
    ('Sò Điệp Hấp & Quenelle', 'Rau Rocket',  30),

    -- Ốc Pháp Nướng
    ('Ốc Pháp Nướng', 'Ốc Pháp',           6),
    ('Ốc Pháp Nướng', 'Bơ nhạt',           30),
    ('Ốc Pháp Nướng', 'Tỏi',               15),
    ('Ốc Pháp Nướng', 'Ngò tây (parsley)',  5),

    -- Thịt Bò Carpaccio
    ('Thịt Bò Carpaccio', 'Thăn bò Black Angus', 120),
    ('Thịt Bò Carpaccio', 'Phô mai Parmesan',     30),
    ('Thịt Bò Carpaccio', 'Rau Rocket',           40),
    ('Thịt Bò Carpaccio', 'Dầu olive',            20),

    -- Khay Thịt Nguội & Phô Mai Tổng Hợp
    ('Khay Thịt Nguội & Phô Mai Tổng Hợp', 'Thịt nguội tổng hợp', 150),
    ('Khay Thịt Nguội & Phô Mai Tổng Hợp', 'Phô mai Parmesan',     50),
    ('Khay Thịt Nguội & Phô Mai Tổng Hợp', 'Phô mai dê',           50),
    ('Khay Thịt Nguội & Phô Mai Tổng Hợp', 'Bánh mì men chua',      2),

    -- Xà Lách Burrata
    ('Xà Lách Burrata', 'Phô mai Burrata',       1),
    ('Xà Lách Burrata', 'Cà chua',              80),
    ('Xà Lách Burrata', 'Đào tươi',             60),
    ('Xà Lách Burrata', 'Thịt nguội tổng hợp',  50),
    ('Xà Lách Burrata', 'Giấm balsamic',        15),

    -- Súp Nấm Truffle
    ('Súp Nấm Truffle', 'Nấm mỡ',    150),
    ('Súp Nấm Truffle', 'Nấm truffle',  5),
    ('Súp Nấm Truffle', 'Kem tươi',    50),

    -- Súp Hành Tây Phô Mai Đút Lò
    ('Súp Hành Tây Phô Mai Đút Lò', 'Hành tây',        200),
    ('Súp Hành Tây Phô Mai Đút Lò', 'Nước cao gà/bò',  300),
    ('Súp Hành Tây Phô Mai Đút Lò', 'Phô mai Emmental',  60),
    ('Súp Hành Tây Phô Mai Đút Lò', 'Bánh mì men chua',   1),
    ('Súp Hành Tây Phô Mai Đút Lò', 'Bơ nhạt',           20),

    -- Súp Tôm Hùm & Tôm Sú
    ('Súp Tôm Hùm & Tôm Sú', 'Tôm hùm xanh', 80),
    ('Súp Tôm Hùm & Tôm Sú', 'Tôm sú',       80),
    ('Súp Tôm Hùm & Tôm Sú', 'Kem tươi',     50),
    ('Súp Tôm Hùm & Tôm Sú', 'Cà chua',      50),

    -- Thăn Bò Tartar
    ('Thăn Bò Tartar', 'Thăn bò Black Angus', 150),
    ('Thăn Bò Tartar', 'Khoai tây',           100),

    -- Thăn Bò Bít Tết (Steak Frites)
    ('Thăn Bò Bít Tết (Steak Frites)', 'Thăn bò Black Angus', 200),
    ('Thăn Bò Bít Tết (Steak Frites)', 'Khoai tây',           150),
    ('Thăn Bò Bít Tết (Steak Frites)', 'Bơ nhạt',              30),

    -- Bò Wellington
    ('Bò Wellington', 'Lõi thăn bò', 250),
    ('Bò Wellington', 'Nấm mỡ',      100),
    ('Bò Wellington', 'Bột mì',       50),
    ('Bò Wellington', 'Bơ nhạt',      40),
    ('Bò Wellington', 'Nấm truffle',   5),
    ('Bò Wellington', 'Măng tây',      80),
    ('Bò Wellington', 'Khoai tây',    120),

    -- Sườn Cừu New Zealand Nướng
    ('Sườn Cừu New Zealand Nướng', 'Sườn cừu',             300),
    ('Sườn Cừu New Zealand Nướng', 'Khoai tây gnocchi',    150),
    ('Sườn Cừu New Zealand Nướng', 'Rượu vang đỏ',          80),
    ('Sườn Cừu New Zealand Nướng', 'Hương thảo (rosemary)',  5),

    -- Sườn Bò Nướng Côte de Boeuf
    ('Sườn Bò Nướng Côte de Boeuf (Cho 2 người)', 'Sườn bò',      800),
    ('Sườn Bò Nướng Côte de Boeuf (Cho 2 người)', 'Khoai tây',    200),
    ('Sườn Bò Nướng Côte de Boeuf (Cho 2 người)', 'Bơ nhạt',       50),
    ('Sườn Bò Nướng Côte de Boeuf (Cho 2 người)', 'Xà lách xanh',  80),

    -- Burger Bò The Bistro
    ('Burger Bò The Bistro', 'Thịt bò băm',    125),
    ('Burger Bò The Bistro', 'Bánh mì brioche',   1),
    ('Burger Bò The Bistro', 'Khoai tây',       100),
    ('Burger Bò The Bistro', 'Cà chua',          30),

    -- Ức Vịt Áp Chảo Sốt Cam
    ('Ức Vịt Áp Chảo Sốt Cam', 'Ức vịt',   200),
    ('Ức Vịt Áp Chảo Sốt Cam', 'Cam tươi',    1),
    ('Ức Vịt Áp Chảo Sốt Cam', 'Khoai tây', 100),

    -- Đùi Gà Rút Xương Nướng Chậm
    ('Đùi Gà Rút Xương Nướng Chậm', 'Đùi gà',   250),
    ('Đùi Gà Rút Xương Nướng Chậm', 'Khoai tây', 150),
    ('Đùi Gà Rút Xương Nướng Chậm', 'Bơ nhạt',    20),
    ('Đùi Gà Rút Xương Nướng Chậm', 'Tỏi',        15),

    -- Đùi Vịt Hầm Confit
    ('Đùi Vịt Hầm Confit', 'Đùi vịt',              250),
    ('Đùi Vịt Hầm Confit', 'Mỡ vịt',               200),
    ('Đùi Vịt Hầm Confit', 'Táo',                    80),
    ('Đùi Vịt Hầm Confit', 'Hương thảo (rosemary)',   5),
    ('Đùi Vịt Hầm Confit', 'Khoai tây',             150),

    -- Gà Cordon Bleu
    ('Gà Cordon Bleu', 'Đùi gà',                200),
    ('Gà Cordon Bleu', 'Dăm bông',               50),
    ('Gà Cordon Bleu', 'Phô mai Emmental',        50),
    ('Gà Cordon Bleu', 'Bột panko (breadcrumb)',  60),
    ('Gà Cordon Bleu', 'Trứng gà',                 1),
    ('Gà Cordon Bleu', 'Xà lách xanh',            60),

    -- Cá Hồi Áp Chảo
    ('Cá Hồi Áp Chảo', 'Cá hồi',    180),
    ('Cá Hồi Áp Chảo', 'Khoai tây', 120),
    ('Cá Hồi Áp Chảo', 'Dầu olive',  15),

    -- Cá Tuyết Patagonia Nóng Lạnh
    ('Cá Tuyết Patagonia Nóng Lạnh', 'Cá tuyết',          200),
    ('Cá Tuyết Patagonia Nóng Lạnh', 'Bơ nhạt',            40),
    ('Cá Tuyết Patagonia Nóng Lạnh', 'Rượu vang trắng',    30),
    ('Cá Tuyết Patagonia Nóng Lạnh', 'Ngò tây (parsley)',   5),

    -- Tôm Hùm Xanh
    ('Tôm Hùm Xanh', 'Tôm hùm xanh', 500),
    ('Tôm Hùm Xanh', 'Bắp ngô',        80),
    ('Tôm Hùm Xanh', 'Bơ nhạt',        30),

    -- Cá Chẽm Sốt Mù Tạt
    ('Cá Chẽm Sốt Mù Tạt', 'Cá chẽm',             200),
    ('Cá Chẽm Sốt Mù Tạt', 'Bông cải xanh baby',   80),
    ('Cá Chẽm Sốt Mù Tạt', 'Mù tạt vàng (Dijon)',  20),
    ('Cá Chẽm Sốt Mù Tạt', 'Kem tươi',             60),

    -- Spaghetti Nghêu Vongole
    ('Spaghetti Nghêu Vongole', 'Mì spaghetti',    120),
    ('Spaghetti Nghêu Vongole', 'Nghêu tươi',      250),
    ('Spaghetti Nghêu Vongole', 'Tỏi',              10),
    ('Spaghetti Nghêu Vongole', 'Rượu vang trắng',  30),

    -- Lasagna Bò & Cà Chua
    ('Lasagna Bò & Cà Chua', 'Lá lasagna',       100),
    ('Lasagna Bò & Cà Chua', 'Thịt bò băm',      150),
    ('Lasagna Bò & Cà Chua', 'Cà chua',          100),
    ('Lasagna Bò & Cà Chua', 'Phô mai Parmesan',  50),
    ('Lasagna Bò & Cà Chua', 'Phô mai Emmental',  50),

    -- Fettuccine Phô Mai Dê
    ('Fettuccine Phô Mai Dê', 'Fettuccine tươi', 120),
    ('Fettuccine Phô Mai Dê', 'Phô mai dê',       80),
    ('Fettuccine Phô Mai Dê', 'Ớt chuông',        60),
    ('Fettuccine Phô Mai Dê', 'Hạt óc chó',       30),
    ('Fettuccine Phô Mai Dê', 'Kem tươi',         50),

    -- Risotto Hải Sản
    ('Risotto Hải Sản', 'Gạo arborio',     100),
    ('Risotto Hải Sản', 'Tôm sú',           80),
    ('Risotto Hải Sản', 'Mực',              60),
    ('Risotto Hải Sản', 'Nghêu tươi',       80),
    ('Risotto Hải Sản', 'Cồi sò điệp',      60),
    ('Risotto Hải Sản', 'Rượu vang trắng',  50),
    ('Risotto Hải Sản', 'Phô mai Parmesan', 30),

    -- Bánh Mì Croque Monsieur
    ('Bánh Mì Croque Monsieur', 'Bánh mì men chua',   2),
    ('Bánh Mì Croque Monsieur', 'Dăm bông',          60),
    ('Bánh Mì Croque Monsieur', 'Phô mai Emmental',   60),
    ('Bánh Mì Croque Monsieur', 'Bơ nhạt',           15),

    -- Burger Chay Phô Mai Dê
    ('Burger Chay Phô Mai Dê', 'Bánh mì brioche',  1),
    ('Burger Chay Phô Mai Dê', 'Phô mai dê',      60),
    ('Burger Chay Phô Mai Dê', 'Phô mai Cheddar',  40),
    ('Burger Chay Phô Mai Dê', 'Cà chua',         40),
    ('Burger Chay Phô Mai Dê', 'Xà lách xanh',    30),
    ('Burger Chay Phô Mai Dê', 'Khoai tây',       100),

    -- Rau Củ Hầm Ratatouille
    ('Rau Củ Hầm Ratatouille', 'Rau củ Địa Trung Hải', 200),
    ('Rau Củ Hầm Ratatouille', 'Cà chua',              100),
    ('Rau Củ Hầm Ratatouille', 'Dầu olive',             20),
    ('Rau Củ Hầm Ratatouille', 'Tỏi',                   10),

    -- Khoai Tây Chiên
    ('Khoai Tây Chiên', 'Khoai tây', 200),
    ('Khoai Tây Chiên', 'Muối tinh',   5),

    -- Bánh Crepe Suzette
    ('Bánh Crepe Suzette', 'Bột mì',       50),
    ('Bánh Crepe Suzette', 'Trứng gà',      2),
    ('Bánh Crepe Suzette', 'Sữa tươi',    100),
    ('Bánh Crepe Suzette', 'Cam tươi',      1),
    ('Bánh Crepe Suzette', 'Grand Marnier', 30),
    ('Bánh Crepe Suzette', 'Kem vani',     60),
    ('Bánh Crepe Suzette', 'Bơ nhạt',     20),

    -- Bánh Profiteroles Nhân Nutella
    ('Bánh Profiteroles Nhân Nutella', 'Bột mì',   60),
    ('Bánh Profiteroles Nhân Nutella', 'Bơ nhạt',  50),
    ('Bánh Profiteroles Nhân Nutella', 'Trứng gà',  3),
    ('Bánh Profiteroles Nhân Nutella', 'Nutella',   60),
    ('Bánh Profiteroles Nhân Nutella', 'Kem tươi',  80),

    -- Xoài Nướng Kiểu Việt
    ('Xoài Nướng Kiểu Việt', 'Xoài',      200),
    ('Xoài Nướng Kiểu Việt', 'Chanh dây',  50),
    ('Xoài Nướng Kiểu Việt', 'Kem dừa',    60),

    -- Bánh Tart Táo Úp Ngược (Tarte Tatin)
    ('Bánh Tart Táo Úp Ngược (Tarte Tatin)', 'Táo',     200),
    ('Bánh Tart Táo Úp Ngược (Tarte Tatin)', 'Caramel',  60),
    ('Bánh Tart Táo Úp Ngược (Tarte Tatin)', 'Bột mì',   80),
    ('Bánh Tart Táo Úp Ngược (Tarte Tatin)', 'Bơ nhạt',  60),
    ('Bánh Tart Táo Úp Ngược (Tarte Tatin)', 'Kem vani',  60),

    -- Tiramisu Truyền Thống
    ('Tiramisu Truyền Thống', 'Phô mai Mascarpone',   80),
    ('Tiramisu Truyền Thống', 'Cà phê hạt',           15),
    ('Tiramisu Truyền Thống', 'Kahlúa',               10),
    ('Tiramisu Truyền Thống', 'Đường nâu',            20),
    ('Tiramisu Truyền Thống', 'Bánh quy savoiardi',   60),
    ('Tiramisu Truyền Thống', 'Trứng gà',              2),
    ('Tiramisu Truyền Thống', 'Bột cacao',            10),

    -- Đồ uống
    ('Set Cà Phê Gourmand',                             'Cà phê hạt',             18),
    ('Cocktail Espresso Martini',                        'Vodka Smirnoff Black',   45),
    ('Cocktail Espresso Martini',                        'Kahlúa',                 15),
    ('Cocktail Espresso Martini',                        'Cà phê hạt',             15),
    ('Champagne Beaumont des Crayères',                  'Champagne',             150),
    ('Trà & Trà Thảo Mộc',                              'Trà túi lọc',             1),
    ('Nước Khoáng Thiên Nhiên',                          'Nước khoáng',             1),
    ('Rượu Vang Đỏ Château Merlet (Bordeaux) - Ly',     'Rượu vang đỏ',          150),
    ('Rượu Vang Trắng Sauvignon Blanc - Ly',             'Rượu vang trắng',       150),
    ('Bia Tươi Kronenbourg 1664 Blanc',                  'Bia Kronenbourg 1664 Blanc', 330)
) AS v(item_name, ing_name, qty)
JOIN menu.menu_items        mi  ON mi.name  = v.item_name AND mi.deleted_at  IS NULL
JOIN inventory.ingredients  ing ON ing.name = v.ing_name  AND ing.deleted_at IS NULL
ON CONFLICT (menu_item_id, ingredient_id) DO NOTHING;
