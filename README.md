# 🛒 Nexora Commerce

> AI-Powered E-Commerce Platform — Spring Boot · React · PostgreSQL/pgvector · Redis · VNPAY

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-blue?logo=react)](https://react.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue?logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red?logo=redis)](https://redis.io/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![Status](https://img.shields.io/badge/Status-Phase%201%20(MVP)-blue)](README.md#-trạng-thái-dự-án)


## 📌 Giới thiệu

**Nexora Commerce** là nền tảng thương mại điện tử hiện đại tích hợp trí tuệ nhân tạo, được xây dựng với kiến trúc production-ready. Hệ thống bao gồm đầy đủ nghiệp vụ e-commerce cốt lõi, cổng thanh toán VNPAY với lịch sử giao dịch chi tiết, và 5 tính năng AI đột phá sử dụng **Gemini API** kết hợp **pgvector**.

**Cập nhật lần cuối:** 31/05/2026

---

## 📊 Trạng thái dự án (Phase 1 - MVP)

| Giai đoạn | Nội dung | Trạng thái | Chi tiết |
|---|---|---|---|
| **Phase 0** | Database schema (24 bảng) + Flyway | ✅ Hoàn thành | PostgreSQL + pgvector extension |
| **Phase 0** | Java Entities & JPA Mapping | ✅ Hoàn thành | Đầy đủ 24 entities với @Entity |
| **Phase 1** | E-Commerce Core Features | 🔄 Đang phát triển | Auth, Products, Cart, Orders |
| **Phase 1** | VNPAY Payment Integration | ✅ Hoàn thành | Sandbox environment + IPN |
| **Phase 1** | Payment History & Transactions | ✅ Hoàn thành | Backend + Frontend UI |
| **Phase 1** | React Frontend (Core) | 🔄 Đang phát triển | Product listing, Cart, Orders |
| **Phase 2** | 5 Tính năng AI (Gemini) | ⚪ Sắp bắt đầu | RAG Chatbot, Semantic Search, ... |
| **Phase 3** | Social Commerce | ⚪ Chưa bắt đầu | Tương lai |

---

## ✨ Tính năng nổi bật

### 🏪 E-Commerce Core
- Quản lý sản phẩm đa biến thể (màu sắc, dung lượng, kích cỡ)
- Giỏ hàng động, mã giảm giá, phí vận chuyển
- Hệ thống đơn hàng với lịch sử trạng thái đầy đủ (Audit log)
- **Redis Distributed Lock** — ngăn chặn overselling khi nhiều người mua cùng lúc
- **Reserve on Checkout** — giữ kho an toàn trong thời gian thanh toán

### 💳 Thanh toán & Lịch sử giao dịch
- Tích hợp cổng thanh toán **VNPAY Sandbox**
- Xử lý ReturnURL (giao diện) và IPN (server-to-server bất đồng bộ)
- Tự động hoàn kho khi thanh toán thất bại
- **Lịch sử giao dịch chi tiết** — theo dõi tất cả giao dịch (VNPAY, COD, ...)
- **Trạng thái thanh toán** — UNPAID, PAID, REFUNDED với badge màu sắc
- **Kiểm tra tính toàn vẹn giao dịch** — xác thực HMAC-SHA512 từ VNPAY

### 🤖 5 Tính năng AI (Gemini + pgvector)
| # | Tính năng | Mô tả |
|---|---|---|
| 1 | **RAG Shopping Chatbot** | Tư vấn mua sắm bằng ngôn ngữ tự nhiên, tìm sản phẩm theo ngữ nghĩa |
| 2 | **Semantic Search** | Tìm kiếm theo ý nghĩa câu thay vì khớp từ khóa (cosine distance pgvector) |
| 3 | **AI Review Summarizer** | Tóm tắt Ưu/Nhược điểm sản phẩm từ hàng trăm đánh giá, cache Redis 1h |
| 4 | **Sentiment & Auto-Reply** | Phân tích sắc thái review, tự động sinh câu trả lời Admin phù hợp |
| 5 | **SEO Content Generator** | Sinh mô tả sản phẩm chuẩn SEO từ thông số kỹ thuật đầu vào |

### 🔐 Bảo mật
- JWT Access Token + Refresh Token rotation
- Dynamic RBAC — phân quyền động theo vai trò (ADMIN / USER)
- UUID-based User ID — không lộ thông tin tuần tự

---

## 🏗️ Kiến trúc hệ thống

```
┌──────────────────┐     ┌──────────────────────────────────────┐
│   React Frontend  │────▶│         Spring Boot API              │
│   (Vite · TS)    │     │                                      │
└──────────────────┘     │  ┌─────────────┐  ┌──────────────┐  │
                         │  │ Spring AI   │  │ VNPAY Service│  │
                         │  │ (Gemini LLM)│  │ (Sandbox)    │  │
                         │  └──────┬──────┘  └──────────────┘  │
                         └─────────│────────────────────────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              ▼                    ▼                    ▼
     ┌─────────────────┐  ┌──────────────┐  ┌──────────────────┐
     │ PostgreSQL 17   │  │  Redis 7     │  │  Gemini API      │
     │ + pgvector ext  │  │  Cache+Lock  │  │  LLM + Embedding │
     │ (24 tables)     │  │              │  │                  │
     └─────────────────┘  └──────────────┘  └──────────────────┘
```


## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 21 · Spring Boot 3.3 · Spring Security · Spring AI |
| **Database** | PostgreSQL 17 + pgvector extension · Flyway migrations |
| **Cache & Lock** | Redis 7 · Lettuce client · Distributed Locking |
| **Frontend** | React 19 · TypeScript · Vite · TailwindCSS |
| **AI** | Google Gemini API · Spring AI · pgvector (vector(768)) |
| **Payment** | VNPAY Sandbox · HMAC-SHA512 |
| **Auth** | JWT (JJWT 0.12) · BCrypt · Refresh Token rotation |
| **DevOps** | Docker Compose · WSL2 Ubuntu |
| **Docs** | SpringDoc OpenAPI (Swagger UI) |

---

## 🚀 Khởi chạy nhanh

### Yêu cầu
- **Docker & Docker Compose** (khuyến nghị) — one-command setup
- **Node.js 20+** — cho frontend development
- **Java 21+** — cho backend development (optional nếu dùng Docker)
- **PostgreSQL 17** — nếu chạy local (optional)
- **Redis 7** — nếu chạy local (optional)

### Phương án 1: Dùng Docker (Khuyến nghị)

```bash
# 1. Clone repository
git clone https://github.com/XuanBao04/nexora-commerce.git
cd nexora-commerce

# 2. Cấu hình môi trường
cp .env.example .env
# ✏️ Chỉnh sửa .env nếu cần (VNPAY_TMN_CODE, GEMINI_API_KEY, ...)

# 3. Khởi chạy tất cả services
chmod +x scripts/dev.sh scripts/fullstack/*.sh
./scripts/fullstack/start.sh

# Hoặc dùng Docker Compose trực tiếp
docker-compose -f docker/docker-compose.dev.yml up -d
```

### Phương án 2: Chạy local (Backend + Frontend riêng)

#### Backend
```bash
cd backend
mvn clean install
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

#### Frontend
```bash
cd frontend
pnpm install
pnpm dev
```

### 3. Truy cập dịch vụ

| Dịch vụ | URL | Ghi chú |
|---|---|---|
| **Frontend** | http://localhost:5173 | React Vite dev server |
| **API** | http://localhost:8080/api | Spring Boot backend |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | API documentation |
| **Database** | localhost:5432 | PostgreSQL (user: admin) |
| **Redis** | localhost:6379 | In-memory cache |

### 4. Dừng services

```bash
# Dùng script
./scripts/fullstack/stop.sh

# Hoặc Docker Compose
docker-compose -f docker/docker-compose.dev.yml down

# Xóa volumes (xóa dữ liệu)
docker-compose -f docker/docker-compose.dev.yml down -v
```

---

## 📊 Database Schema (24 bảng)

### Thiết kế chính
```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         NEXORA COMMERCE DATABASE                             │
├─────────────────────────────────────────────────────────────────────────────┤

👤 NGƯỜI DÙNG & BẢO MẬT (5 bảng)
├─ users                    — UUID PK, email, password (bcrypt), role
├─ roles                     — ADMIN, USER, VENDOR
├─ user_roles               — Many-to-many relationship
├─ user_addresses           — Shipping/billing addresses
├─ refresh_tokens           — JWT refresh token management
└─ verification_tokens      — Email verification

📦 DANH MỤC & SẢN PHẨM (7 bảng)
├─ categories               — Product categories
├─ brands                   — Brand information
├─ products                 — Product master (name, description, ...)
├─ product_variants         — SKU, color, size, stock, pgvector(768) embedding
├─ product_attributes       — Attribute definitions (Size, Color, ...)
├─ product_attribute_values — Attribute values
└─ product_images           — Product images (Cloudinary URLs)

🛒 GIỎ HÀNG (1 bảng)
└─ cart_items               — User cart (user_id + variant_sku + quantity)

📋 ĐƠN HÀNG & THANH TOÁN (5 bảng)
├─ orders                   — Order master (user_id, total, shipping_fee, ...)
├─ order_items              — Order line items (order_id + variant_sku)
├─ order_status_history     — Audit log (PENDING → CONFIRMED → SHIPPED → ...)
├─ payment_transactions     — Transaction history (status, provider_ref, ...)
└─ coupons                  — Discount codes

⭐ ĐÁNH GIÁ & AI (3 bảng)
├─ product_reviews          — Review text, rating, verified_purchase
├─ review_images            — Review images
└─ ai_chat_messages         — RAG chatbot messages storage

```

### Đặc điểm nổi bật của schema
- **UUID PK**: Tất cả tables dùng `uuid` primary key (không tuần tự)
- **pgvector(768)**: `product_variants` có vector embedding cho semantic search
- **Audit Trail**: `order_status_history` để tracking đơn hàng
- **Payment Tracking**: `payment_transactions` để lưu chi tiết từng giao dịch
- **Relationships**: Foreign keys đầy đủ với CASCADE delete
- **Flyway Migration**: Version control cho schema (`V1__init_schema.sql`)

### Tối ưu hóa
- Indexed fields: `user_id`, `product_id`, `order_id`, `sku`, `email`
- Composite indexes: `(order_id, created_at)`, `(user_id, created_at)`
- Vector index: pgvector HNSW index cho similarity search

---

## 📁 Cấu trúc dự án

```
nexora-commerce/
├── backend/                              # Spring Boot 3.3 API
│   ├── src/main/java/com/nexoracommerce/
│   │   ├── auth/                         # Authentication & Authorization
│   │   │   ├── controller/               # AuthController.java
│   │   │   ├── service/                  # AuthService.java
│   │   │   └── dto/                      # Login, Register DTOs
│   │   │
│   │   ├── user/                         # User Management
│   │   │   ├── entity/                   # User, UserAddress, UserRole
│   │   │   ├── repository/               # User, UserRole repositories
│   │   │   └── service/                  # UserService.java
│   │   │
│   │   ├── product/                      # Products & Inventory
│   │   │   ├── entity/                   # Product, ProductVariant, Category
│   │   │   ├── repository/               # ProductRepository, ...
│   │   │   ├── service/                  # ProductService.java
│   │   │   └── controller/               # ProductController.java
│   │   │
│   │   ├── cart/                         # Shopping Cart
│   │   │   ├── entity/                   # CartItem.java
│   │   │   ├── repository/               # CartItemRepository.java
│   │   │   └── service/                  # CartService.java
│   │   │
│   │   ├── checkout/                     # Checkout & Inventory Lock
│   │   │   ├── service/                  # CheckoutService.java (Redis lock)
│   │   │   └── dto/                      # CheckoutRequest, CheckoutResponse
│   │   │
│   │   ├── order/                        # Orders & Order Items
│   │   │   ├── entity/                   # Order, OrderItem, OrderStatusHistory
│   │   │   ├── repository/               # OrderRepository, ...
│   │   │   ├── service/                  # OrderService.java
│   │   │   └── controller/               # OrderController.java
│   │   │
│   │   ├── payment/                      # Payment Processing
│   │   │   ├── entity/                   # PaymentTransaction.java
│   │   │   ├── repository/               # PaymentTransactionRepository.java
│   │   │   ├── service/                  # PaymentService.java, VnpayService.java
│   │   │   ├── controller/               # PaymentController.java
│   │   │   └── dto/                      # PaymentTransactionResponse.java
│   │   │
│   │   ├── review/                       # Product Reviews
│   │   │   ├── entity/                   # ProductReview, ReviewImage
│   │   │   ├── repository/               # ProductReviewRepository.java
│   │   │   └── service/                  # ReviewService.java
│   │   │
│   │   ├── ai/                           # 5 AI Features (Phase 2)
│   │   │   ├── chatbot/                  # RAG Shopping Chatbot
│   │   │   ├── search/                   # Semantic Search
│   │   │   ├── review/                   # Review Summarizer & Sentiment
│   │   │   └── content/                  # SEO Content Generator
│   │   │
│   │   ├── config/                       # Configuration
│   │   │   ├── SecurityConfig.java       # JWT, CORS, Authorization
│   │   │   ├── SwaggerConfig.java        # Swagger/OpenAPI documentation
│   │   │   ├── CorsConfig.java           # CORS settings
│   │   │   └── RedisConfig.java          # Redis client configuration
│   │   │
│   │   └── common/                       # Utilities & Exceptions
│   │       ├── exception/                # GlobalExceptionHandler, ...
│   │       ├── response/                 # ApiResponse, ApiError
│   │       ├── mapper/                   # MapStruct mappers
│   │       └── util/                     # StringUtil, ValidationUtil
│   │
│   ├── src/main/resources/
│   │   ├── application.yml               # Main config
│   │   ├── application-dev.yml           # Development profile
│   │   ├── application-prod.yml          # Production profile
│   │   ├── application-test.yml          # Test profile
│   │   ├── db/migration/                 # Flyway migrations
│   │   │   └── V1__init_schema.sql       # 24 tables + pgvector
│   │   └── db/                           # Database ERD
│   │       └── nexora_erd.dbml           # DBML schema definition
│   │
│   ├── pom.xml                           # Maven dependencies
│   ├── Dockerfile                        # Production image
│   └── Dockerfile.dev                    # Development image
│
├── frontend/                             # React 19 + TypeScript
│   ├── src/
│   │   ├── pages/                        # Page components
│   │   │   ├── Home.tsx
│   │   │   ├── ProductDetail.tsx
│   │   │   ├── Cart.tsx
│   │   │   ├── Checkout.tsx
│   │   │   ├── Orders.tsx
│   │   │   ├── Order.tsx                 # Order detail with payment history
│   │   │   ├── Login.tsx
│   │   │   └── Register.tsx
│   │   │
│   │   ├── components/                   # Reusable components
│   │   │   ├── HeaderLayout.tsx
│   │   │   ├── Footer.tsx
│   │   │   └── ui/                       # shadcn/ui components
│   │   │
│   │   ├── features/                     # Feature modules
│   │   │   ├── auth/                     # Auth state & actions
│   │   │   ├── products/                 # Product state & actions
│   │   │   ├── cart/                     # Cart state & actions
│   │   │   ├── orders/                   # Orders state & actions
│   │   │   └── admin/                    # Admin features (Phase 2)
│   │   │
│   │   ├── hooks/                        # Custom React hooks
│   │   │   ├── useAsync.ts               # Async data fetching
│   │   │   ├── useDebounce.ts            # Debounce hook
│   │   │   └── usePagination.ts          # Pagination hook
│   │   │
│   │   ├── services/                     # API services
│   │   │   ├── api/                      # API client (axios)
│   │   │   ├── authService.ts
│   │   │   ├── productService.ts
│   │   │   ├── orderService.ts
│   │   │   ├── paymentService.ts
│   │   │   └── checkoutService.ts
│   │   │
│   │   ├── store/                        # Zustand state management
│   │   │   ├── useAuthStore.ts
│   │   │   ├── useCartStore.ts
│   │   │   └── useOrderStore.ts
│   │   │
│   │   ├── types/                        # TypeScript types
│   │   │   ├── api.ts                    # API response types
│   │   │   ├── order.ts                  # Order & Payment types
│   │   │   ├── product.ts                # Product types
│   │   │   └── user.ts                   # User types
│   │   │
│   │   ├── lib/                          # Utilities
│   │   │   └── utils.ts                  # Shared utilities
│   │   │
│   │   ├── routes/                       # Routing
│   │   │   ├── AppRoutes.tsx             # Route definitions
│   │   │   └── paths.ts                  # Route constants
│   │   │
│   │   ├── App.tsx                       # Root component
│   │   └── main.tsx                      # Entry point
│   │
│   ├── package.json                      # Dependencies (React 19, TailwindCSS, ...)
│   ├── vite.config.ts                    # Vite configuration
│   ├── tailwind.config.js                # TailwindCSS config
│   ├── tsconfig.json
│   ├── Dockerfile                        # Production nginx image
│   ├── Dockerfile.dev                    # Development image
│   └── nginx.conf                        # Nginx configuration
│
├── scripts/                              # Automation scripts
│   ├── dev.sh                            # Start all services
│   ├── down.sh                           # Stop all services
│   ├── prod.sh                           # Production deployment
│   ├── logs.sh                           # View service logs
│   ├── redis-cli.sh                      # Redis CLI access
│   │
│   ├── backend/
│   │   ├── dev.sh                        # Start backend only
│   │   ├── build.sh                      # Build backend
│   │   ├── docker-build.sh               # Build Docker image
│   │   ├── stop.sh                       # Stop backend
│   │   └── logs.sh                       # View backend logs
│   │
│   ├── frontend/
│   │   ├── dev.sh                        # Start frontend (Vite dev server)
│   │   ├── build.sh                      # Build frontend (production)
│   │   ├── docker-build.sh               # Build Docker image
│   │   ├── stop.sh                       # Stop frontend
│   │   └── logs.sh                       # View frontend logs
│   │
│   ├── docker/
│   │   ├── build.sh                      # Build all Docker images
│   │   ├── up.sh                         # Start Docker Compose
│   │   ├── down.sh                       # Stop Docker Compose
│   │   ├── clean.sh                      # Remove Docker containers & volumes
│   │   └── logs.sh                       # View Docker logs
│   │
│   ├── fullstack/
│   │   ├── start.sh                      # Start everything
│   │   ├── stop.sh                       # Stop everything
│   │   ├── restart.sh                    # Restart everything
│   │   └── build.sh                      # Build everything
│   │
│   └── common/                           # Shared scripts
│       ├── colors.sh                     # Color output utilities
│       ├── config.sh                     # Configuration
│       └── helpers.sh                    # Helper functions
│
├── docker/                               # Docker Compose files
│   ├── docker-compose.yml                # Production compose
│   ├── docker-compose.dev.yml            # Development compose
│   └── docker-compose.prod.yml           # Production with volumes
│
├── docs/                                 # Documentation
│   ├── swagger-openapi.yml               # OpenAPI specification
│   └── nexora_erd.dbml                   # Database ERD (DBML format)
│
├── Makefile                              # Make targets
├── README.md                             # This file
├── task.md                               # Progress tracking
├── .env.example                          # Environment template
└── .gitignore
```

### 📊 Backend Architecture

```
┌─────────────────────────────────────────────┐
│         Spring Boot 3.3 API Layer            │
├─────────────────────────────────────────────┤
│ Controllers (REST API endpoints)             │
│ ├─ AuthController                           │
│ ├─ ProductController                        │
│ ├─ CartController                           │
│ ├─ OrderController                          │
│ ├─ PaymentController                        │
│ └─ ReviewController                         │
├─────────────────────────────────────────────┤
│ Services (Business Logic)                    │
│ ├─ AuthService ─→ UserService               │
│ ├─ ProductService ─→ VnpayService           │
│ ├─ CartService ─→ CheckoutService           │
│ ├─ OrderService ─→ PaymentService           │
│ ├─ ReviewService ─→ AiService (Phase 2)     │
│ └─ InventoryService ─→ RedisLock            │
├─────────────────────────────────────────────┤
│ Repositories (JPA Data Access)               │
│ ├─ UserRepository                           │
│ ├─ ProductRepository                        │
│ ├─ OrderRepository                          │
│ ├─ PaymentTransactionRepository             │
│ └─ ProductReviewRepository                  │
├─────────────────────────────────────────────┤
│ Data Layer                                   │
│ ├─ PostgreSQL 17 (24 tables + pgvector)     │
│ ├─ Redis 7 (Cache + Distributed Lock)       │
│ └─ Gemini API (Phase 2)                     │
└─────────────────────────────────────────────┘
```

---

## 🔑 Biến môi trường quan trọng

Xem file `.env.example` để biết tất cả biến. Các biến bắt buộc:

```env
# Database
DB_URL=jdbc:postgresql://localhost:5432/nexora_commerce
DB_USERNAME=admin
DB_PASSWORD=your_password

# JWT
JWT_SECRET=your_secret_key_min_64_chars

# VNPAY Sandbox
VNPAY_TMN_CODE=your_tmn_code
VNPAY_HASH_SECRET=your_hash_secret

# Gemini AI
GEMINI_API_KEY=your_gemini_api_key

# Cloudinary (ảnh sản phẩm)
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

---

## 📖 API Documentation

### Interactive Swagger UI
- **URL**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs
- **YAML**: [docs/swagger-openapi.yml](docs/swagger-openapi.yml)

### Các API Endpoints chính (Phase 1)

```
🔐 Authentication
  POST   /api/auth/register           — Đăng ký tài khoản
  POST   /api/auth/login              — Đăng nhập
  POST   /api/auth/refresh-token      — Làm mới JWT token
  POST   /api/auth/logout             — Đăng xuất

📦 Products
  GET    /api/products                — Danh sách sản phẩm (phân trang)
  GET    /api/products/{id}           — Chi tiết sản phẩm
  GET    /api/products/search         — Tìm kiếm sản phẩm
  GET    /api/categories              — Danh sách danh mục

🛒 Shopping Cart
  GET    /api/cart                    — Lấy giỏ hàng
  POST   /api/cart/items              — Thêm vào giỏ
  PUT    /api/cart/items/{itemId}     — Cập nhật số lượng
  DELETE /api/cart/items/{itemId}     — Xóa khỏi giỏ

📋 Orders
  POST   /api/orders                  — Tạo đơn hàng
  GET    /api/orders                  — Danh sách đơn hàng của user
  GET    /api/orders/{id}             — Chi tiết đơn hàng + payment history
  PUT    /api/orders/{id}/cancel      — Hủy đơn hàng

💳 Payments
  POST   /api/payments/vnpay          — Tạo request VNPAY
  GET    /api/payments/vnpay/callback — VNPAY callback (ReturnURL)
  POST   /api/payments/vnpay/ipn      — VNPAY IPN (server-to-server)
  GET    /api/payments/history        — Lịch sử giao dịch

⭐ Reviews
  GET    /api/products/{id}/reviews   — Danh sách review
  POST   /api/reviews                 — Thêm review
  PUT    /api/reviews/{id}            — Cập nhật review
```

---

## ⚙️ Các chức năng quan trọng (Đã triển khai)

- **Xác thực & Phiên**: JWT Access Token với Refresh Token rotation, refresh token được lưu trong Cookie `HttpOnly` bảo mật. Xem `AuthController` để biết luồng đăng nhập/refresh/logout: [backend/src/main/java/com/nexoracommerce/auth/controller/AuthController.java](backend/src/main/java/com/nexoracommerce/auth/controller/AuthController.java#L1).

- **Checkout & Bảo vệ tồn kho (Reserve on Checkout)**: Quy trình checkout dùng Redis để kiểm tra tồn kho trước, decrement atomic trên cache, tạo order (reserve trong DB) và rollback cả cache + DB khi thanh toán thất bại. Luồng chính: [backend/src/main/java/com/nexoracommerce/checkout/service/CheckoutService.java](backend/src/main/java/com/nexoracommerce/checkout/service/CheckoutService.java#L1).

- **Redis & Caching**: cấu hình Lettuce, `RedisTemplate` và `RedisCacheManager` cho caching và kết nối pool: [backend/src/main/java/com/nexoracommerce/config/database/RedisConfig.java](backend/src/main/java/com/nexoracommerce/config/database/RedisConfig.java#L1).

- **Thanh toán VNPAY (Sandbox)**: Tạo URL thanh toán chuẩn VNPAY (bao gồm `vnp_SecureHash` HMAC-SHA512) và xác thực IPN/Return từ VNPAY. Xem triển khai: [backend/src/main/java/com/nexoracommerce/payment/service/VnPayService.java](backend/src/main/java/com/nexoracommerce/payment/service/VnPayService.java#L1) và webhook IPN: [backend/src/main/java/com/nexoracommerce/payment/controller/PaymentController.java](backend/src/main/java/com/nexoracommerce/payment/controller/PaymentController.java#L1).

- **Lịch sử giao dịch & Trạng thái thanh toán**: Lưu chi tiết giao dịch (provider id, amount, status) trong `payment_transactions` để hiển thị lịch sử và badge trạng thái (PAID / UNPAID / REFUNDED): [backend/src/main/java/com/nexoracommerce/order/entity/PaymentTransaction.java](backend/src/main/java/com/nexoracommerce/order/entity/PaymentTransaction.java#L1).

- **Order APIs & Audit Trail**: Endpoints quản lý đơn hàng, preview tính giá, lịch sử user, chi tiết đơn hàng (kèm lịch sử giao dịch), và chuyển trạng thái đơn có audit: [backend/src/main/java/com/nexoracommerce/order/controller/OrderController.java](backend/src/main/java/com/nexoracommerce/order/controller/OrderController.java#L1).

- **Rate limiting & Security**: Một số endpoints (ví dụ `AuthController`) có `@RateLimited` để giảm rủi ro brute-force. Xem: [backend/src/main/java/com/nexoracommerce/auth/controller/AuthController.java](backend/src/main/java/com/nexoracommerce/auth/controller/AuthController.java#L1).


---

## 🔐 Bảo mật

### JWT Token Flow
1. **Login** → API trả về `accessToken` (15 phút) + `refreshToken` (7 ngày)
2. **Authenticated Requests** → Gửi `Authorization: Bearer <accessToken>`
3. **Token Expired** → Gửi `refreshToken` để lấy token mới
4. **Refresh Token Expired** → User phải đăng nhập lại

### Encryption & Hashing
- **Passwords**: BCrypt (10 rounds salt)
- **VNPAY**: HMAC-SHA512 signature verification
- **JWT**: HS256 (secret key 64+ chars)
- **Stored Tokens**: Indexed, time-limited

### Authorization (RBAC)
```
ADMIN
  ├─ Manage products (create, update, delete)
  ├─ View all orders
  ├─ Manage users & roles
  ├─ View reports & analytics
  └─ AI features management

USER
  ├─ View products
  ├─ Create orders (own only)
  ├─ Update own profile
  ├─ View own orders & payment history
  └─ Create reviews
```

---

## 🚀 Deployment

### Development
```bash
./scripts/fullstack/start.sh    # All services
docker-compose -f docker/docker-compose.dev.yml up
```

### Production (Docker)
```bash
# Build images
./scripts/fullstack/build.sh

# Run with production compose
docker-compose -f docker/docker-compose.prod.yml up -d

# View logs
docker-compose -f docker/docker-compose.prod.yml logs -f
```

### Production (Cloud)
Đối với Heroku, AWS, hoặc DigitalOcean, xem tài liệu riêng.

---

## 🧪 Testing

```bash
# Backend unit tests
cd backend
mvn test

# Frontend unit tests
cd frontend
pnpm test

# Integration tests
mvn verify

# Load testing
mvn gatling:execute
```

---

## 📚 Documentation

- **Database ERD**: [db/nexora_erd.dbml](backend/src/main/resources/db/nexora_erd.dbml)
- **API Spec**: [docs/swagger-openapi.yml](docs/swagger-openapi.yml)
- **Progress**: [task.md](task.md)
- **Roadmap**: (removed — README describes current project state only)

---

## 🤝 Contributing

Mỗi đóng góp đều được hoan nghênh! Vui lòng:

1. Fork repository
2. Tạo branch: `git checkout -b feature/your-feature`
3. Commit: `git commit -am 'Add feature'`
4. Push: `git push origin feature/your-feature`
5. Tạo Pull Request

---

## 📄 License

MIT License — xem [LICENSE](LICENSE) file

---

## 📞 Contact & Support

- **Author**: Bao Phan
- **GitHub**: [@XuanBao04](https://github.com/XuanBao04)
- **Project**: [nexora-commerce](https://github.com/XuanBao04/nexora-commerce)

---

**Cập nhật cuối**: 31/05/2026 · **Status**: Phase 1 (MVP)

---
## 👨‍💻 Tác giả

**Bao Phan** — [GitHub](https://github.com/XuanBao04)

> *Nexora Commerce là một phần của tầm nhìn xây dựng nền tảng Social Commerce — kết hợp e-commerce, mạng xã hội và AI tại Việt Nam.*
