# 🛒 Nexora Commerce

> AI-Powered E-Commerce Platform — Spring Boot · React · PostgreSQL/pgvector · Redis · VNPAY

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-blue?logo=react)](https://react.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue?logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red?logo=redis)](https://redis.io/)

---

## 📌 Giới thiệu

**Nexora Commerce** là nền tảng thương mại điện tử hiện đại tích hợp trí tuệ nhân tạo, được xây dựng với kiến trúc production-ready. Hệ thống bao gồm đầy đủ nghiệp vụ e-commerce cốt lõi, cổng thanh toán VNPAY, và 5 tính năng AI đột phá sử dụng **Gemini API** kết hợp **pgvector**.

---

## ✨ Tính năng nổi bật

### 🏪 E-Commerce Core
- Quản lý sản phẩm đa biến thể (màu sắc, dung lượng, kích cỡ)
- Giỏ hàng động, mã giảm giá, phí vận chuyển
- Hệ thống đơn hàng với lịch sử trạng thái đầy đủ (Audit log)
- **Redis Distributed Lock** — ngăn chặn overselling khi nhiều người mua cùng lúc
- **Reserve on Checkout** — giữ kho an toàn trong thời gian thanh toán

### 💳 Thanh toán VNPAY
- Tích hợp cổng thanh toán **VNPAY Sandbox**
- Xử lý ReturnURL (giao diện) và IPN (server-to-server bất đồng bộ)
- Tự động hoàn kho khi thanh toán thất bại

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

---

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
- Docker & Docker Compose
- Node.js 20+
- Java 21+ (nếu chạy local)

### 1. Clone & cấu hình môi trường
```bash
git clone https://github.com/XuanBao04/nexora-commerce.git
cd nexora-commerce

# Tạo file .env từ template
cp .env.example .env

# Chỉnh sửa các giá trị cần thiết (DB password, API keys...)
nano .env
```

### 2. Chạy toàn bộ stack bằng script
```bash
# Cấp quyền thực thi
chmod +x scripts/dev.sh

# Khởi chạy tất cả services (DB, Redis, Backend, Frontend)
./scripts/dev.sh
```

### 3. Truy cập
| Service | URL |
|---|---|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| pgvector DB | localhost:5432 |
| Redis | localhost:6379 |

---

## 📊 Database Schema (24 bảng)

```
Người dùng & Bảo mật     Danh mục & Sản phẩm       Đơn hàng & Thanh toán
──────────────────────   ────────────────────────   ──────────────────────────
users                    categories                 orders
roles                    brands                     order_items
user_roles               products                   order_status_history
user_addresses           product_variants ←pgvector payment_transactions
refresh_tokens           product_attributes         coupons
verification_tokens      product_attribute_values
                         product_images             Đánh giá & AI
                                                    ─────────────────
                         Giỏ hàng                   product_reviews
                         ─────────                  review_images
                         cart_items                 ai_chat_messages
```

---

## 📁 Cấu trúc dự án

```
nexora-commerce/
├── backend/                    # Spring Boot API
│   └── src/main/java/com/nexoracommerce/
│       ├── ai/                 # 5 AI features
│       ├── auth/               # JWT, login, register
│       ├── checkout/           # Checkout flow + Redis lock
│       ├── order/              # Orders, order items
│       ├── payment/            # VNPAY integration
│       ├── product/            # Products, variants, categories
│       ├── cart/               # Shopping cart
│       └── config/             # Security, CORS, Swagger
│
├── frontend/                   # React + TypeScript
│   └── src/
│       ├── pages/              # Route pages
│       ├── components/         # Reusable components
│       └── api/                # API service layer
│
├── scripts/                    # Dev scripts
│   └── dev.sh                  # Start all services
│
├── docker-compose.yml          # Production compose
├── docker-compose.dev.yml      # Development compose
└── .env.example                # Environment template
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

## 🗺️ Roadmap

- [x] 24-bảng PostgreSQL schema với pgvector
- [x] Java Entities & JPA Mapping đầy đủ
- [x] Flyway database migration
- [ ] VNPAY Sandbox integration
- [ ] 5 AI Features (Gemini + pgvector)
- [ ] React Frontend hoàn chỉnh
- [ ] Social Commerce integration *(tương lai)*

---

## 👨‍💻 Tác giả

**Bao Phan** — [GitHub](https://github.com/XuanBao04)

> *Nexora Commerce là một phần của tầm nhìn xây dựng nền tảng Social Commerce — kết hợp e-commerce, mạng xã hội và AI tại Việt Nam.*
