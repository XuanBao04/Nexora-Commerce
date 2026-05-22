---
name: backend-architecture-essentials
description: Spring Boot 3.x core patterns, JPA layer optimization, and structured exception handling for Nexora Commerce e-commerce backend.
---

## Scope & Activation Rules

Activate when:
- Creating service layer classes, repositories, or entity models
- Implementing REST endpoints for cart, order, product management
- Optimizing JPA queries to prevent N+1 issues
- Handling business logic with transactional boundaries

## System Directives

### Core Patterns
- **Layered Architecture**: Controller → Service → Repository (clear separation of concerns)
- **Constructor Injection Only**: No `@Autowired` on fields; use `@RequiredArgsConstructor` for DI
- **DTOs for API Contracts**: Never expose JPA entities in responses; map entities to DTOs
- **Transactional Boundaries**: `@Transactional` on service methods only, never on controllers
- **Custom Queries for Complex Logic**: Use `@Query` with JPQL for anything beyond simple method names
- **Lazy Loading by Default**: Set `fetch = FetchType.LAZY` on all relationships; use JOIN FETCH only when necessary
- **Service Return Types**: Use `Optional<>` for single records; `Page<>` for paginated results; throw exceptions on invalid states
- **Validation at Entry Points**: Use `@Valid` on controller parameters; custom validators for complex rules

### Anti-Patterns to Avoid
- No field injection with `@Autowired`
- No business logic in controllers
- No direct JPA entities in API responses (always map to DTO)
- No catching generic `Exception`
- No `@Transactional` on read-only operations without `readOnly = true`
- No N+1 queries (use JOIN FETCH, projections, or batch operations)

## Production Example: Order Management

### Entity Layer
```java
@Entity
@Table(name = "orders", indexes = @Index(name = "idx_user_id", columnList = "user_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Data
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String userId;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "order")
    private List<OrderItem> items = new ArrayList<>();
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

### Repository Layer
```java
@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.userId = ?1 ORDER BY o.createdAt DESC")
    Page<Order> findByUserIdWithItems(String userId, Pageable pageable);
    
    Optional<Order> findByIdAndUserId(String id, String userId);
    
    @Modifying
    @Query("UPDATE Order o SET o.status = ?2 WHERE o.id = ?1")
    void updateStatus(String id, OrderStatus status);
}
```

### Service Layer
```java
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final OrderMapper mapper;
    
    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderId, String userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        return mapper.toResponse(order);
    }
    
    public void cancelOrder(String orderId, String userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        
        if (!order.getStatus().equals(OrderStatus.PENDING)) {
            throw new IllegalStateException("Can only cancel pending orders");
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Order cancelled: {}", orderId);
    }
}
```

### Controller Layer
```java
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String id, @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(orderService.getOrder(id, userId));
    }
    
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable String id, @AuthenticationPrincipal String userId) {
        orderService.cancelOrder(id, userId);
        return ResponseEntity.noContent().build();
    }
}
```

### DTO Records
```java
public record OrderResponse(
    String id,
    OrderStatus status,
    BigDecimal totalAmount,
    LocalDateTime createdAt,
    List<OrderItemDto> items
) {}

public record OrderItemDto(
    String productVariantId,
    String productName,
    int quantity,
    BigDecimal price
) {}
```

## Verification Commands

```bash
# Check no field injections
grep -r "@Autowired" backend/src/main/java --include="*.java" | grep -v "//" | wc -l

# Check all queries use @Query
grep -r "findBy[A-Z]" backend/src/main/java/*/repository --include="*.java" | wc -l

# Build and run
cd backend && mvn clean package -DskipTests
```
