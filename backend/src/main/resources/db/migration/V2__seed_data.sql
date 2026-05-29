-- =========================================================================
-- SEED DATA MIGRATION FOR NEXORA COMMERCE (50 PRODUCTION PRODUCTS)
-- =========================================================================

-- 1. Seed Roles
INSERT INTO roles (id, name) VALUES 
(1, 'ROLE_CUSTOMER'),
(2, 'ROLE_ADMIN')
ON CONFLICT (name) DO NOTHING;

SELECT setval('roles_id_seq', COALESCE((SELECT MAX(id) FROM roles), 1));

-- 2. Seed Users
INSERT INTO users (id, username, password, full_name, email, status, created_at) VALUES 
('c1111111-1111-1111-1111-111111111111', 'customer1', '$2a$10$OJUTzrrUPFtIcFKIG7IeheSSq150BDV7nQoKMqLUPRk7ookBImVza', 'Customer One', 'customer1@shopcart.com', 'ACTIVE', CURRENT_TIMESTAMP),
('a2222222-2222-2222-2222-222222222222', 'admin', '$2a$10$m6Zt0YLbfTwiqlRmUSTefedpvNND.WX0khe0AzIWpopj00xtrcjJ.', 'Administrator', 'admin@shopcart.com', 'ACTIVE', CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

INSERT INTO users_roles (user_id, role_id) VALUES 
('c1111111-1111-1111-1111-111111111111', 1),
('a2222222-2222-2222-2222-222222222222', 2)
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 3. Seed Categories (với image_url, sort_order)
INSERT INTO categories (id, name, slug, parent_id, image_url, sort_order) VALUES
(1, 'Điện thoại', 'dien-thoai', NULL, 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=200', 1),
(2, 'Laptop', 'laptop', NULL, 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=200', 2),
(3, 'Máy tính bảng', 'may-tinh-bang', NULL, 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=200', 3),
(4, 'Tai nghe & Âm thanh', 'tai-nghe-am-thanh', NULL, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=200', 4),
(5, 'Phụ kiện & Linh kiện', 'phu-kien-linh-kien', NULL, 'https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=200', 5)
ON CONFLICT (id) DO NOTHING;

SELECT setval('categories_id_seq', COALESCE((SELECT MAX(id) FROM categories), 1));

-- 4. Seed Brands (với image_url)
INSERT INTO brands (id, name, slug, image_url) VALUES
(1, 'Apple', 'apple', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=100'),
(2, 'Samsung', 'samsung', 'https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=100'),
(3, 'Xiaomi', 'xiaomi', 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=100'),
(4, 'Dell', 'dell', 'https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=100'),
(5, 'Asus', 'asus', 'https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=100'),
(6, 'Sony', 'sony', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=100'),
(7, 'Logitech', 'logitech', 'https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=100'),
(8, 'Keychron', 'keychron', 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=100'),
(9, 'Anker', 'anker', 'https://images.unsplash.com/photo-1620288627223-53302f4e8c74?w=100')
ON CONFLICT (id) DO NOTHING;

SELECT setval('brands_id_seq', COALESCE((SELECT MAX(id) FROM brands), 1));

-- 5. Seed 50 Products
INSERT INTO products (id, name, description, category_id, brand_id, status) VALUES
-- Nhóm 1: Điện thoại (Category 1)
('P001', 'iPhone 15 128GB', 'Smartphone Apple A16, man hinh Super Retina XDR 6.1 inch', 1, 1, 'ACTIVE'),
('P002', 'iPhone 15 Pro Max 256GB', 'Chip A17 Pro quy luc, khung vien Titan sieu ben', 1, 1, 'ACTIVE'),
('P003', 'Samsung Galaxy S24 Ultra', 'Camera ai 200MP, but S-Pen quyen nang, chip Snap 8 Gen 3', 1, 2, 'ACTIVE'),
('P004', 'Samsung Galaxy Z Fold5', 'Man hinh gap doc dao, da nhiem thong minh', 1, 2, 'ACTIVE'),
('P005', 'Xiaomi 14 Ultra', 'Ong kinh Leica sieu khung, sac nhanh 90W', 1, 3, 'ACTIVE'),
('P006', 'Xiaomi Redmi Note 13', 'Smartphone quoc dan, pin trau gia re', 1, 3, 'ACTIVE'),
('P007', 'iPhone 14 Plus 128GB', 'Man hinh lon pin cuc trau, phien ban tieu chuan gia tot', 1, 1, 'ACTIVE'),
('P008', 'Samsung Galaxy A55 5G', 'Thiet ke sang trong, khang nuoc IP67', 1, 2, 'ACTIVE'),
('P009', 'Xiaomi POCO X6 Pro', 'Quai vat cau hinh phan khuc tam trung', 1, 3, 'ACTIVE'),
('P010', 'iPhone 13 128GB', 'Smartphone quoc dan van rat dang mua thoi diem hien tai', 1, 1, 'ACTIVE'),

-- Nhóm 2: Laptop (Category 2)
('P011', 'MacBook Air M3 13 inch', 'Laptop mong nhe chip Apple M3, pin toi uu', 2, 1, 'ACTIVE'),
('P012', 'MacBook Pro M3 Max 16 inch', 'Quai vat hieu nang cho dan design va coder chuyen nghiep', 2, 1, 'ACTIVE'),
('P013', 'Dell XPS 13 Plus', 'Ultrabook cao cap, man hinh vo cuc InfinityEdge', 2, 4, 'ACTIVE'),
('P014', 'Dell Inspiron 16', 'Laptop van phong man hinh lon, hieu nang on dinh', 2, 4, 'ACTIVE'),
('P015', 'Asus ROG Strix G16', 'Laptop gaming thach thuc moi tua game AAA', 2, 5, 'ACTIVE'),
('P016', 'Asus Zenbook 14 OLED', 'Man hinh OLED ruc ro, trong luong sieu nhe', 2, 5, 'ACTIVE'),
('P017', 'Dell Vostro 3430', 'Laptop hoc tap va lam viec sieu ben bỉ', 2, 4, 'ACTIVE'),
('P018', 'Asus TUF Gaming F15', 'Laptop gaming quoc dan, do ben chuan quan doi', 2, 5, 'ACTIVE'),
('P019', 'MacBook Air M2 13 inch', 'Thiet ke moi thoi thuong, muc gia tiep can de dang', 2, 1, 'ACTIVE'),
('P020', 'Dell Alienware m16', 'Co may gaming toi thuong cua nha Dell', 2, 4, 'ACTIVE'),

-- Nhóm 3: Máy tính bảng (Category 3)
('P021', 'iPad Air 11 inch M4', 'May tinh bang phuc vu hoc tap va cong viec', 3, 1, 'ACTIVE'),
('P022', 'iPad Pro 13 inch M4', 'Man hinh Tandem OLED, do mong ky luc', 3, 1, 'ACTIVE'),
('P023', 'Samsung Galaxy Tab S9 Ultra', 'Man hinh AMOLED kiet tac, khang nuoc tien phong', 3, 2, 'ACTIVE'),
('P024', 'Samsung Galaxy Tab A9+', 'May tinh bang gia re cho nhu cau giai tri gia dinh', 3, 2, 'ACTIVE'),
('P025', 'Xiaomi Pad 6', 'Man hinh 144Hz mượt ma, dung luong pin lon', 3, 3, 'ACTIVE'),
('P026', 'iPad Gen 10 64GB', 'iPad gia tot nhat hien tai phu hop cho hoc sinh', 3, 1, 'ACTIVE'),
('P027', 'iPad Mini 6', 'May tinh bang nho gon quy luc trong long ban tay', 3, 1, 'ACTIVE'),
('P028', 'Samsung Galaxy Tab S9 FE', 'Phien ban fan edition ho tro but S-Pen di kem', 3, 2, 'ACTIVE'),
('P029', 'Xiaomi Pad 6 Pro', 'Hieu nang manh me hon, camera sac net', 3, 3, 'ACTIVE'),
('P030', 'iPad Air 5 M1', 'Chip M1 van rat manh me cho moi tac vu', 3, 1, 'ACTIVE'),

-- Nhóm 4: Tai nghe & Âm thanh (Category 4)
('P031', 'Sony WH-1000XM5', 'Tai nghe chong on chu dong dinh cao, am thanh chi tiet', 4, 6, 'ACTIVE'),
('P032', 'Sony WF-1000XM5', 'Tai nghe True Wireless chong on thong minh sieu nho gon', 4, 6, 'ACTIVE'),
('P033', 'AirPods Pro Gen 2 Type-C', 'Chong on chu dong, xuyen am thong minh tu Apple', 4, 1, 'ACTIVE'),
('P034', 'AirPods 3', 'Tai nghe open-ear thoi thuong, am thanh vom song dong', 4, 1, 'ACTIVE'),
('P035', 'Sony WH-CH520', 'Tai nghe chup tai gia re, pin trau den 50 gio', 4, 6, 'ACTIVE'),
('P036', 'Samsung Galaxy Buds2 Pro', 'Am thanh vom hifi 24bit, on dinh he sinh thai', 4, 2, 'ACTIVE'),
('P037', 'Xiaomi Buds 5', 'Tai nghe khong day gia tot, thiet ke om tai', 4, 3, 'ACTIVE'),
('P038', 'Sony SRS-XB100', 'Loa di dong khong day nho gon, bass manh me', 4, 6, 'ACTIVE'),
('P039', 'AirPods Max', 'Tai nghe over-ear premium, am thanh kiet tac', 4, 1, 'ACTIVE'),
('P040', 'Anker Soundcore Space Q45', 'Tai nghe chup tai chong on gia tot nhat phan khuc', 4, 9, 'ACTIVE'),

-- Nhóm 5: Phụ kiện & Thiết bị (Category 5)
('P041', 'Logitech MX Master 3S', 'Chuot khong day cho dan van phong va designer', 5, 7, 'ACTIVE'),
('P042', 'Logitech Pebble M350', 'Chuot khong day click im lang nho gon click', 5, 7, 'ACTIVE'),
('P043', 'Keychron K8 Pro', 'Ban phim co wireless ho tro macOS va Windows', 5, 8, 'ACTIVE'),
('P044', 'Keychron K2 V2', 'Ban phim co layout 75% gon nhe quoc dan', 5, 8, 'ACTIVE'),
('P045', 'Anker 737 Power Bank', 'Pin du phong dung luong lon, sac nhanh vao ra 140W', 5, 9, 'ACTIVE'),
('P046', 'Anker Nano 3 30W', 'Cu sac nhanh sieu nho gon cho iPhone va iPad', 5, 9, 'ACTIVE'),
('P047', 'Logitech G Pro X Superlight', 'Chuot gaming sieu nhe danh cho game thu Esport', 5, 7, 'ACTIVE'),
('P048', 'Keychron Q1 Pro', 'Ban phim co custom full nhom cao cap', 5, 8, 'ACTIVE'),
('P049', 'Anker MagGo Power Bank', 'Pin sac du phong khong day ho tro Magsafe', 5, 9, 'ACTIVE'),
('P050', 'Logitech K380', 'Ban phim bluetooth da thiet bi mong nhe thoi trang', 5, 7, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- 6. Seed Product Variants (50 SKUs khớp với 50 mã sản phẩm gốc)
INSERT INTO product_variants (sku, product_id, price, quantity, reserved_quantity, sold_quantity) VALUES
('P001', 'P001', 21990000, 50, 0, 0),
('P002', 'P002', 33990000, 40, 0, 0),
('P003', 'P003', 29990000, 35, 0, 0),
('P004', 'P004', 38990000, 20, 0, 0),
('P005', 'P005', 26990000, 15, 0, 0),
('P006', 'P006', 4690000,  100, 0, 0),
('P007', 'P007', 22490000, 45, 0, 0),
('P008', 'P008', 9990000,  60, 0, 0),
('P009', 'P009', 8490000,  70, 0, 0),
('P010', 'P010', 14290000, 80, 0, 0),
('P011', 'P011', 28990000, 30, 0, 0),
('P012', 'P012', 89990000, 10, 0, 0),
('P013', 'P013', 39990000, 15, 0, 0),
('P014', 'P014', 16990000, 40, 0, 0),
('P015', 'P015', 35490000, 25, 0, 0),
('P016', 'P016', 22990000, 35, 0, 0),
('P017', 'P017', 12490000, 50, 0, 0),
('P018', 'P018', 19490000, 45, 0, 0),
('P019', 'P019', 24990000, 50, 0, 0),
('P020', 'P020', 62990000, 8,  0, 0),
('P021', 'P021', 16990000, 50, 0, 0),
('P022', 'P022', 28990000, 20, 0, 0),
('P023', 'P023', 28490000, 18, 0, 0),
('P024', 'P024', 4990000,  95, 0, 0),
('P025', 'P025', 7490000,  65, 0, 0),
('P026', 'P026', 8790000,  110, 0, 0),
('P027', 'P027', 11990000, 30, 0, 0),
('P028', 'P028', 10490000, 40, 0, 0),
('P029', 'P029', 9990000,  30, 0, 0),
('P030', 'P030', 13990000, 25, 0, 0),
('P031', 'P031', 7990000,  50, 0, 0),
('P032', 'P032', 4990000,  55, 0, 0),
('P033', 'P033', 5390000,  120, 0, 0),
('P034', 'P034', 4190000,  85, 0, 0),
('P035', 'P035', 1190000,  150, 0, 0),
('P036', 'P036', 3590000,  70, 0, 0),
('P037', 'P037', 1490000,  90, 0, 0),
('P038', 'P038', 1290000,  100, 0, 0),
('P039', 'P039', 12490000, 15, 0, 0),
('P040', 'P040', 2390000,  80, 0, 0),
('P041', 'P041', 2490000,  50, 0, 0),
('P042', 'P042', 450000,   300, 0, 0),
('P043', 'P043', 2790000,  60, 0, 0),
('P044', 'P044', 1890000,  85, 0, 0),
('P045', 'P045', 3290000,  40, 0, 0),
('P046', 'P046', 390000,   400, 0, 0),
('P047', 'P047', 2990000,  35, 0, 0),
('P048', 'P048', 4190000,  20, 0, 0),
('P049', 'P049', 1190000,  75, 0, 0),
('P050', 'P050', 690000,   250, 0, 0)
ON CONFLICT (sku) DO NOTHING;

-- 7. Seed Product Images (Dùng link placeholder sạch sẽ, cập nhật hàng loạt sau)
-- Delete existing images for the products being seeded to allow re-runs
DELETE FROM product_images WHERE product_id IN ('P001', 'P002', 'P003', 'P004', 'P005', 'P006', 'P007', 'P008', 'P009', 'P010', 'P011', 'P012', 'P013', 'P014', 'P015', 'P016', 'P017', 'P018', 'P019', 'P020', 'P021', 'P022', 'P023', 'P024', 'P025', 'P026', 'P027', 'P028', 'P029', 'P030', 'P031', 'P032', 'P033', 'P034', 'P035', 'P036', 'P037', 'P038', 'P039', 'P040', 'P041', 'P042', 'P043', 'P044', 'P045', 'P046', 'P047', 'P048', 'P049', 'P050');

INSERT INTO product_images (product_id, sku, image_url, is_primary) VALUES
('P001', 'P001', 'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=500&auto=format&fit=crop', TRUE),
('P002', 'P002', 'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=500&auto=format&fit=crop', TRUE),
('P003', 'P003', 'https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=500&auto=format&fit=crop', TRUE),
('P004', 'P004', 'https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=500&auto=format&fit=crop', TRUE),
('P005', 'P005', 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=500&auto=format&fit=crop', TRUE),
('P006', 'P006', 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=500&auto=format&fit=crop', TRUE),
('P007', 'P007', 'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=500&auto=format&fit=crop', TRUE),
('P008', 'P008', 'https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=500&auto=format&fit=crop', TRUE),
('P009', 'P009', 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=500&auto=format&fit=crop', TRUE),
('P010', 'P010', 'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=500&auto=format&fit=crop', TRUE),
('P011', 'P011', 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=500&auto=format&fit=crop', TRUE),
('P012', 'P012', 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=500&auto=format&fit=crop', TRUE),
('P013', 'P013', 'https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=500&auto=format&fit=crop', TRUE),
('P014', 'P014', 'https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=500&auto=format&fit=crop', TRUE),
('P015', 'P015', 'https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=500&auto=format&fit=crop', TRUE),
('P016', 'P016', 'https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=500&auto=format&fit=crop', TRUE),
('P017', 'P017', 'https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=500&auto=format&fit=crop', TRUE),
('P018', 'P018', 'https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=500&auto=format&fit=crop', TRUE),
('P019', 'P019', 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=500&auto=format&fit=crop', TRUE),
('P020', 'P020', 'https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=500&auto=format&fit=crop', TRUE),
('P021', 'P021', 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=500&auto=format&fit=crop', TRUE),
('P022', 'P022', 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=500&auto=format&fit=crop', TRUE),
('P023', 'P023', 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=500&auto=format&fit=crop', TRUE),
('P024', 'P024', 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=500&auto=format&fit=crop', TRUE),
('P025', 'P025', 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=500&auto=format&fit=crop', TRUE),
('P026', 'P026', 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=500&auto=format&fit=crop', TRUE),
('P027', 'P027', 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=500&auto=format&fit=crop', TRUE),
('P028', 'P028', 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=500&auto=format&fit=crop', TRUE),
('P029', 'P029', 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=500&auto=format&fit=crop', TRUE),
('P030', 'P030', 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=500&auto=format&fit=crop', TRUE),
('P031', 'P031', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop', TRUE),
('P032', 'P032', 'https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=500&auto=format&fit=crop', TRUE),
('P033', 'P033', 'https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=500&auto=format&fit=crop', TRUE),
('P034', 'P034', 'https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=500&auto=format&fit=crop', TRUE),
('P035', 'P035', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop', TRUE),
('P036', 'P036', 'https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=500&auto=format&fit=crop', TRUE),
('P037', 'P037', 'https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=500&auto=format&fit=crop', TRUE),
('P038', 'P038', 'https://images.unsplash.com/photo-1608248597481-496100c8c836?w=500&auto=format&fit=crop', TRUE),
('P039', 'P039', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop', TRUE),
('P040', 'P040', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop', TRUE),
('P041', 'P041', 'https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=500&auto=format&fit=crop', TRUE),
('P042', 'P042', 'https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=500&auto=format&fit=crop', TRUE),
('P043', 'P043', 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=500&auto=format&fit=crop', TRUE),
('P044', 'P044', 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=500&auto=format&fit=crop', TRUE),
('P045', 'P045', 'https://images.unsplash.com/photo-1620288627223-53302f4e8c74?w=500&auto=format&fit=crop', TRUE),
('P046', 'P046', 'https://images.unsplash.com/photo-1620288627223-53302f4e8c74?w=500&auto=format&fit=crop', TRUE),
('P047', 'P047', 'https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=500&auto=format&fit=crop', TRUE),
('P048', 'P048', 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=500&auto=format&fit=crop', TRUE),
('P049', 'P049', 'https://images.unsplash.com/photo-1620288627223-53302f4e8c74?w=500&auto=format&fit=crop', TRUE),
('P050', 'P050', 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=500&auto=format&fit=crop', TRUE);

SELECT setval('product_images_id_seq', COALESCE((SELECT MAX(id) FROM product_images), 1));

-- 8. Seed Coupons (với discount_type, usage_limit, start_date)
INSERT INTO coupons (code, discount_percent, discount_type, active, minimum_order_amount, usage_limit, used_count, start_date, expiry_date) VALUES
('WELCOME10', 10, 'PERCENTAGE', TRUE, 0, 100, 0, CURRENT_TIMESTAMP, NULL),
('SALE15', 15, 'PERCENTAGE', TRUE, 0, 200, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '30 days'),
('VIP20', 20, 'PERCENTAGE', FALSE, 500000, 50, 0, CURRENT_TIMESTAMP, NULL)
ON CONFLICT (code) DO NOTHING;

-- 9. Seed Cart Items
INSERT INTO cart_items (user_id, variant_sku, quantity, created_at) VALUES
('c1111111-1111-1111-1111-111111111111', 'P001', 1, CURRENT_TIMESTAMP),
('c1111111-1111-1111-1111-111111111111', 'P041', 2, CURRENT_TIMESTAMP)
ON CONFLICT (user_id, variant_sku) DO NOTHING;

SELECT setval('cart_items_id_seq', COALESCE((SELECT MAX(id) FROM cart_items), 1));

-- 10. Seed Orders (với payment_status, customer_note)
INSERT INTO orders (id, user_id, total_price, shipping_fee, discount_amount, coupon_code, shipping_address, phone_number, status, payment_status, customer_note, created_at, last_modified_date) VALUES
('ORD-1001', 'c1111111-1111-1111-1111-111111111111', 41979900, 29900, 0, 'WELCOME10', '123 Duong Le Loi, Q1, HCMC', '0901234567', 'DELIVERED', 'PAID', 'Nhận hàng tại nhà', CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '3 days'),
('ORD-1002', 'c1111111-1111-1111-1111-111111111111', 32779900, 29900, 0, NULL, '456 Duong Nguyen Hue, Q1, HCMC', '0907654321', 'PENDING', 'UNPAID', 'Vui lòng giao vào buổi sáng', CURRENT_TIMESTAMP - INTERVAL '6 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours')
ON CONFLICT (id) DO NOTHING;

-- 11. Seed Order Items (với snapshot product_name, variant_name)
INSERT INTO order_items (order_id, variant_sku, product_name, variant_name, quantity, price) VALUES
('ORD-1001', 'P002', 'iPhone 15 Pro Max 256GB', 'Default', 1, 33990000),
('ORD-1001', 'P031', 'Sony WH-1000XM5', 'Default', 1, 7990000),
('ORD-1002', 'P003', 'Samsung Galaxy S24 Ultra', 'Default', 1, 29990000),
('ORD-1002', 'P043', 'Keychron K8 Pro', 'Default', 1, 2790000)
ON CONFLICT (order_id, variant_sku) DO NOTHING;

-- Đồng bộ lại chuỗi tự tăng cho order_items an toàn
SELECT setval('order_items_id_seq', COALESCE((SELECT MAX(id) FROM order_items), 1));