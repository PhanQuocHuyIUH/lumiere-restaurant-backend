INSERT INTO menu.menu_categories (name, description, display_order)
SELECT v.name, v.description, v.display_order
FROM (
    VALUES
        ('Khai Vị & Salad', 'Danh muc Khai Vị & Salad', 1),
        ('Súp', 'Danh muc Súp', 2),
        ('Món Chính - Bò & Cừu', 'Danh muc Món Chính - Bò & Cừu', 3),
        ('Món Chính - Gia Cầm & Heo', 'Danh muc Món Chính - Gia Cầm & Heo', 4),
        ('Món Chính - Hải Sản', 'Danh muc Món Chính - Hải Sản', 5),
        ('Mì Ý & Cơm Ý', 'Danh muc Mì Ý & Cơm Ý', 6),
        ('Món Ăn Kèm & Bánh Mì', 'Danh muc Món Ăn Kèm & Bánh Mì', 7),
        ('Tráng Miệng', 'Danh muc Tráng Miệng', 8),
        ('Thức Uống & Rượu Vang', 'Danh muc Thức Uống & Rượu Vang', 9),
        ('Combo & Set', 'Danh mục các món Combo và Set', 10)
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
    image_public_id,
    is_available
)
SELECT
    c.id,
    v.item_name,
    v.item_description,
    v.price,
    v.cook_time,
    v.image_url,
    v.image_public_id,
    v.is_available
FROM (
    VALUES
        ('Khai Vị & Salad', 'Cua Xanh Cà Mau & Hàu Tươi', 'Thịt cua xanh, hàu tươi, bơ nghiền, thạch cua, ngò rí và kem cà chua chua cay thanh mát.', 550000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782422/lumiere/menu-items/rlu6yzybbuwrutnl6aj8.jpg', 'lumiere/menu-items/rlu6yzybbuwrutnl6aj8', TRUE),
        ('Khai Vị & Salad', 'Gan Ngỗng Áp Chảo', 'Gan ngỗng áp chảo xém mặt béo ngậy, dùng kèm bọt chuối, rau củ theo mùa và sốt rượu Port.', 550000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782423/lumiere/menu-items/bg9jkesb9mcxms5fikzj.jpg', 'lumiere/menu-items/bg9jkesb9mcxms5fikzj', TRUE),
        ('Khai Vị & Salad', 'Sò Điệp Hấp & Quenelle', 'Sò điệp hấp và sò điệp viên ''quenelle'', sốt pesto hạt phỉ rau tên lửa và bọt nghêu.', 495000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782424/lumiere/menu-items/ltik87mzo9hunxqqcloz.jpg', 'lumiere/menu-items/ltik87mzo9hunxqqcloz', TRUE),
        ('Khai Vị & Salad', 'Ốc Pháp Nướng', 'Ốc Pháp nướng trong lò với bơ tỏi và rau ngò tây thơm lừng.', 220000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782425/lumiere/menu-items/wahwmulp6ufntl0603ms.jpg', 'lumiere/menu-items/wahwmulp6ufntl0603ms', TRUE),
        ('Khai Vị & Salad', 'Thịt Bò Carpaccio', 'Thịt bò sống thái lát mỏng, dùng kèm phô mai Parmesan và rau Rocket.', 250000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782426/lumiere/menu-items/ahescnlrs1ejkmasllec.jpg', 'lumiere/menu-items/ahescnlrs1ejkmasllec', TRUE),
        ('Khai Vị & Salad', 'Khay Thịt Nguội & Phô Mai Tổng Hợp', 'Sự kết hợp hoàn hảo giữa các loại thịt nguội thủ công và phô mai nhập khẩu do Bếp trưởng tuyển chọn.', 490000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782427/lumiere/menu-items/xrhocyf6mtcgupv0ezmb.jpg', 'lumiere/menu-items/xrhocyf6mtcgupv0ezmb', TRUE),
        ('Khai Vị & Salad', 'Xà Lách Burrata', 'Phô mai Burrata tươi dùng kèm cà chua bi, đào, thịt nguội và sốt giấm balsamic.', 390000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782428/lumiere/menu-items/lmvdbk1ablbekaoi7yw7.jpg', 'lumiere/menu-items/lmvdbk1ablbekaoi7yw7', TRUE),
        ('Súp', 'Súp Nấm Truffle', 'Súp nấm Truffle cao cấp nấu theo công thức huyền thoại của Paul Bocuse.', 395000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782429/lumiere/menu-items/tpcg9ltjatypxw0nzv6n.jpg', 'lumiere/menu-items/tpcg9ltjatypxw0nzv6n', TRUE),
        ('Súp', 'Súp Hành Tây Phô Mai Đút Lò', 'Súp hành tây truyền thống nước dùng bò ngọt thanh, phủ phô mai Emmental và bánh mì nướng giòn.', 270000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782430/lumiere/menu-items/eb53vxvxph5sbkmz4cgc.jpg', 'lumiere/menu-items/eb53vxvxph5sbkmz4cgc', TRUE),
        ('Súp', 'Súp Tôm Hùm & Tôm Sú', 'Súp hải sản đậm đà chiết xuất từ vỏ tôm hùm và tôm sú tươi.', 290000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782431/lumiere/menu-items/rzxxsukkdrezrybxenug.jpg', 'lumiere/menu-items/rzxxsukkdrezrybxenug', TRUE),
        ('Món Chính - Bò & Cừu', 'Thăn Bò Tartar', 'Thăn bò sống Black Angus xay nhuyễn kiểu Pháp, trộn gia vị, dùng kèm khoai tây chiên và xà lách tươi.', 550000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782431/lumiere/menu-items/lndb1ble4tznmmg4mnsc.jpg', 'lumiere/menu-items/lndb1ble4tznmmg4mnsc', TRUE),
        ('Món Chính - Bò & Cừu', 'Thăn Bò Bít Tết (Steak Frites)', 'Thăn bò nướng vừa chín tới, dùng kèm sốt bơ Charmélcia đặc biệt, khoai tây chiên giòn và xà lách.', 650000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782432/lumiere/menu-items/whs8mikqfvxlobbpctiw.jpg', 'lumiere/menu-items/whs8mikqfvxlobbpctiw', TRUE),
        ('Món Chính - Bò & Cừu', 'Bò Wellington', 'Lõi thăn bò cuộn nấm và bánh ngàn lớp đút lò, dùng kèm khoai tây nghiền nấm truffle, măng tây và sốt Périgourdine.', 950000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782433/lumiere/menu-items/yiqht0hi66uamul6ltin.jpg', 'lumiere/menu-items/yiqht0hi66uamul6ltin', TRUE),
        ('Món Chính - Bò & Cừu', 'Sườn Cừu New Zealand Nướng', 'Sườn cừu nướng tảng, dùng kèm khoai tây gnocchi, mứt cừu và sốt vang đỏ.', 780000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782434/lumiere/menu-items/njbq35s85gulrwixxkgd.jpg', 'lumiere/menu-items/njbq35s85gulrwixxkgd', TRUE),
        ('Món Chính - Bò & Cừu', 'Sườn Bò Nướng Côte de Boeuf (Cho 2 người)', '800g thịt sườn bò nướng cháy cạnh, dùng kèm sốt Charmélcia, khoai tây chiên và xà lách tươi.', 1850000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782435/lumiere/menu-items/plvptqumz60amtnondbn.jpg', 'lumiere/menu-items/plvptqumz60amtnondbn', TRUE),
        ('Món Chính - Bò & Cừu', 'Burger Bò The Bistro', 'Bánh mì brioche mềm, 125g thịt bò băm tươi nướng, xà lách, cà chua, sốt mayonnaise Charmélcia, kèm khoai tây chiên.', 350000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782435/lumiere/menu-items/ozmd38gapz8uqdwdemzc.jpg', 'lumiere/menu-items/ozmd38gapz8uqdwdemzc', TRUE),
        ('Món Chính - Gia Cầm & Heo', 'Ức Vịt Áp Chảo Sốt Cam', 'Ức vịt ủ khô áp chảo da giòn, thịt hồng đào mềm mọng, dùng kèm khoai tây chiên và sốt cam chua ngọt.', 480000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782436/lumiere/menu-items/l0maekay9jemlfbdy6as.jpg', 'lumiere/menu-items/l0maekay9jemlfbdy6as', TRUE),
        ('Món Chính - Gia Cầm & Heo', 'Đùi Gà Rút Xương Nướng Chậm', 'Đùi gà nướng chậm mềm tan, dùng kèm khoai tây bi xào bơ tỏi.', 280000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782437/lumiere/menu-items/muaef1fc1vdgwhhome2a.jpg', 'lumiere/menu-items/muaef1fc1vdgwhhome2a', TRUE),
        ('Món Chính - Gia Cầm & Heo', 'Đùi Vịt Hầm Confit', 'Đùi vịt Pháp hầm ngập mỡ truyền thống, dùng kèm táo hương thảo và khoai tây nghiền.', 420000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782438/lumiere/menu-items/pmnjyguhc1rzcqfeycub.jpg', 'lumiere/menu-items/pmnjyguhc1rzcqfeycub', TRUE),
        ('Món Chính - Gia Cầm & Heo', 'Gà Cordon Bleu', 'Ức gà cuộn dăm bông và phô mai tẩm bột chiên giòn, dùng kèm xà lách xanh.', 320000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782438/lumiere/menu-items/fgsayoxvcmeqke9uq0kk.jpg', 'lumiere/menu-items/fgsayoxvcmeqke9uq0kk', TRUE),
        ('Món Chính - Hải Sản', 'Cá Hồi Áp Chảo', 'Phi lê cá hồi áp chảo dùng kèm xà lách tươi, khoai tây nghiền vị mù tạt xanh và sốt Dashi.', 490000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782439/lumiere/menu-items/pswcmstavk5un95ilhz2.jpg', 'lumiere/menu-items/pswcmstavk5un95ilhz2', TRUE),
        ('Món Chính - Hải Sản', 'Cá Tuyết Patagonia Nóng Lạnh', 'Cá tuyết dùng hai dải nhiệt nóng-lạnh, ăn kèm thảo mộc tươi và sốt bơ trắng.', 650000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782440/lumiere/menu-items/qgypikggnb4hum2ewb1y.jpg', 'lumiere/menu-items/qgypikggnb4hum2ewb1y', TRUE),
        ('Món Chính - Hải Sản', 'Tôm Hùm Xanh', 'Tôm hùm xanh cao cấp dùng kèm bắp xông khói nghiền và sốt hắc mai biển chua nhẹ.', 1450000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782441/lumiere/menu-items/dgkpipmgz1h2ikrow0a8.jpg', 'lumiere/menu-items/dgkpipmgz1h2ikrow0a8', TRUE),
        ('Món Chính - Hải Sản', 'Cá Chẽm Sốt Mù Tạt', 'Phi lê cá chẽm nướng dùng kèm bông cải xanh baby và sốt kem mù tạt vàng Velouté.', 350000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782441/lumiere/menu-items/cbhrcccnscgjoiutogzv.jpg', 'lumiere/menu-items/cbhrcccnscgjoiutogzv', TRUE),
        ('Mì Ý & Cơm Ý', 'Spaghetti Nghêu Vongole', 'Mì Ý xào nghêu tươi, rượu vang trắng, tỏi và cà chua bi.', 270000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782442/lumiere/menu-items/ux7jogzhumhdqc72xksm.jpg', 'lumiere/menu-items/ux7jogzhumhdqc72xksm', TRUE),
        ('Mì Ý & Cơm Ý', 'Lasagna Bò & Cà Chua', 'Mì Ý đút lò nhiều lớp với sốt thịt bò băm, cà chua tươi và phô mai đun chảy.', 350000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782443/lumiere/menu-items/tqyojprmqldofjtnzfor.jpg', 'lumiere/menu-items/tqyojprmqldofjtnzfor', TRUE),
        ('Mì Ý & Cơm Ý', 'Fettuccine Phô Mai Dê', 'Mì dẹt tự làm trộn sốt phô mai dê béo ngậy, ớt chuông nướng và hạt óc chó.', 280000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782444/lumiere/menu-items/qjirayxhinldnhtyx14g.jpg', 'lumiere/menu-items/qjirayxhinldnhtyx14g', TRUE),
        ('Mì Ý & Cơm Ý', 'Risotto Hải Sản', 'Cơm Ý nấu chậm với tôm, mực, nghêu và cồi sò điệp.', 455000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782445/lumiere/menu-items/zqfs5qa2lemawe3nxdfd.jpg', 'lumiere/menu-items/zqfs5qa2lemawe3nxdfd', TRUE),
        ('Món Ăn Kèm & Bánh Mì', 'Bánh Mì Croque Monsieur', 'Bánh mì men chua nướng giòn kẹp dăm bông và phô mai chảy.', 180000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782445/lumiere/menu-items/wecvefyy82umewzresls.jpg', 'lumiere/menu-items/wecvefyy82umewzresls', TRUE),
        ('Món Ăn Kèm & Bánh Mì', 'Burger Chay Phô Mai Dê', 'Bánh mì brioche, cà chua, xà lách, phô mai dê, phô mai cheddar, sốt mayonnaise, kèm khoai tây chiên.', 290000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782446/lumiere/menu-items/lqnaf7cdf9c8vwiaqpat.jpg', 'lumiere/menu-items/lqnaf7cdf9c8vwiaqpat', TRUE),
        ('Món Ăn Kèm & Bánh Mì', 'Rau Củ Hầm Ratatouille', 'Các loại rau củ Địa Trung Hải hầm nhừ với sốt cà chua và dầu olive.', 120000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782447/lumiere/menu-items/gxz1kynglhn1eh58l4hx.jpg', 'lumiere/menu-items/gxz1kynglhn1eh58l4hx', TRUE),
        ('Món Ăn Kèm & Bánh Mì', 'Khoai Tây Chiên', 'Khoai tây thái sợi chiên giòn tự làm.', 90000.00::NUMERIC(12,2), 15, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782448/lumiere/menu-items/mlkhnwok3gbxfchtqyc4.jpg', 'lumiere/menu-items/mlkhnwok3gbxfchtqyc4', TRUE),
        ('Tráng Miệng', 'Bánh Crepe Suzette', 'Bánh crepe đốt rượu trực tiếp tại bàn, dùng kèm sốt cam tươi, rượu Grand Marnier và kem vani.', 250000.00::NUMERIC(12,2), 5, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782449/lumiere/menu-items/oc2p7n3ighs7efaz2dxl.jpg', 'lumiere/menu-items/oc2p7n3ighs7efaz2dxl', TRUE),
        ('Tráng Miệng', 'Bánh Profiteroles Nhân Nutella', 'Bánh su nướng giòn nhân kem, rưới đẫm sốt socola Nutella ấm nóng và kem tươi Chantilly.', 180000.00::NUMERIC(12,2), 5, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782450/lumiere/menu-items/jhsvyol9q6ey8vo9zdke.jpg', 'lumiere/menu-items/jhsvyol9q6ey8vo9zdke', TRUE),
        ('Tráng Miệng', 'Xoài Nướng Kiểu Việt', 'Xoài nướng và salad trái cây nhiệt đới, dùng kèm sốt chanh dây và kem dừa.', 250000.00::NUMERIC(12,2), 5, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782450/lumiere/menu-items/qrdrjyf97j79lxgvfpmy.jpg', 'lumiere/menu-items/qrdrjyf97j79lxgvfpmy', TRUE),
        ('Tráng Miệng', 'Bánh Tart Táo Úp Ngược (Tarte Tatin)', 'Bánh tart táo phủ caramel nướng úp ngược, dùng kèm kem vani hạt.', 220000.00::NUMERIC(12,2), 5, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782451/lumiere/menu-items/t66hp9hglvmbcrgnukty.jpg', 'lumiere/menu-items/t66hp9hglvmbcrgnukty', TRUE),
        ('Tráng Miệng', 'Tiramisu Truyền Thống', 'Bánh Tiramisu làm thủ công với phô mai Mascarpone và cà phê Espresso.', 180000.00::NUMERIC(12,2), 5, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782452/lumiere/menu-items/rxkangetyxxafycxqgh5.jpg', 'lumiere/menu-items/rxkangetyxxafycxqgh5', TRUE),
        ('Thức Uống & Rượu Vang', 'Set Cà Phê Gourmand', 'Một ly cà phê Espresso dùng kèm một đĩa nhỏ gồm 3-4 loại bánh ngọt mini ngẫu nhiên trong ngày.', 180000.00::NUMERIC(12,2), 3, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782453/lumiere/menu-items/fuif6w8tbgedukn5exfq.jpg', 'lumiere/menu-items/fuif6w8tbgedukn5exfq', TRUE),
        ('Thức Uống & Rượu Vang', 'Cocktail Espresso Martini', 'Sự pha trộn mạnh mẽ giữa rượu Vodka Smirnoff Black, Kahlúa, cà phê Espresso và đường nâu.', 220000.00::NUMERIC(12,2), 3, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782454/lumiere/menu-items/aiqedlawww0auvntvcbb.jpg', 'lumiere/menu-items/aiqedlawww0auvntvcbb', TRUE),
        ('Thức Uống & Rượu Vang', 'Champagne Beaumont des Crayères', 'Rượu sâm banh Pháp cao cấp, hương vị trái cây tươi sáng và bọt khí mịn màng (Ly).', 350000.00::NUMERIC(12,2), 3, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782455/lumiere/menu-items/ic6r2y62yg46a1r5z5xr.jpg', 'lumiere/menu-items/ic6r2y62yg46a1r5z5xr', TRUE),
        ('Thức Uống & Rượu Vang', 'Trà & Trà Thảo Mộc', 'Lựa chọn trà Ceylan, trà xanh bạc hà hoặc trà thảo mộc cỏ roi ngựa (Verveine).', 80000.00::NUMERIC(12,2), 3, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782455/lumiere/menu-items/wew0aazdu8mygcq5c6hz.jpg', 'lumiere/menu-items/wew0aazdu8mygcq5c6hz', TRUE),
        ('Thức Uống & Rượu Vang', 'Nước Khoáng Thiên Nhiên', 'Nước khoáng thiên nhiên tinh khiết.', 50000.00::NUMERIC(12,2), 3, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782456/lumiere/menu-items/lico0ltnbhhglox20ibt.jpg', 'lumiere/menu-items/lico0ltnbhhglox20ibt', TRUE),
        ('Thức Uống & Rượu Vang', 'Rượu Vang Đỏ Château Merlet (Bordeaux) - Ly', 'Rượu vang đỏ đậm đà từ vùng Bordeaux nước Pháp, kết hợp tuyệt vời với các món thịt đỏ.', 200000.00::NUMERIC(12,2), 3, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782458/lumiere/menu-items/ydazvf6bgrvuijk6lcqk.jpg', 'lumiere/menu-items/ydazvf6bgrvuijk6lcqk', TRUE),
        ('Thức Uống & Rượu Vang', 'Rượu Vang Trắng Sauvignon Blanc - Ly', 'Vang trắng tươi mát, hương trái cây nhiệt đới, hoàn hảo khi dùng kèm các món hải sản.', 180000.00::NUMERIC(12,2), 3, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782458/lumiere/menu-items/svefzmsuab5jgshvcskh.jpg', 'lumiere/menu-items/svefzmsuab5jgshvcskh', TRUE),
        ('Thức Uống & Rượu Vang', 'Bia Tươi Kronenbourg 1664 Blanc', 'Bia lúa mì cao cấp của Pháp với hương vị cam quýt và mùi thơm gia vị nhẹ nhàng.', 90000.00::NUMERIC(12,2), 3, 'https://res.cloudinary.com/de6yeqwsp/image/upload/v1776782459/lumiere/menu-items/day4jzzp3qk90onfwhsu.jpg', 'lumiere/menu-items/day4jzzp3qk90onfwhsu', TRUE)
) AS v(
    category_name,
    item_name,
    item_description,
    price,
    cook_time,
    image_url,
    image_public_id,
    is_available
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

-- ==========================================
-- 3. Insert Combo Items
-- ==========================================

-- ---------------------------------------------------------
-- 3.1) FIXED combo: "Seafood Duo Combo" = (Cá Hồi Áp Chảo x1) + (Cá Chẽm Sốt Mù Tạt x1)
-- ---------------------------------------------------------

-- Bước 1: Tạo món Combo trong bảng menu_items
INSERT INTO menu.menu_items (
    category_id, name, description, price, cook_time, image_url, image_public_id,
    is_available, item_type, combo_kind
)
SELECT
    c.id, 'Seafood Duo Combo', 'Combo cố định gồm 2 món hải sản.', 900000.00::NUMERIC(12,2), 15, NULL, NULL,
    TRUE, 'COMBO'::menu_item_type_enum, 'FIXED'::combo_kind_enum
FROM menu.menu_categories c
WHERE c.name = 'Combo & Set' AND c.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM menu.menu_items mi
    WHERE mi.name = 'Seafood Duo Combo' AND mi.deleted_at IS NULL
);

-- Bước 2: Liên kết các món con vào bảng combo_fixed_components
INSERT INTO menu.combo_fixed_components (combo_item_id, component_item_id, quantity)
SELECT combo.id, component.id, 1
FROM menu.menu_items combo
         CROSS JOIN menu.menu_items component
WHERE combo.name = 'Seafood Duo Combo' AND combo.deleted_at IS NULL
  AND component.name IN ('Cá Hồi Áp Chảo', 'Cá Chẽm Sốt Mù Tạt') AND component.deleted_at IS NULL
    ON CONFLICT (combo_item_id, component_item_id) DO NOTHING;


-- ---------------------------------------------------------
-- 3.2) PICK combo: "Lunch Set" with 2 slots (Main 1-1, Drink 1-1)
-- ---------------------------------------------------------

-- Bước 1: Tạo món Combo Pick trong bảng menu_items
INSERT INTO menu.menu_items (
    category_id, name, description, price, cook_time, image_url, image_public_id,
    is_available, item_type, combo_kind
)
SELECT
    c.id, 'Lunch Set', 'Combo chọn món: 1 món chính + 1 thức uống.', 520000.00::NUMERIC(12,2), 15, NULL, NULL,
    TRUE, 'COMBO'::menu_item_type_enum, 'PICK'::combo_kind_enum
FROM menu.menu_categories c
WHERE c.name = 'Combo & Set' AND c.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM menu.menu_items mi
    WHERE mi.name = 'Lunch Set' AND mi.deleted_at IS NULL
);

-- Bước 2: Tạo các Slot (Main và Drink) cho bảng combo_pick_slots
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

-- Bước 3: Đưa các món tùy chọn vào Slot tương ứng (bảng combo_pick_slot_items)
-- 3.2a) Đưa các món chính vào Slot 'Main'
INSERT INTO menu.combo_pick_slot_items (slot_id, menu_item_id)
SELECT s.id, m.id
FROM menu.combo_pick_slots s
         JOIN menu.menu_items combo ON s.combo_item_id = combo.id
         CROSS JOIN menu.menu_items m
WHERE combo.name = 'Lunch Set' AND combo.deleted_at IS NULL
  AND s.name = 'Main'
  AND m.name IN ('Ức Vịt Áp Chảo Sốt Cam', 'Đùi Gà Rút Xương Nướng Chậm') AND m.deleted_at IS NULL
    ON CONFLICT (slot_id, menu_item_id) DO NOTHING;

-- 3.2b) Đưa các món nước vào Slot 'Drink'
INSERT INTO menu.combo_pick_slot_items (slot_id, menu_item_id)
SELECT s.id, m.id
FROM menu.combo_pick_slots s
         JOIN menu.menu_items combo ON s.combo_item_id = combo.id
         CROSS JOIN menu.menu_items m
WHERE combo.name = 'Lunch Set' AND combo.deleted_at IS NULL
  AND s.name = 'Drink'
  AND m.name IN ('Trà & Trà Thảo Mộc', 'Nước Khoáng Thiên Nhiên') AND m.deleted_at IS NULL
    ON CONFLICT (slot_id, menu_item_id) DO NOTHING;