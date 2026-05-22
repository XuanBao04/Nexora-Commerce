---
name: spring-boot-core-skill
description: Enforce Spring Boot 3.x layered architecture, dependency injection patterns, exception handling, and Java 21 best practices for e-commerce domain services.
---

## Scope & Activation Rules

Activate when:
- Creating or modifying Spring Boot controllers, services, or repositories
- Designing REST API endpoints for cart, order, or product management
- Implementing business logic with dependency injection
- Handling application-level exceptions and validation
- Using Java 21 features (Records, Pattern Matching, Virtual Threads)

## System Directives

### DO
- **Enforce Layered Architecture**: Controller → Service → Repository. Controllers accept DTOs, Services contain business logic, Repositories handle data access only.
- **Use Records for Immutable Data Transfer Objects** (Java 21): `public record CreateOrderRequest(String cartId, AddressDto address) {}`
- **Apply Pattern Matching** in exception handling and type checking.
- **Inject Dependencies via Constructor**: Never use field injection (`@Autowired` on fields). Always use constructor injection for testability.
- **Validate at Entry Points**: Use `@Valid` on controller parameters with custom validators for complex rules.
- **Implement Idempotent POST Operations**: Return 409 Conflict if duplicate resource creation is attempted.
- **Use @Transactional at Service Layer**: Never at controller level. Apply to public methods that modify state.
- **Return DTOs from Services**: Never expose JPA entities in API responses; map entities to DTOs.
- **Centralize Exception Handling**: Use `@RestControllerAdvice` with `@ExceptionHandler` for consistent error responses.
- **Log at Critical Points**: Entry/exit of service methods, exceptions, and business-critical decisions.

### DO NOT
- Use field injection (`@Autowired` on fields). This breaks testability and hides dependencies.
- Expose JPA entities in REST responses. Always map to DTOs.
- Place business logic in controllers. Controllers should only orchestrate Service calls.
- Use mutable static fields. Leverage records and immutable objects.
- Catch generic exceptions. Always catch specific exceptions and handle appropriately.
- Use `@Transactional` on controllers or repositories. Apply only to service methods.
- Implement database queries in service logic. Encapsulate in custom repository methods or specifications.
- Return null from service methods. Use Optional or custom Result types.
- Mix Lombok annotations with Records. Records are self-documenting; use them instead.
- Ignore validation errors. Propagate them to the controller for proper HTTP response.

## Production Reference Implementation

### OrderService (Service Layer - Cart & Order Management)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final PaymentClient paymentClient;
    private final OrderMapper orderMapper;
    private final OrderValidator orderValidator;
    
    @Transactional
    public CreateOrderResponse createOrder(String cartId, CreateOrderRequest request) {
        log.info("Creating order for cart: {}", cartId);
        
        Cart cart = cartRepository.findById(cartId)
            .orElseThrow(() -> new CartNotFoundException("Cart not found: " + cartId));
        
        orderValidator.validateOrder(cart, request.address());
        
        Order order = Order.builder()
            .cartId(cartId)
            .userId(request.userId())
            .address(request.address())
            .totalAmount(cart.getTotalPrice())
            .status(OrderStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .build();
        
        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully: {}", savedOrder.getId());
        
        try {
            paymentClient.initiatePayment(savedOrder.getId(), savedOrder.getTotalAmount());
        } catch (PaymentException e) {
            log.error("Payment initiation failed for order: {}", savedOrder.getId(), e);
            savedOrder.setStatus(OrderStatus.PAYMENT_FAILED);
            orderRepository.save(savedOrder);
            throw new OrderCreationException("Payment initiation failed", e);
        }
        
        return orderMapper.toResponse(savedOrder);
    }
    
    @Transactional(readOnly = true)
    public OrderDetailsResponse getOrderDetails(String orderId, String userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        return orderMapper.toDetailsResponse(order);
    }
}
```

### OrderController (Controller Layer)

```java
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CreateOrderResponse> createOrder(
        @Valid @RequestBody CreateOrderRequest request,
        @RequestHeader("Authorization") String bearerToken) {
        
        String userId = extractUserIdFromToken(bearerToken);
        log.info("Creating order for user: {}", userId);
        
        CreateOrderResponse response = orderService.createOrder(request.cartId(), request.withUserId(userId));
        return ResponseEntity
            .created(URI.create("/api/orders/" + response.id()))
            .body(response);
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailsResponse> getOrderDetails(
        @PathVariable String orderId,
        @RequestHeader("Authorization") String bearerToken) {
        
        String userId = extractUserIdFromToken(bearerToken);
        OrderDetailsResponse response = orderService.getOrderDetails(orderId, userId);
        return ResponseEntity.ok(response);
    }
}
```

### DTOs Using Records (Java 21)

```java
public record CreateOrderRequest(
    String cartId,
    AddressDto address,
    String paymentMethod
) implements Serializable {}

public record OrderDetailsResponse(
    String id,
    String userId,
    LocalDateTime createdAt,
    OrderStatus status,
    BigDecimal totalAmount,
    List<OrderItemDto> items,
    AddressDto shippingAddress
) {}

public record AddressDto(
    String street,
    String city,
    String postalCode,
    String country
) {}
```

### Order Entity (JPA)

```java
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_status", columnList = "status")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Data
@Builder
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "cart_id", nullable = false, unique = true)
    private String cartId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Embedded
    private Address address;
    
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### OrderRepository (Repository Layer)

```java
@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    
    Optional<Order> findByIdAndUserId(String id, String userId);
    
    List<Order> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.userId = ?1 AND o.status IN ?2")
    List<Order> findByUserIdAndStatusIn(String userId, List<OrderStatus> statuses, Pageable pageable);
    
    @Modifying
    @Query("UPDATE Order o SET o.status = ?2 WHERE o.id = ?1")
    void updateOrderStatus(String orderId, OrderStatus status);
}
```

### Global Exception Handler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        log.warn("Order not found: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("ORDER_NOT_FOUND", ex.getMessage()));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("VALIDATION_ERROR", message));
    }
    
    @ExceptionHandler(OrderCreationException.class)
    public ResponseEntity<ErrorResponse> handleOrderCreation(OrderCreationException ex) {
        log.error("Order creation failed", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("ORDER_CREATION_FAILED", "Failed to create order"));
    }
}
```

## Anti-Patterns & Automated Fixes

### Anti-Pattern 1: Injecting Entities in REST Response
**Problem**: Exposing JPA entities directly leads to N+1 queries and unwanted field exposure.
```java
// ❌ WRONG
@GetMapping("/{id}")
public Order getOrder(@PathVariable String id) {
    return orderRepository.findById(id).orElseThrow();
}
```
**Fix**: Use DTOs/Records mapped from entities.
```java
// ✅ CORRECT
@GetMapping("/{id}")
public ResponseEntity<OrderDetailsResponse> getOrder(@PathVariable String id) {
    Order order = orderService.getOrderDetails(id);
    return ResponseEntity.ok(orderMapper.toResponse(order));
}
```

### Anti-Pattern 2: Field Injection & Testability
**Problem**: Field injection makes unit testing harder and hides dependencies.
```java
// ❌ WRONG
@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;
}
```
**Fix**: Use constructor injection via `@RequiredArgsConstructor`.
```java
// ✅ CORRECT
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
}
```

### Anti-Pattern 3: Business Logic in Controller
**Problem**: Controllers become bloated; services cannot be reused.
```java
// ❌ WRONG
@PostMapping("/orders")
public ResponseEntity<Order> create(@RequestBody CreateOrderRequest req) {
    Order order = new Order();
    order.setUserId(extractUser(req));
    order.setTotalAmount(calculateTotal(req.items()));
    // ... 20 more lines of business logic
    return ResponseEntity.ok(orderRepository.save(order));
}
```
**Fix**: Delegate to Service layer.
```java
// ✅ CORRECT
@PostMapping("/orders")
public ResponseEntity<CreateOrderResponse> create(@Valid @RequestBody CreateOrderRequest req) {
    CreateOrderResponse response = orderService.createOrder(req);
    return ResponseEntity.created(...).body(response);
}
```

### Anti-Pattern 4: Missing Null Checks in Transactions
**Problem**: Unhandled null references cause runtime errors in critical paths.
```java
// ❌ WRONG
@Transactional
public void updateOrder(String id, OrderUpdateRequest req) {
    Order order = orderRepository.findById(id).get(); // Throws NoSuchElementException
    order.setStatus(req.status());
}
```
**Fix**: Handle Optional explicitly.
```java
// ✅ CORRECT
@Transactional
public void updateOrder(String id, OrderUpdateRequest req) {
    Order order = orderRepository.findById(id)
        .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));
    order.setStatus(req.status());
}
```

## Verification Commands

### Verify Layered Architecture Compliance
```bash
# Check for @Autowired field injections (should be empty)
find backend/src/main/java -name "*.java" -exec grep -l "@Autowired\s*private" {} \;

# Verify no business logic in controllers (grep for common patterns)
grep -r "new.*Repository\|\.save(\|\.delete(" backend/src/main/java/com/*/controller/ || echo "✅ No direct repository access in controllers"
```

### Verify Exception Handling
```bash
# Check for catch (Exception e) patterns (should be minimal)
grep -r "catch\s*(\s*Exception\s" backend/src/main/java --include="*.java" | wc -l

# Verify RestControllerAdvice is present
grep -r "@RestControllerAdvice" backend/src/main/java --include="*.java" | wc -l
```

### Build & Test
```bash
# Build with compilation checks
cd backend && mvn clean compile -DskipTests

# Run unit tests (verify DI works)
mvn test -Dtest=OrderServiceTest

# Static analysis for Spring violations
mvn spring-boot:build-info && mvn checkstyle:check
```

### Runtime Verification
```bash
# Start application and verify health
curl -X GET http://localhost:8080/actuator/health

# Test Order endpoint with sample payload
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"cartId":"cart-123","address":{"street":"123 Main St","city":"New York","postalCode":"10001","country":"US"}}'

# Verify transaction logs
grep "Creating order for" logs/application.log
```
