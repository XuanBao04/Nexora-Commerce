---
name: spring-data-jpa-skill
description: Enforce optimal JPA entity design, query optimization, N+1 prevention, pagination, and lazy loading strategies for high-throughput e-commerce operations.
---

## Scope & Activation Rules

Activate when:
- Defining JPA entities, relationships, and inheritance strategies
- Writing custom repository queries (JPQL, native SQL, QueryDSL)
- Implementing filtering, sorting, and pagination
- Configuring fetch strategies (lazy vs. eager loading)
- Optimizing queries to prevent N+1 problems
- Managing transactions and batch operations
- Designing database indices and constraints

## System Directives

### DO
- **Use Lazy Loading by Default**: Set `fetch = FetchType.LAZY` on all relationships. Load eagerly only when necessary and documented.
- **Implement Custom Repository Methods**: Use `@Query` for complex queries instead of method name inference.
- **Always Paginate Large Result Sets**: Use `Pageable` parameter; never load all records into memory.
- **Add Database Indices on Foreign Keys**: Index columns used in WHERE clauses, JOINs, and ORDER BY.
- **Use `@Transactional(readOnly = true)` for Queries**: Prevents unnecessary write locks and improves performance.
- **Batch Insert/Update Operations**: Use `saveAll()` or `@Modifying @Query` for bulk operations.
- **Map Correct Column Names**: Use `@Column(name = "db_column_name")` for explicit mapping.
- **Define Entity Constraints at Database Level**: Use `@Column(nullable = false)`, `@Unique`, `@Index`.
- **Use DTOs for Projections**: Return only required fields via custom JPQL queries or Projections interface.
- **Implement `@SqlResultSetMapping` for Complex Queries**: Use for native SQL with complex result structures.
- **Override `hashCode()` and `equals()`** for entities used in collections, based on business identifier (ID).
- **Use Audit Annotations**: `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy` via `@EnableJpaAuditing`.
- **Validate Data at Persistence Layer**: Use JSR-303 annotations (`@NotNull`, `@NotBlank`, etc.).

### DO NOT
- Use eager loading (`fetch = FetchType.EAGER`) without strong justification. This causes performance issues at scale.
- Write repository methods with inferred names for complex queries. Use `@Query` explicitly.
- Load full entities when only ID or specific fields are needed. Use projections or DTOs.
- Execute queries in loops. Batch them or use JOIN FETCH to load related data in a single query.
- Use `@OneToMany` without specifying cascade rules. Be explicit: cascade which operations?
- Skip indices on frequently queried columns. Always add `@Index` to foreign keys and filters.
- Persist circular references without configured cascade. This can cause infinite loops.
- Use mutable entities in collections without proper `hashCode()` and `equals()` implementation.
- Execute native SQL without parameterization. Always use bound parameters to prevent SQL injection.
- Skip `@Transactional(readOnly = true)` on query methods. This is a critical optimization.
- Use `Optional.get()` without checking `isPresent()`. Always handle empty Optional properly.
- Leave entities with mutable default values (e.g., `new ArrayList<>()`). Use collection factory methods.
- Use `JOIN FETCH` for `@OneToMany` or `@ManyToMany` relationships inside queries that return a `Page<T>` object to prevent Hibernate from triggering inefficient in-memory pagination.
- Directly reassign the collection reference of a `@OneToMany(orphanRemoval = true)` relationship (e.g., `this.items = newItems`). Always use `.clear()` and `.addAll()` instead to prevent Hibernate from losing track of managed entities and throwing orphan-related exceptions.

## Production Reference Implementation

### Product Entity (E-commerce Domain)

```java
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_category_id", columnList = "category_id"),
    @Index(name = "idx_sku", columnList = "sku", unique = true),
    @Index(name = "idx_active", columnList = "is_active"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Data
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"category", "inventory", "reviews"})
public class Product extends AuditableEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false, length = 255)
    @NotBlank(message = "Product name is required")
    private String name;
    
    @Column(length = 1000)
    private String description;
    
    @Column(nullable = false, unique = true, length = 50)
    @NotBlank(message = "SKU is required")
    private String sku;
    
    @Column(nullable = false, precision = 10, scale = 2)
    @DecimalMin("0.01")
    private BigDecimal price;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cost;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "product")
    private ProductInventory inventory;
    
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "product", orphanRemoval = true)
    private List<ProductReview> reviews = new ArrayList<>();
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    public void addReview(ProductReview review) {
        reviews.add(review);
        review.setProduct(this);
    }
    
    public void updateInventory(int quantity) {
        if (inventory == null) {
            inventory = ProductInventory.builder().product(this).build();
        }
        inventory.setQuantityOnHand(quantity);
    }
}
```

### Cart Entity with Order Items

```java
@Entity
@Table(name = "carts", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_status", columnList = "status")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Data
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "items")
public class Cart extends AuditableEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "user_id", nullable = false)
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CartStatus status;
    
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "cart", orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;
    
    @Column(nullable = false)
    private Integer itemCount;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
        recalculate();
    }
    
    public void removeItem(String itemId) {
        items.removeIf(item -> item.getId().equals(itemId));
        recalculate();
    }
    
    public void recalculate() {
        this.totalPrice = items.stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.itemCount = items.stream().mapToInt(CartItem::getQuantity).sum();
    }
}
```

### CartItem (Child Entity)

```java
@Entity
@Table(name = "cart_items", indexes = {
    @Index(name = "idx_cart_id", columnList = "cart_id"),
    @Index(name = "idx_product_id", columnList = "product_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Data
@Builder
@EqualsAndHashCode(of = "id")
public class CartItem extends AuditableEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    @JsonBackReference
    private Cart cart;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(nullable = false)
    @Min(1)
    private Integer quantity;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

### ProductRepository with Optimized Queries

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, String>, JpaSpecificationExecutor<Product> {
    
    // Simple query with pagination
    Page<Product> findByIsActiveTrue(Pageable pageable);
    
    // JOIN FETCH to prevent N+1 (Custom query)
    @Query("SELECT DISTINCT p FROM Product p " +
           "JOIN FETCH p.category c " +
           "WHERE p.isActive = true AND c.id = ?1 " +
           "ORDER BY p.createdAt DESC")
    List<Product> findActiveByCategoryWithCategory(String categoryId, Pageable pageable);
    
    // Projection to fetch only required fields
    @Query(value = "SELECT new com.nexora.product.dto.ProductSummaryDto(" +
           "p.id, p.name, p.price, p.inventory.quantityOnHand) " +
           "FROM Product p WHERE p.isActive = true")
    Page<ProductSummaryDto> findAllActiveSummaries(Pageable pageable);
    
    // Native query with parameterized input
    @Query(value = "SELECT * FROM products WHERE category_id = ? AND is_active = true " +
           "ORDER BY price ASC LIMIT ?", nativeQuery = true)
    List<Product> findByCategoryOrderByPrice(String categoryId, int limit);
    
    // Find by SKU (indexed column)
    Optional<Product> findBySku(String sku);
    
    // Bulk update
    @Modifying
    @Query("UPDATE Product p SET p.isActive = false WHERE p.id IN ?1")
    void deactivateProducts(List<String> productIds);
    
    // Check existence without loading entity
    boolean existsBySkuAndIdNot(String sku, String productId);
    
    // Pagination with filtering
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
```

### CartRepository with Advanced Queries

```java
@Repository
public interface CartRepository extends JpaRepository<Cart, String> {
    
    // Find cart with all items loaded in single query
    @Query("SELECT DISTINCT c FROM Cart c " +
           "LEFT JOIN FETCH c.items ci " +
           "LEFT JOIN FETCH ci.product p " +
           "WHERE c.id = ?1 AND c.userId = ?2")
    Optional<Cart> findByIdAndUserIdWithItems(String cartId, String userId);
    
    // Find active carts for a user with pagination
    Page<Cart> findByUserIdAndStatus(String userId, CartStatus status, Pageable pageable);
    
    // Cleanup abandoned carts (older than 7 days)
    @Modifying
    @Query("DELETE FROM Cart c WHERE c.status = 'ABANDONED' " +
           "AND c.updatedAt < CURRENT_TIMESTAMP - INTERVAL '7 day'")
    int deleteAbandonedCarts();
    
    // Count active carts per user
    @Query("SELECT COUNT(c) FROM Cart c WHERE c.userId = ?1 AND c.status = 'ACTIVE'")
    long countActiveCartsByUserId(String userId);
}
```

### Product Specification for Complex Filtering

```java
public class ProductSpecifications {
    
    public static Specification<Product> active() {
        return (root, query, cb) -> cb.equal(root.get("isActive"), true);
    }
    
    public static Specification<Product> byCategory(String categoryId) {
        return (root, query, cb) -> {
            Join<Product, Category> categoryJoin = root.join("category", JoinType.INNER);
            return cb.equal(categoryJoin.get("id"), categoryId);
        };
    }
    
    public static Specification<Product> priceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> cb.between(root.get("price"), minPrice, maxPrice);
    }
    
    public static Specification<Product> nameContains(String keyword) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%");
    }
}

// Usage in service
Page<Product> products = productRepository.findAll(
    Specification.where(ProductSpecifications.active())
        .and(ProductSpecifications.byCategory(categoryId))
        .and(ProductSpecifications.priceRange(minPrice, maxPrice)),
    PageRequest.of(page, size, Sort.by("price").ascending())
);
```

### AuditableEntity Base Class

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class AuditableEntity {
    
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;
    
    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;
    
    @LastModifiedBy
    @Column(name = "last_modified_by")
    private String lastModifiedBy;
    
    @LastModifiedDate
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;
}
```

### Service Layer with Transaction Management

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final CartMapper cartMapper;
    
    @Transactional(readOnly = true)
    public CartDetailResponse getCartWithItems(String cartId, String userId) {
        log.debug("Fetching cart: {} for user: {}", cartId, userId);
        
        Cart cart = cartRepository.findByIdAndUserIdWithItems(cartId, userId)
            .orElseThrow(() -> new CartNotFoundException("Cart not found"));
        
        return cartMapper.toDetailResponse(cart);
    }
    
    @Transactional
    public void addItemToCart(String cartId, String productId, int quantity) {
        Cart cart = cartRepository.findById(cartId)
            .orElseThrow(() -> new CartNotFoundException("Cart not found"));
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        
        if (product.getInventory() == null || product.getInventory().getQuantityOnHand() < quantity) {
            throw new InsufficientInventoryException("Not enough stock available");
        }
        
        CartItem item = CartItem.builder()
            .product(product)
            .quantity(quantity)
            .price(product.getPrice())
            .build();
        
        cart.addItem(item);
        cartRepository.save(cart);
        log.info("Item added to cart: {}, product: {}, quantity: {}", cartId, productId, quantity);
    }
    
    @Transactional
    public void batchAddItems(String cartId, List<AddItemRequest> items) {
        Cart cart = cartRepository.findById(cartId)
            .orElseThrow(() -> new CartNotFoundException("Cart not found"));
        
        items.forEach(item -> {
            Product product = productRepository.findById(item.productId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + item.productId()));
            
            CartItem cartItem = CartItem.builder()
                .product(product)
                .quantity(item.quantity())
                .price(product.getPrice())
                .build();
            
            cart.addItem(cartItem);
        });
        
        cartRepository.save(cart);
        log.info("Batch added {} items to cart: {}", items.size(), cartId);
    }
}
```

## Anti-Patterns & Automated Fixes

### Anti-Pattern 1: N+1 Query Problem with Lazy Loading
**Problem**: Iterating over relationships loads each child record in a separate query.
```java
// ❌ WRONG
List<Product> products = productRepository.findAll();
products.forEach(p -> {
    System.out.println(p.getCategory().getName()); // Triggers separate query per product
});
```
**Fix**: Use JOIN FETCH or projections to load in single query.
```java
// ✅ CORRECT
@Query("SELECT DISTINCT p FROM Product p JOIN FETCH p.category WHERE p.isActive = true")
List<Product> findAllWithCategory();
```

### Anti-Pattern 2: Eager Loading Everywhere
**Problem**: Eager loading cascades through relationships, loading entire entity graph.
```java
// ❌ WRONG
@OneToMany(fetch = FetchType.EAGER)
private List<CartItem> items;

@ManyToOne(fetch = FetchType.EAGER)
private Category category;
```
**Fix**: Use lazy loading and explicit JOIN FETCH only when needed.
```java
// ✅ CORRECT
@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
private List<CartItem> items;

@ManyToOne(fetch = FetchType.LAZY)
private Category category;
```

### Anti-Pattern 3: Loading Full Entities for Simple Lookups
**Problem**: Selecting entire entity when only ID is needed.
```java
// ❌ WRONG
Product product = productRepository.findById(id).get();
return product.getId(); // Loaded entire entity for one field
```
**Fix**: Use projections to fetch only required fields.
```java
// ✅ CORRECT
@Query("SELECT p.id FROM Product p WHERE p.id = ?1")
Optional<String> findIdById(String id);
```

### Anti-Pattern 4: Mutable Entity Collections Without Proper Equals
**Problem**: Entities in collections lose identity when modified.
```java
// ❌ WRONG
@Entity
public class Product {
    @OneToMany(mappedBy = "product")
    private List<ProductReview> reviews; // No hashCode/equals override
}
```
**Fix**: Implement `hashCode()` and `equals()` based on business ID.
```java
// ✅ CORRECT
@Entity
@EqualsAndHashCode(of = "id")
public class Product {
    @OneToMany(mappedBy = "product")
    private List<ProductReview> reviews;
}
```

### Anti-Pattern 5: No Pagination on Large Result Sets
**Problem**: Loading millions of records into memory causes OutOfMemoryError.
```java
// ❌ WRONG
List<Order> allOrders = orderRepository.findAll();
```
**Fix**: Always paginate.
```java
// ✅ CORRECT
Page<Order> ordersPage = orderRepository.findAll(PageRequest.of(0, 20));
```

## Verification Commands

### Verify Lazy Loading Configuration
```bash
# Check all relationships have explicit fetch type
grep -r "@OneToMany\|@ManyToOne\|@OneToOne" backend/src/main/java --include="*.java" | grep -v "fetch = FetchType.LAZY" | wc -l

# Should return 0 or only justified cases with comments
```

### Verify Indices on Foreign Keys
```bash
# Check for @Index on foreign key columns
grep -B 2 "@JoinColumn" backend/src/main/java --include="*.java" | grep "@Index" || echo "⚠️ Consider adding indices on foreign key columns"
```

### Run Query Performance Analysis
```bash
# Build and start app with SQL logging
cd backend && mvn clean package -DskipTests
SPRING_JPA_SHOW_SQL=true SPRING_JPA_PROPERTIES_HIBERNATE_FORMAT_SQL=true \
java -jar target/nexora-commerce-backend-1.0.0.jar &

# Make requests and check for N+1 patterns (repeated SELECT queries)
curl -X GET "http://localhost:8080/api/products?page=0&size=10" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq

# Count SQL queries in logs
grep "SELECT" logs/application.log | sort | uniq -c
```

### Verify Transaction Boundaries
```bash
# Check @Transactional(readOnly = true) on query methods
grep -A 3 "@Query" backend/src/main/java --include="*.java" | grep -c "readOnly = true" || echo "⚠️ Add readOnly = true to query methods"

# Verify transaction isolation levels where appropriate
grep -B 2 "@Transactional" backend/src/main/java --include="*.java" | grep "isolation = " || echo "✅ Using default isolation level"
```

### Pagination Test
```bash
# Test paginated endpoint
curl -X GET "http://localhost:8080/api/products?page=0&size=5&sort=price,desc" \
  -H "Authorization: Bearer $JWT_TOKEN"

# Should return only 5 records with pagination metadata
# Should include: totalPages, totalElements, hasNext, hasPrevious
```

### Validate Entity Constraints
```bash
# Test constraint violations
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"","sku":"","price":0}' \
  # Should return 400 Bad Request with validation errors

# Test duplicate SKU
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Product","sku":"SKU-001","price":99.99}' \
  # Should return 409 Conflict if SKU exists
```
