-- =========================================================================
-- SEED DATA MIGRATION FOR NEXORA COMMERCE
-- =========================================================================

-- 1. Seed Roles
INSERT INTO roles (id, name) VALUES 
(1, 'ROLE_CUSTOMER'),
(2, 'ROLE_ADMIN')
ON CONFLICT (name) DO NOTHING;

-- Synchronize roles sequence
SELECT setval('roles_id_seq', COALESCE((SELECT MAX(id) FROM roles), 1));

-- 2. Seed Users (with BCrypt hashed passwords)
-- password123 -> $2a$10$w3v6O7o4yGpeM/oMvR6wse43.J8PzGzX1d8nJ5P/2mS1lKx0G.Fm6
-- admin123    -> $2a$10$vKywXk87186r74c6Lh/UCOh45rQk.eDqGg2P9P3b9k7G0J7N9qN3u
INSERT INTO users (id, username, password, full_name, email, created_at) VALUES 
('c1111111-1111-1111-1111-111111111111', 'customer1', '$2a$10$w3v6O7o4yGpeM/oMvR6wse43.J8PzGzX1d8nJ5P/2mS1lKx0G.Fm6', 'Customer One', 'customer1@shopcart.com', CURRENT_TIMESTAMP),
('a2222222-2222-2222-2222-222222222222', 'admin', '$2a$10$vKywXk87186r74c6Lh/UCOh45rQk.eDqGg2P9P3b9k7G0J7N9qN3u', 'Administrator', 'admin@shopcart.com', CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

-- 3. Map Users to Roles
INSERT INTO users_roles (user_id, role_id) VALUES 
('c1111111-1111-1111-1111-111111111111', 1), -- customer1 -> ROLE_CUSTOMER
('a2222222-2222-2222-2222-222222222222', 2)  -- admin -> ROLE_ADMIN
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 4. Seed Products
INSERT INTO products (id, name, description, category_id, brand_id) VALUES
('P001', 'iPhone 15 128GB', 'Smartphone Apple A16, man hinh Super Retina XDR 6.1 inch', NULL, NULL),
('P002', 'Samsung Galaxy S24', 'Smartphone Android cao cap, man hinh Dynamic AMOLED 2X', NULL, NULL),
('P003', 'Xiaomi 14', 'Smartphone Snapdragon 8 Gen 3, camera Leica', NULL, NULL),
('P004', 'MacBook Air M3 13 inch', 'Laptop mong nhe chip Apple M3, pin toi uu', NULL, NULL),
('P005', 'Dell XPS 13', 'Ultrabook cao cap, man hinh InfinityEdge', NULL, NULL),
('P006', 'iPad Air 11 inch', 'May tinh bang phuc vu hoc tap va cong viec', NULL, NULL),
('P007', 'Sony WH-1000XM5', 'Tai nghe chong on chu dong, chat am chi tiet', NULL, NULL),
('P008', 'Logitech MX Master 3S', 'Chuot khong day cho dan van phong va designer', NULL, NULL),
('P009', 'Keychron K8 Pro', 'Ban phim co wireless ho tro macOS va Windows', NULL, NULL),
('P010', 'Anker 737 Power Bank', 'Pin du phong dung luong lon, sac nhanh 140W', NULL, NULL)
ON CONFLICT (id) DO NOTHING;

-- 5. Seed Product Variants
INSERT INTO product_variants (sku, product_id, price, quantity, reserved_quantity, sold_quantity) VALUES
('P001', 'P001', 21990000, 50, 1, 0),
('P002', 'P002', 18990000, 49, 0, 1),
('P003', 'P003', 15990000, 50, 1, 0),
('P004', 'P004', 28990000, 50, 0, 0),
('P005', 'P005', 32990000, 50, 0, 0),
('P006', 'P006', 16990000, 50, 0, 0),
('P007', 'P007', 7990000, 49, 0, 1),
('P008', 'P008', 2490000, 50, 2, 0),
('P009', 'P009', 2790000, 50, 1, 0),
('P010', 'P010', 3290000, 50, 0, 0)
ON CONFLICT (sku) DO NOTHING;

-- 6. Seed Product Images
-- ĐÃ SỬA: Loại bỏ từ khóa ON CONFLICT không tường minh lỗi cú pháp. Để tránh lặp dữ liệu khi chạy lại file nhiều lần, chúng ta thực hiện xóa dữ liệu cũ trước khi chèn (hoặc dựa vào ID tự tăng sạch từ cơ sở dữ liệu mới tạo).
TRUNCATE TABLE product_images RESTART IDENTITY CASCADE;

INSERT INTO product_images (product_id, sku, image_url, is_primary) VALUES
('P001', 'P001', 'https://res.cloudinary.com/baofan/image/upload/c_fill,g_auto,w_500,h_500,q_auto,f_auto/v1778497692/iphone_15_vpedjn.webp', TRUE),
('P002', 'P002', 'https://res.cloudinary.com/baofan/image/upload/c_fill,g_auto,w_500,h_500,q_auto,f_auto/v1778497692/samsung_s24_c91ta6.webp', TRUE),
('P003', 'P003', 'https://res.cloudinary.com/baofan/image/upload/c_fill,g_auto,w_500,h_500,q_auto,f_auto/v1778497692/xiaomi_14_aiz1x3.webp', TRUE),
('P004', 'P004', 'https://res.cloudinary.com/baofan/image/upload/c_fill,g_auto,w_500,h_500,q_auto,f_auto/v1778497692/macbook_air_13_gy3hye.webp', TRUE),
('P005', 'P005', 'https://res.cloudinary.com/baofan/image/upload/c_fill,g_auto,w_500,h_500,q_auto,f_auto/v1778497693/dell_xps_13_rbgewa.webp', TRUE),
('P006', 'P006', 'https://res.cloudinary.com/baofan/image/upload/c_fill,g_auto,w_500,h_500,q_auto,f_auto/v1778497692/ipad_air_m4_ddu4gg.webp', TRUE),
('P007', 'P007', 'https://res.cloudinary.com/baofan/image/upload/c_fill,g_auto,w_500,h_500,q_auto,f_auto/v1778497692/sony_qq4ywl.webp', TRUE),
('P008', 'P008', 'https://res.cloudinary.com/baofan/image/upload/c_fill,g_auto,w_500,h_500,q_auto,f_auto/v1778497692/logictech_chbwix.webp', TRUE),
('P009', 'P009', 'https://res.cloudinary.com/baofan/image/upload/c_fill,g_auto,w_500,h_500,q_auto,f_auto/v1778497692/keychron_k8_jw8b6c.webp', TRUE),
('P010', 'P010', 'https://res.cloudinary.com/baofan/image/upload/c_fill,g_auto,w_500,h_500,q_auto,f_auto/v1778497693/anker_737_fdw6oe.webp', TRUE);

-- Synchronize product images sequence
SELECT setval('product_images_id_seq', COALESCE((SELECT MAX(id) FROM product_images), 1));

-- 7. Seed Coupons
INSERT INTO coupons (code, discount_percent, active, minimum_order_amount, expiry_date) VALUES
('WELCOME10', 10, TRUE, 0, NULL),
('SALE15', 15, TRUE, 0, NULL),
('VIP20', 20, FALSE, 0, NULL)
ON CONFLICT (code) DO NOTHING;

-- 8. Seed Cart Items
INSERT INTO cart_items (user_id, variant_sku, quantity, created_at) VALUES
('c1111111-1111-1111-1111-111111111111', 'P001', 1, CURRENT_TIMESTAMP),
('c1111111-1111-1111-1111-111111111111', 'P008', 2, CURRENT_TIMESTAMP)
ON CONFLICT (user_id, variant_sku) DO NOTHING;

-- Synchronize cart items sequence
SELECT setval('cart_items_id_seq', COALESCE((SELECT MAX(id) FROM cart_items), 1));

-- 9. Seed Orders
-- ĐÃ SỬA: Đổi giá trị NULL tại cột user_id của đơn hàng 'ORD-1002' thành 'c1111111-1111-1111-1111-111111111111' để thỏa mãn ràng buộc NOT NULL bảo vệ tính nhất quán dữ liệu.
INSERT INTO orders (id, user_id, total_price, shipping_fee, discount_amount, coupon_code, shipping_address, phone_number, status, created_at, last_modified_date) VALUES
('ORD-1001', 'c1111111-1111-1111-1111-111111111111', 27009900, 29900, 0, 'WELCOME10', '123 Duong Le Loi, Q1, HCMC', '0901234567', 'DELIVERED', CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '3 days'),
('ORD-1002', 'c1111111-1111-1111-1111-111111111111', 18809900, 29900, 0, NULL, '456 Duong Nguyen Hue, Q1, HCMC', '0907654321', 'PENDING', CURRENT_TIMESTAMP - INTERVAL '6 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours')
ON CONFLICT (id) DO NOTHING;

-- 10. Seed Order Items
INSERT INTO order_items (order_id, variant_sku, quantity, price) VALUES
('ORD-1001', 'P002', 1, 18990000),
('ORD-1001', 'P007', 1, 7990000),
('ORD-1002', 'P003', 1, 15990000),
('ORD-1002', 'P009', 1, 2790000)
ON CONFLICT (id, order_id, variant_sku) DO NOTHING; -- Bổ sung chỉ mục đầy đủ cho ON CONFLICT của bảng order_items nếu chạy lại nhiều lần

-- Synchronize order items sequence
SELECT setval('order_items_id_seq', COALESCE((SELECT MAX(id) FROM order_items), 1));