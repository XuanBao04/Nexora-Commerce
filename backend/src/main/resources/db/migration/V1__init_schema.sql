-- Enable vector and UUID extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;

-- =========================================================================
-- CREATE SEQUENCES FOR HIBERNATE GENERATORS
-- =========================================================================
CREATE SEQUENCE IF NOT EXISTS roles_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS user_addresses_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS refresh_tokens_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS verification_tokens_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS categories_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS brands_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS product_attributes_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS product_attribute_values_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS product_images_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS cart_items_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS wishlists_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS order_items_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS order_status_history_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS payment_transactions_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS product_reviews_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS review_images_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ai_chat_messages_id_seq START WITH 1 INCREMENT BY 1;

-- =========================================================================
-- PHÂN HỆ 1: NGƯỜI DÙNG & BẢO MẬT (6 TABLES)
-- =========================================================================

-- 1. Table: users
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    email VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Table: roles
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT PRIMARY KEY DEFAULT nextval('roles_id_seq'),
    name VARCHAR(50) NOT NULL UNIQUE
);

-- 3. Table: users_roles (Junction table)
CREATE TABLE IF NOT EXISTS users_roles (
    user_id UUID NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_users_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_users_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- 4. Table: user_addresses
CREATE TABLE IF NOT EXISTS user_addresses (
    id BIGINT PRIMARY KEY DEFAULT nextval('user_addresses_id_seq'),
    user_id UUID NOT NULL,
    receiver_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    address_line VARCHAR(255) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_user_addresses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 5. Table: refresh_tokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT PRIMARY KEY DEFAULT nextval('refresh_tokens_id_seq'),
    user_id UUID NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 6. Table: verification_tokens
CREATE TABLE IF NOT EXISTS verification_tokens (
    id BIGINT PRIMARY KEY DEFAULT nextval('verification_tokens_id_seq'),
    user_id UUID NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL, -- 'EMAIL_VERIFICATION' or 'PASSWORD_RESET'
    expiry_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =========================================================================
-- PHÂN HỆ 2: DANH MỤC & BIẾN THỂ SẢN PHẨM (8 TABLES)
-- =========================================================================

-- 7. Table: categories
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT PRIMARY KEY DEFAULT nextval('categories_id_seq'),
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    parent_id BIGINT,
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL
);

-- 8. Table: brands
CREATE TABLE IF NOT EXISTS brands (
    id BIGINT PRIMARY KEY DEFAULT nextval('brands_id_seq'),
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE
);

-- 9. Table: products
CREATE TABLE IF NOT EXISTS products (
    id VARCHAR(50) PRIMARY KEY, -- Custom product ID (e.g. 'P001')
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category_id BIGINT,
    brand_id BIGINT,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands(id) ON DELETE SET NULL
);

-- 10. Table: product_attributes
CREATE TABLE IF NOT EXISTS product_attributes (
    id BIGINT PRIMARY KEY DEFAULT nextval('product_attributes_id_seq'),
    name VARCHAR(100) NOT NULL UNIQUE
);

-- 11. Table: product_attribute_values
CREATE TABLE IF NOT EXISTS product_attribute_values (
    id BIGINT PRIMARY KEY DEFAULT nextval('product_attribute_values_id_seq'),
    attribute_id BIGINT NOT NULL,
    value VARCHAR(100) NOT NULL,
    CONSTRAINT fk_attribute_values_attribute FOREIGN KEY (attribute_id) REFERENCES product_attributes(id) ON DELETE CASCADE,
    CONSTRAINT uq_attribute_value UNIQUE (attribute_id, value)
);

-- 12. Table: product_variants
CREATE TABLE IF NOT EXISTS product_variants (
    sku VARCHAR(50) PRIMARY KEY, -- Custom SKU (e.g. 'IP15-BLK-128')
    product_id VARCHAR(50) NOT NULL,
    price BIGINT NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    sold_quantity INTEGER NOT NULL DEFAULT 0,
    embedding vector(768), -- Embedding vector for Semantic Search using Gemini text-embedding-004
    CONSTRAINT fk_variants_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- 13. Table: variant_attribute_values (Junction table)
CREATE TABLE IF NOT EXISTS variant_attribute_values (
    sku VARCHAR(50) NOT NULL,
    value_id BIGINT NOT NULL,
    PRIMARY KEY (sku, value_id),
    CONSTRAINT fk_variant_attr_variant FOREIGN KEY (sku) REFERENCES product_variants(sku) ON DELETE CASCADE,
    CONSTRAINT fk_variant_attr_value FOREIGN KEY (value_id) REFERENCES product_attribute_values(id) ON DELETE CASCADE
);

-- 14. Table: product_images
CREATE TABLE IF NOT EXISTS product_images (
    id BIGINT PRIMARY KEY DEFAULT nextval('product_images_id_seq'),
    product_id VARCHAR(50) NOT NULL,
    sku VARCHAR(50), -- Can reference a specific variant
    image_url VARCHAR(255) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_images_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_images_variant FOREIGN KEY (sku) REFERENCES product_variants(sku) ON DELETE CASCADE
);

-- =========================================================================
-- PHÂN HỆ 3: GIỎ HÀNG & YÊU THÍCH (2 TABLES)
-- =========================================================================

-- 15. Table: cart_items
CREATE TABLE IF NOT EXISTS cart_items (
    id BIGINT PRIMARY KEY DEFAULT nextval('cart_items_id_seq'),
    user_id UUID NOT NULL,
    variant_sku VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cart_items_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_variant FOREIGN KEY (variant_sku) REFERENCES product_variants(sku) ON DELETE CASCADE,
    CONSTRAINT uq_user_variant UNIQUE (user_id, variant_sku)
);

-- 16. Table: wishlists
CREATE TABLE IF NOT EXISTS wishlists (
    id BIGINT PRIMARY KEY DEFAULT nextval('wishlists_id_seq'),
    user_id UUID NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wishlists_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlists_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_product UNIQUE (user_id, product_id)
);

-- =========================================================================
-- PHÂN HỆ 4: ĐƠN HÀNG, LỊCH SỬ & KHUYẾN MÃI (5 TABLES)
-- =========================================================================

-- 17. Table: coupons
CREATE TABLE IF NOT EXISTS coupons (
    code VARCHAR(50) PRIMARY KEY,
    discount_percent INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    minimum_order_amount BIGINT NOT NULL DEFAULT 0,
    expiry_date TIMESTAMP
);

-- 18. Table: orders
CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(50) PRIMARY KEY, -- Custom Order ID (e.g. 'ORD-1001')
    user_id UUID NOT NULL,
    total_price BIGINT NOT NULL,
    shipping_fee BIGINT NOT NULL DEFAULT 0,
    discount_amount BIGINT NOT NULL DEFAULT 0,
    coupon_code VARCHAR(50),
    shipping_address VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    status VARCHAR(50) NOT NULL, -- e.g. 'PENDING', 'PAID', 'SHIPPING', 'DELIVERED', 'CANCELLED'
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_coupon FOREIGN KEY (coupon_code) REFERENCES coupons(code) ON DELETE SET NULL
);

-- 19. Table: order_items
CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT PRIMARY KEY DEFAULT nextval('order_items_id_seq'),
    order_id VARCHAR(50) NOT NULL,
    variant_sku VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL,
    price BIGINT NOT NULL, -- Price snapshot at checkout
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_variant FOREIGN KEY (variant_sku) REFERENCES product_variants(sku) ON DELETE RESTRICT
);

-- 20. Table: order_status_history (Audit Log)
CREATE TABLE IF NOT EXISTS order_status_history (
    id BIGINT PRIMARY KEY DEFAULT nextval('order_status_history_id_seq'),
    order_id VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    changed_by VARCHAR(100) NOT NULL, -- Username or 'SYSTEM'
    reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_history_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- 21. Table: payment_transactions
CREATE TABLE IF NOT EXISTS payment_transactions (
    id BIGINT PRIMARY KEY DEFAULT nextval('payment_transactions_id_seq'),
    order_id VARCHAR(50) NOT NULL,
    payment_method VARCHAR(50) NOT NULL, -- 'COD', 'VNPAY', 'MOMO'
    amount BIGINT NOT NULL,
    provider_transaction_id VARCHAR(255), -- Reference ID from payment provider
    status VARCHAR(50) NOT NULL, -- 'PENDING', 'SUCCESS', 'FAILED'
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_trans_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- =========================================================================
-- PHÂN HỆ 5: ĐÁNH GIÁ & TƯƠNG TÁC AI (3 TABLES)
-- =========================================================================

-- 22. Table: product_reviews
CREATE TABLE IF NOT EXISTS product_reviews (
    id BIGINT PRIMARY KEY DEFAULT nextval('product_reviews_id_seq'),
    user_id UUID NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    order_id VARCHAR(50), -- Linked to verified purchase
    rating INTEGER NOT NULL,
    comment TEXT,
    parent_id BIGINT, -- Self reference for Admin replies
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL,
    CONSTRAINT fk_reviews_parent FOREIGN KEY (parent_id) REFERENCES product_reviews(id) ON DELETE CASCADE
);

-- 23. Table: review_images
CREATE TABLE IF NOT EXISTS review_images (
    id BIGINT PRIMARY KEY DEFAULT nextval('review_images_id_seq'),
    review_id BIGINT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    CONSTRAINT fk_review_images_review FOREIGN KEY (review_id) REFERENCES product_reviews(id) ON DELETE CASCADE
);

-- 24. Table: ai_chat_messages (AI Conversation Log)
CREATE TABLE IF NOT EXISTS ai_chat_messages (
    id BIGINT PRIMARY KEY DEFAULT nextval('ai_chat_messages_id_seq'),
    user_id UUID, -- Can be null for guest session
    session_id VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL, -- 'USER' or 'AI'
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_messages_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Create index for performance on key search columns
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_brand ON products(brand_id);
CREATE INDEX IF NOT EXISTS idx_variants_product ON product_variants(product_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_user ON cart_items(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_user ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_ai_chat_session ON ai_chat_messages(session_id);
