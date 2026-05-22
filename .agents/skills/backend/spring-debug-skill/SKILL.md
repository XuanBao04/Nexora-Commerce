---
name: spring-debug-skill
description: Enforce production-grade testing, structured logging, debugging practices, and performance monitoring for Spring Boot applications.
---

## Scope & Activation Rules

Activate when:
- Writing unit tests, integration tests, or end-to-end tests
- Configuring logging (levels, appenders, output format)
- Adding performance metrics, health checks, or actuator endpoints
- Debugging runtime issues, memory leaks, or slow queries
- Monitoring application behavior in production
- Analyzing error traces and stack overflow scenarios

## System Directives

### DO
- **Use Structured Logging**: Always log with context (userId, orderId, requestId) in JSON format for centralized log aggregation.
- **Test at Multiple Levels**: Unit tests → Integration tests → Slice tests. Use `@DataJpaTest`, `@WebMvcTest`, `@SpringBootTest` appropriately.
- **Mock External Dependencies**: Use `@MockBean` for remote services (payment gateway, email, etc.) in tests.
- **Verify Log Output**: Assert that critical operations produce expected log statements in tests.
- **Enable Actuator Endpoints**: Expose `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus` in non-prod.
- **Implement Correlation IDs**: Pass `X-Request-ID` or similar headers through entire request lifecycle for tracing.
- **Profile Performance Bottlenecks**: Use `StopWatch`, `@Timed` from Micrometer, or Spring Data audit logs.
- **Log at Entry/Exit of Service Methods**: Especially for transactional operations and external calls.
- **Use SLF4J with Logback**: Never use System.out.println or log4j directly.
- **Set Appropriate Log Levels**: DEBUG in dev, INFO in staging, WARN/ERROR in production.
- **Implement Custom Health Checks**: Use `@Component implements HealthIndicator` for business-critical dependencies.
- **Test Exception Handling**: Verify that expected exceptions are thrown and logged appropriately.
- **Use Test Containers for Integration Tests**: Use Testcontainers for MySQL, Redis to avoid mocking databases.
- **Profile Database Queries**: Enable Hibernate statistics and slow query logging in development.

### DO NOT
- Log sensitive data (passwords, tokens, PII, credit card numbers). Sanitize logs before production.
- Use generic `Exception` in catch blocks without logging specifics.
- Leave `System.out.println()` or printStackTrace() in code. Always use SLF4J.
- Test only the happy path. Write tests for error scenarios and edge cases.
- Skip integration tests. Unit tests alone don't catch integration issues.
- Mock everything in integration tests. Test real database interactions with Testcontainers.
- Leave debugging code in production branches (`e.printStackTrace()`, `System.exit()`, verbose logs).
- Ignore performance warnings from Spring Boot startup logs.
- Test without isolation. Ensure tests run independently; clear database state between tests.
- Use blocking operations in tests without timeouts. Always set `@Timeout` or equivalent.
- Skip checking for memory leaks in long-running tests.
- Overlook slow query logs. Investigate and optimize queries flagged in logs.

## Production Reference Implementation

### SLF4J Logging Configuration (logback-spring.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <property name="LOG_FILE" value="${LOG_FILE:-logs/application.log}"/>
    <property name="LOG_PATTERN" value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"/>
    
    <!-- Console Appender for development -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>
    
    <!-- File Appender with rolling policy -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_FILE}</file>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_FILE}.%d{yyyy-MM-dd}.%i.zip</fileNamePattern>
            <maxFileSize>10MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>1GB</totalSizeCap>
        </rollingPolicy>
    </appender>
    
    <!-- JSON Appender for structured logging (using Logstash) -->
    <appender name="JSON" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_FILE}.json</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_FILE}.json.%d{yyyy-MM-dd}.%i.zip</fileNamePattern>
            <maxFileSize>10MB</maxFileSize>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service":"nexora-commerce-backend"}</customFields>
        </encoder>
    </appender>
    
    <!-- Spring profiles -->
    <springProfile name="dev">
        <root level="DEBUG">
            <appender-ref ref="CONSOLE"/>
            <appender-ref ref="FILE"/>
        </root>
        <logger name="org.springframework" level="DEBUG"/>
        <logger name="org.hibernate.SQL" level="DEBUG"/>
        <logger name="org.hibernate.type.descriptor.sql.BasicBinder" level="TRACE"/>
    </springProfile>
    
    <springProfile name="prod">
        <root level="INFO">
            <appender-ref ref="FILE"/>
            <appender-ref ref="JSON"/>
        </root>
        <logger name="org.springframework" level="WARN"/>
        <logger name="org.hibernate" level="WARN"/>
    </springProfile>
</configuration>
```

### Structured Logging Service with Correlation ID

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceWithLogging {
    
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final MeterRegistry meterRegistry;
    
    @Transactional
    public CreateOrderResponse createOrder(String cartId, CreateOrderRequest request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("userId", request.userId());
        MDC.put("cartId", cartId);
        
        try {
            log.info("Order creation started", 
                Map.of("action", "ORDER_CREATE_START", "cartId", cartId));
            
            Timer.Sample sample = Timer.start(meterRegistry);
            
            Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> {
                    log.warn("Cart not found", Map.of("error", "CART_NOT_FOUND"));
                    return new CartNotFoundException("Cart not found: " + cartId);
                });
            
            Order order = Order.builder()
                .userId(request.userId())
                .cartId(cartId)
                .status(OrderStatus.PENDING)
                .totalAmount(cart.getTotalPrice())
                .createdAt(LocalDateTime.now())
                .build();
            
            Order savedOrder = orderRepository.save(order);
            
            sample.stop(Timer.builder("order.creation.duration")
                .tag("status", "success")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry));
            
            log.info("Order created successfully",
                Map.of("orderId", savedOrder.getId(), "amount", savedOrder.getTotalAmount()));
            
            return new CreateOrderResponse(savedOrder.getId(), savedOrder.getStatus());
        } catch (Exception e) {
            log.error("Order creation failed", 
                Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()), e);
            
            meterRegistry.counter("order.creation.failures", 
                "error", e.getClass().getSimpleName()).increment();
            
            throw new OrderCreationException("Failed to create order", e);
        } finally {
            MDC.clear();
        }
    }
}
```

### Request Correlation ID Interceptor

```java
@Component
@RequiredArgsConstructor
public class CorrelationIdFilter implements Filter {
    
    private static final String CORRELATION_ID_HEADER = "X-Request-ID";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
        
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        
        MDC.put("correlationId", correlationId);
        MDC.put("requestPath", httpRequest.getRequestURI());
        MDC.put("method", httpRequest.getMethod());
        
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
```

### Integration Testing Example

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Slf4j
class OrderIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private CartRepository cartRepository;
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("nexora_test")
        .withUsername("test")
        .withPassword("test");
    
    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        cartRepository.deleteAll();
        log.info("Test setup completed");
    }
    
    @Test
    @DisplayName("Should create order with valid cart and items")
    void testCreateOrderSuccess() {
        // Arrange
        String cartId = "cart-123";
        Cart cart = Cart.builder()
            .id(cartId)
            .userId("user-456")
            .status(CartStatus.ACTIVE)
            .totalPrice(BigDecimal.valueOf(99.99))
            .itemCount(2)
            .build();
        
        cartRepository.save(cart);
        
        CreateOrderRequest request = new CreateOrderRequest(
            cartId,
            new AddressDto("123 Main St", "New York", "10001", "US"),
            "CREDIT_CARD"
        );
        
        // Act
        ResponseEntity<CreateOrderResponse> response = restTemplate.postForEntity(
            "/api/orders",
            request,
            CreateOrderResponse.class
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().orderId()).isNotNull();
        
        Order savedOrder = orderRepository.findById(response.getBody().orderId()).orElse(null);
        assertThat(savedOrder).isNotNull();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
        
        log.info("Test passed: Order created successfully");
    }
    
    @Test
    @DisplayName("Should return 404 when cart not found")
    void testCreateOrderCartNotFound() {
        CreateOrderRequest request = new CreateOrderRequest(
            "invalid-cart",
            new AddressDto("123 Main St", "New York", "10001", "US"),
            "CREDIT_CARD"
        );
        
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "/api/orders",
            request,
            ErrorResponse.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().error()).isEqualTo("CART_NOT_FOUND");
    }
}
```

### Unit Testing with Mocking

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private CartRepository cartRepository;
    
    @Mock
    private PaymentClient paymentClient;
    
    @InjectMocks
    private OrderService orderService;
    
    @Test
    @DisplayName("Should create order successfully")
    void shouldCreateOrderSuccessfully() {
        // Arrange
        String cartId = "cart-123";
        String userId = "user-456";
        Cart cart = Cart.builder()
            .id(cartId)
            .userId(userId)
            .totalPrice(BigDecimal.valueOf(99.99))
            .build();
        
        Order savedOrder = Order.builder()
            .id("order-789")
            .userId(userId)
            .status(OrderStatus.PENDING)
            .build();
        
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        
        CreateOrderRequest request = new CreateOrderRequest(
            cartId,
            new AddressDto("123 Main St", "New York", "10001", "US"),
            "CREDIT_CARD"
        );
        
        // Act
        CreateOrderResponse response = orderService.createOrder(cartId, request.withUserId(userId));
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("order-789");
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(paymentClient, times(1)).initiatePayment(anyString(), any(BigDecimal.class));
    }
    
    @Test
    @DisplayName("Should throw CartNotFoundException when cart not found")
    void shouldThrowCartNotFound() {
        when(cartRepository.findById("invalid")).thenReturn(Optional.empty());
        
        CreateOrderRequest request = new CreateOrderRequest(
            "invalid",
            new AddressDto("123 Main St", "New York", "10001", "US"),
            "CREDIT_CARD"
        );
        
        assertThrows(CartNotFoundException.class, 
            () -> orderService.createOrder("invalid", request.withUserId("user-456")));
    }
}
```

### Actuator Configuration (application.yml)

```yaml
spring:
  application:
    name: nexora-commerce-backend

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,info,env
      base-path: /actuator
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
  metrics:
    enable:
      jvm: true
      process: true
      logback: true
      http: true
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
      slo:
        http.server.requests: 50ms,100ms,200ms,500ms,1s

logging:
  level:
    root: INFO
    com.nexora: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/application.log
```

### Custom Health Indicator

```java
@Component
public class OrderServiceHealthIndicator implements HealthIndicator {
    
    private final OrderRepository orderRepository;
    
    @Autowired
    public OrderServiceHealthIndicator(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
    
    @Override
    public Health health() {
        try {
            long recentOrders = orderRepository.countRecentOrders(LocalDateTime.now().minusHours(1));
            
            if (recentOrders > 1000) {
                return Health.up()
                    .withDetail("status", "Order service healthy")
                    .withDetail("recentOrders", recentOrders)
                    .build();
            } else {
                return Health.outOfService()
                    .withDetail("status", "No recent orders - service may be down")
                    .withDetail("recentOrders", recentOrders)
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

## Anti-Patterns & Automated Fixes

### Anti-Pattern 1: Logging Sensitive Data
**Problem**: Logging passwords, tokens, or PII exposes security risks.
```java
// ❌ WRONG
log.info("User login with credentials: email={}, password={}", email, password);
log.debug("JWT token: {}", token);
```
**Fix**: Sanitize sensitive data or use masking.
```java
// ✅ CORRECT
log.info("User login: email={}", maskEmail(email));
log.debug("JWT token generated for user: {}", userId);

private String maskEmail(String email) {
    return email.replaceAll("(?<=.{2}).(?=.*@)", "*");
}
```

### Anti-Pattern 2: Catching Generic Exception
**Problem**: Generic catch blocks hide specific errors.
```java
// ❌ WRONG
try {
    // code
} catch (Exception e) {
    log.error("Error: " + e.getMessage());
}
```
**Fix**: Catch specific exceptions with detailed logging.
```java
// ✅ CORRECT
try {
    // code
} catch (CartNotFoundException e) {
    log.warn("Cart not found during order creation", e);
    throw new OrderCreationException("Cart not found", e);
} catch (PaymentException e) {
    log.error("Payment processing failed", e);
    throw new OrderCreationException("Payment failed", e);
}
```

### Anti-Pattern 3: Skipping Integration Tests
**Problem**: Only unit testing misses integration issues.
```java
// ❌ WRONG
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock OrderRepository repo;
    // All mocked, no real database testing
}
```
**Fix**: Include integration tests with Testcontainers.
```java
// ✅ CORRECT
@SpringBootTest
class OrderIntegrationTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
    
    @Autowired OrderRepository repo;
    // Tests real database interactions
}
```

### Anti-Pattern 4: Using System.out.println in Production
**Problem**: System.out doesn't route through centralized logging.
```java
// ❌ WRONG
System.out.println("Order created: " + order.getId());
e.printStackTrace();
```
**Fix**: Use SLF4J exclusively.
```java
// ✅ CORRECT
log.info("Order created: {}", order.getId());
log.error("Error occurred", e);
```

## Verification Commands

### Verify Logging Configuration
```bash
# Check for System.out.println (should be empty)
grep -r "System\.out\.println\|System\.err\.println" backend/src --include="*.java" && echo "❌ Found System.out calls" || echo "✅ No System.out calls"

# Check for e.printStackTrace() (should be empty)
grep -r "printStackTrace()" backend/src --include="*.java" && echo "❌ Found printStackTrace calls" || echo "✅ No printStackTrace calls"

# Check SLF4J is used
grep -r "@Slf4j\|LoggerFactory\|getLogger" backend/src/main/java --include="*.java" | wc -l
```

### Run Tests with Coverage
```bash
# Run all tests with code coverage
cd backend && mvn clean test jacoco:report

# Check coverage report
open target/site/jacoco/index.html

# Run integration tests specifically
mvn test -Dgroups=integration
```

### Verify Logging Isolation in Tests
```bash
# Ensure test logs don't leak to console (use logback-test.xml)
cd backend && mvn test -Dorg.slf4j.simpleLogger.defaultLogLevel=warn

# Check for test leaks
grep -r "System.out\|System.err" backend/src/test --include="*.java" || echo "✅ Test files clean"
```

### Health Check & Metrics
```bash
# Start application
cd backend && java -jar target/nexora-commerce-backend-1.0.0.jar &

# Check health
curl -X GET http://localhost:8080/actuator/health | jq

# Check metrics
curl -X GET http://localhost:8080/actuator/metrics | jq

# Check Prometheus metrics
curl -X GET http://localhost:8080/actuator/prometheus | head -20
```

### Performance Profiling
```bash
# Check slow query logs
grep "took" logs/application.log | grep -v "0ms\|[1-5]ms" | head -10

# Check service method timings
grep "Order.*duration\|duration.*Order" logs/application.log | jq '.duration_ms'

# Memory usage over time
ps aux | grep "java.*nexora" | grep -oP "rss=\K\d+"
```
