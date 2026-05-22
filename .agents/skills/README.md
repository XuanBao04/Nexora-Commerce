---
name: nexora-commerce-development-guide
description: Development guide aligned with Nexora Commerce phases - practical patterns for Phase 3-5 implementation.
---

## Project Overview

Nexora Commerce is a full-stack e-commerce platform with AI-powered features and Vietnamese payment integration (VNPAY).

**Tech Stack:**
- Backend: Spring Boot 3.x, Java 21, MySQL 8.0, pgvector (AI embeddings)
- Frontend: React 18+, Vite, TypeScript, Axios
- AI: Spring AI with Gemini LLM, semantic search
- Payment: VNPAY integration

---

## 🎯 Phase 3: E-Commerce Core + VNPAY Payment

### What to Focus On
1. **Order Management APIs** - Create order, list user orders, cancel pending orders
2. **VNPAY Integration** - Payment URL generation, callback handling, IPN validation
3. **Checkout Flow** - COD or VNPAY payment method selection
4. **Transaction Recording** - Save payment attempts + responses

### Key Files to Create
```
backend/
├── checkout/
│   ├── controller/CheckoutController.java
│   ├── dto/CheckoutRequest.java
│   └── service/CheckoutService.java
├── payment/
│   ├── controller/PaymentController.java
│   ├── config/VnpayProperties.java
│   ├── service/VnpayService.java
│   └── dto/VnpayCreatePaymentRequest.java
└── order/
    ├── controller/OrderController.java
    └── repository/PaymentTransactionRepository.java
```

### Backend Flow Diagram
```
User Action: "Proceed to Checkout"
         ↓
CheckoutController.checkout(paymentMethod)
         ↓
CheckoutService.checkout() → Create Order (status=PENDING)
         ↓
If VNPAY: VnpayService.createPaymentUrl() → Return redirect URL
If COD: Set status=CONFIRMED directly
         ↓
Frontend: Redirect to VNPAY or show confirmation
         ↓
[VNPAY redirects back] OR [Server receives IPN]
         ↓
PaymentController.handleCallback/handleIpn() → Verify signature
         ↓
VnpayService.processCallback() → Update Order status
         ↓
Save PaymentTransaction record
```

### Critical Implementation Notes
- **HMAC Validation**: Never skip signature verification
- **Idempotency**: Process IPN only once (check `payment_transactions` table)
- **Amount Handling**: VNPAY uses cents (multiply by 100)
- **Return vs IPN**: Both must update same order; IPN is definitive

### Skill Reference
→ See **vnpay-payment-integration** skill for complete code

---

## 🤖 Phase 4: AI Features with Spring AI + Gemini

### What to Focus On
1. **RAG Chatbot** - Chat with product context using embeddings
2. **Semantic Search** - Find products by meaning, not just keywords
3. **Content Generation** - Create SEO descriptions for products
4. **Sentiment Analysis** - Classify review sentiment

### Architecture
```
User Input: "Tìm laptop mỏng nhẹ dưới 15 triệu"
         ↓
Embed query → Search pgvector for similar product embeddings
         ↓
Load top-5 products + descriptions
         ↓
Build RAG prompt with context
         ↓
Call Gemini LLM with context
         ↓
Parse response → Extract product suggestions
         ↓
Return to user with product cards
```

### Key Implementation Points
- **Embeddings**: Generated once per product variant, stored in pgvector
- **Caching**: Cache semantic search results (TTL 1 hour)
- **Fallback**: If semantic search fails, fall back to keyword search
- **Token Control**: Set `max_output_tokens` to prevent runaway responses
- **System Prompt**: Define AI behavior in Vietnamese

### Skill Reference
→ See **spring-ai-vector-search** skill for complete implementation

---

## ⚛️ Phase 5: React Frontend Integration

### What to Focus On
1. **Product Catalog** - Grid display with semantic search
2. **Cart Management** - Add/remove items, quantity control
3. **Checkout Page** - Payment method selection, order summary
4. **Order History** - List past orders, view details
5. **AI Chat Widget** - Floating chat for product recommendations

### Frontend Architecture
```
App
├── Providers
│   ├── AuthProvider (JWT + refresh token)
│   └── CartProvider (Global cart context)
├── Routes
│   ├── /products (ProductListPage)
│   ├── /products/:id (ProductDetailPage)
│   ├── /cart (CartPage)
│   ├── /checkout (CheckoutPage)
│   ├── /payment/result (PaymentResultPage)
│   └── /orders (OrderHistoryPage)
└── Components
    ├── AiChatWidget (floating chat)
    ├── ProductGrid (with lazy loading)
    └── SearchBar (semantic search)
```

### Critical Patterns
- **API Calls in useEffect**: Never fetch in render
- **Token Refresh**: Auto-refresh on 401 response
- **Cart Persistence**: Save cart to localStorage for offline access
- **Payment Callback**: Read query params from VNPAY redirect
- **Error Handling**: Show user-friendly error messages

### Skill Reference
→ See **frontend-react-essentials** skill for hooks, API layer, components

---

## 🏗️ Backend Architecture (All Phases)

### Layered Structure
```
Controller Layer (REST endpoints)
        ↓ (request/response DTOs)
Service Layer (business logic, transactions)
        ↓ (entities)
Repository Layer (JPA queries)
        ↓
Database (MySQL + pgvector)
```

### Key Rules
1. **Never expose JPA entities** in API responses (always map to DTO)
2. **Transactional boundaries** only at service level
3. **Constructor injection only** (no `@Autowired` on fields)
4. **Lazy loading by default** for relationships
5. **JOIN FETCH** for complex queries to prevent N+1

### Skill Reference
→ See **backend-architecture-essentials** for core patterns

---

## 🚀 Development Workflow

### Getting Started
```bash
# 1. Clone and setup
cd /home/bao_phan/Projects/Nexora-commerce
docker-compose up -d

# 2. Run migrations (Flyway auto-runs)
# Check: docker logs nexora-mysql

# 3. Start backend
cd backend
mvn spring-boot:run

# 4. Start frontend
cd frontend
npm run dev

# 5. Access app
# Frontend: http://localhost:5173
# Backend: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html
```

### Code Review Checklist
- ✅ All API responses are DTOs (not entities)
- ✅ All async operations have loading + error states
- ✅ All transactions use `@Transactional` at service level
- ✅ All API calls use base client with interceptors
- ✅ All custom queries have `@Query` annotation
- ✅ All signatures validated (VNPAY callbacks)
- ✅ No `any` types in TypeScript
- ✅ No `console.log()` left in production code

### Testing Phase-by-Phase

**Phase 3 - VNPAY:**
```bash
# Create test order
curl -X POST http://localhost:8080/api/checkout \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"paymentMethod":"VNPAY"}'

# Verify payment URL generation
# Get returned paymentUrl, visit it
# Complete payment in VNPAY sandbox

# Check order status updated
curl -X GET http://localhost:8080/api/orders/$ORDER_ID
```

**Phase 4 - AI Features:**
```bash
# Test semantic search
curl -X GET "http://localhost:8080/api/ai/search?q=laptop"

# Test chatbot
curl -X POST http://localhost:8080/api/ai/chat \
  -d '{"message":"Tìm laptop gaming","sessionId":"sess-1"}'

# Check embeddings in pgvector
psql nexora_db -c "SELECT COUNT(*) FROM product_variants WHERE embedding IS NOT NULL;"
```

**Phase 5 - Frontend:**
```bash
# Build check
npm run build

# Type check
npx tsc --noEmit

# Test API integration
npm test -- services/api
```

---

## ⚠️ Common Pitfalls

| Pitfall | Fix |
|---------|-----|
| VNPAY hash mismatch on callback | Verify params sorted alphabetically, use correct HashSecret |
| N+1 queries in product listing | Use `@Query` with `LEFT JOIN FETCH` for relationships |
| Token expired during checkout | Implement auto-refresh in API interceptor |
| Cart data lost on page refresh | Persist cart to localStorage or session |
| AI chatbot slow responses | Cache embeddings, set reasonable token limits |
| Semantic search returns poor results | Ensure product descriptions are detailed and indexed |

---

## 📚 Skill Quick Reference

| Skill | Purpose | When to Use |
|-------|---------|------------|
| `backend-architecture-essentials` | Core Spring Boot patterns | Creating services, repositories, APIs |
| `vnpay-payment-integration` | VNPAY payment flow | Phase 3 checkout + payment handling |
| `spring-ai-vector-search` | AI + embeddings + Gemini | Phase 4 chatbot, search, content generation |
| `frontend-react-essentials` | React hooks, API, components | Phase 5 frontend implementation |

---

## 🔗 Environment Variables

### Backend (.env or docker-compose)
```
MYSQL_ROOT_PASSWORD=rootpassword
MYSQL_DATABASE=nexora_db
MYSQL_USER=nexora_user
MYSQL_PASSWORD=nexora_password

JWT_SECRET=your-secret-key-here
VNPAY_TMN_CODE=your-tmn-code
VNPAY_HASH_SECRET=your-hash-secret
GEMINI_API_KEY=your-gemini-key

SPRING_PROFILES_ACTIVE=dev
```

### Frontend (.env.local)
```
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_NAME=Nexora Commerce
```

---

## 📞 When to Ask for Help

The skills in this system are designed to be **self-contained**. Reference them directly:

- **"How do I create a Spring Boot service?"** → Read `backend-architecture-essentials`
- **"How do I integrate VNPAY?"** → Read `vnpay-payment-integration`
- **"How do I build an AI chatbot?"** → Read `spring-ai-vector-search`
- **"How do I fetch data in React?"** → Read `frontend-react-essentials`

Each skill has:
1. Production code examples
2. DTO definitions
3. Controller/Service patterns
4. Verification commands

---

## 🎓 Helpful Patterns

### Always Use This Structure for CRUD Operations
```java
@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
public class ResourceController {
    private final ResourceService service;
    
    @PostMapping
    public ResponseEntity<ResourceResponse> create(@Valid @RequestBody CreateRequest req) {
        return ResponseEntity.status(201).body(service.create(req));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ResourceResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ResourceResponse> update(@PathVariable String id, @Valid @RequestBody UpdateRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### Always Use This Pattern for React Components
```typescript
export const MyComponent: React.FC<MyComponentProps> = React.memo(
  ({ prop1, prop2, onAction }) => {
    const [state, setState] = useState<StateType>(initialValue);
    const { data, loading, error } = useAsync<DataType>();

    useEffect(() => {
      // Setup or data fetch
      return () => {
        // Cleanup
      };
    }, [dependencies]);

    if (loading) return <LoadingSpinner />;
    if (error) return <ErrorAlert message={error} />;

    return (
      <div className="container">
        {/* Component JSX */}
      </div>
    );
  }
);

MyComponent.displayName = 'MyComponent';
```

---

## ✅ Success Criteria per Phase

**Phase 3 Complete When:**
- ✅ User can select COD or VNPAY at checkout
- ✅ VNPAY payment URL generated and redirects correctly
- ✅ Payment callback validates signature
- ✅ Order status updates after successful payment
- ✅ Failed payments handled gracefully

**Phase 4 Complete When:**
- ✅ Products have embeddings stored in pgvector
- ✅ Semantic search returns relevant results
- ✅ Chatbot responds with product recommendations
- ✅ Content generation creates SEO descriptions

**Phase 5 Complete When:**
- ✅ Frontend fetches and displays products
- ✅ Cart add/remove items works
- ✅ Checkout flow integrates with backend
- ✅ Payment result page shows success/failure
- ✅ Order history shows past orders
- ✅ AI chat widget floats and responds

---

**Last Updated:** May 22, 2026 | **Version:** 1.0 | **Status:** Production Ready
