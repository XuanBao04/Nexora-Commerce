---
name: spring-controller-skill
description: Strict guidelines and standards for designing RESTful Spring Boot Controllers (Web Layer) following Clean Architecture.
---

## Scope & Activation Rules
This skill is activated automatically when creating, refactoring, or reviewing any **Spring Boot Controller class** (annotated with `@RestController`) or web layer handlers in the codebase. Every REST controller must strictly adhere to these rules to maintain a decoupled, robust, and industry-standard web layer.

---

## System Directives

### DO
1. **Keep Controllers Thin (Thin Controller Principle):** Limit controller methods strictly to HTTP-specific concerns:
   - Accept input payload and validate it first using `@Valid`.
   - Delegate the actual business and data operations to the Service layer (via interfaces).
   - Wrap the service response inside a standardized `ApiResponse<T>` envelope and return it with the correct HTTP status code.
2. **Adhere to Strict RESTful Naming Conventions:**
   - URLs must exclusively use lowercase letters, hyphens (`-`) for word separation, and plural nouns representing resources (e.g., `/api/v1/orders`, `/api/v1/products`).
   - Use path variables for resource identifiers (e.g., `/api/v1/orders/{orderId}`).
   - Cập nhật và truy vấn trạng thái phụ thông qua sub-resources thay vì động từ (Ví dụ: Dùng `PATCH /api/v1/orders/{id}/status` thay vì `POST /api/v1/orders/{id}/update-status`).
3. **Use Explicit HTTP Methods & Status Codes:**
   - **GET**: Retrieving resources. Returns `200 OK`.
   - **POST**: Creating new resources. Returns `201 Created`.
   - **PUT**: Complete updates of resources. Returns `200 OK`.
   - **PATCH**: Partial updates of resources. Returns `200 OK`.
   - **DELETE**: Removing resources. Returns `204 No Content` (if returning nothing) or `200 OK` (if returning a confirmation).
4. **Enforce Input Validation (Validation First):**
   - Place `@Valid` (from `jakarta.validation`) on request payload parameters in the controller signatures.
   - Annotate request DTOs (Java 21 Records) with constraints like `@NotBlank`, `@NotNull`, `@Min`, `@Max`, `@Size`, `@Email` to catch invalid data at the threshold of the system.
5. **Standardize Data Encapsulation:**
   - Always return a unified generic response structure, typically `ResponseEntity<ApiResponse<T>>`, where `T` is a DTO (Java 21 Record).
   - This ensures all client-facing APIs possess a predictable envelope containing `success`, `message`, `data`, `status`, and metadata.
6. **Apply Constructor Dependency Injection:**
   - Rely solely on Lombok's `@RequiredArgsConstructor` combined with `private final` service interface declarations. Never use field injection via `@Autowired`.

### DO NOT
1. **Never Leak Database Entities:** Do not accept or return database entities (JPA `@Entity` objects like `Order`, `Product`, `User`) in the Controller methods. Doing so couples your database schema to the public API and poses mass-assignment security vulnerabilities.
2. **No Business Logic in Controller:** Never perform database access (calling repositories), calculations, status transition checks, collections transformations, or caching operations directly inside a controller method.
3. **No Verbs in Resource Paths:** Never use verbs in path segments (e.g., avoid `/api/v1/create-order`, `/api/v1/getProducts`, `/api/v1/deleteUser/{id}`). Use the standard HTTP verbs (`POST`, `GET`, `DELETE`) to express the action instead.
4. **Never Return Raw DTOs directly:** Do not return DTOs or collections without being wrapped in the `ApiResponse<T>` envelope. It prevents you from returning unified status codes, localized messages, or pagination details cleanly.
5. **Do Not Catch Exceptions in Controllers:** Avoid `try-catch` blocks inside controller methods. Allow exceptions to propagate upward naturally, where they will be caught and transformed into standard JSON error responses by a global `@RestControllerAdvice`.

---

## Standard Reference Pattern

Below is the standard architectural blueprint illustrating a perfectly decoupled `OrderController` alongside the unified `ApiResponse` wrapper.

### 1. Standard DTO Records (`OrderRequest.java` and `OrderResponse.java`)

```java
package com.nexoracommerce.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderRequest(
    @NotBlank(message = "Shipping address is required")
    String shippingAddress,

    @NotBlank(message = "Phone number is required")
    String phoneNumber,

    @NotEmpty(message = "Order must contain at least one item")
    List<OrderItemRequest> orderItems,

    String couponCode
) {}
```

```java
package com.nexoracommerce.order.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
    String id,
    String userId,
    long totalPrice,
    long shippingFee,
    long discountAmount,
    String status,
    String shippingAddress,
    String phoneNumber,
    LocalDateTime createdAt,
    List<OrderItemResponse> orderItems
) {}
```

### 2. Standard Generic Envelope (`ApiResponse.java`)

```java
package com.nexoracommerce.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    int status,
    PaginationInfo pagination
) {
    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Request processed successfully")
                .data(data)
                .status(200)
                .build();
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .status(200)
                .build();
    }

    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Resource created successfully")
                .data(data)
                .status(201)
                .build();
    }

    public static <T> ApiResponse<T> okWithPagination(T data, PaginationInfo pagination) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Request processed successfully")
                .data(data)
                .status(200)
                .pagination(pagination)
                .build();
    }

    public record PaginationInfo(
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean last
    ) {
        public static PaginationInfo from(Page<?> page) {
            return new PaginationInfo(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
            );
        }
    }
}
```

### 3. Thin Controller Blueprint (`OrderController.java`)

```java
package com.nexoracommerce.order.controller;

import com.nexoracommerce.common.response.ApiResponse;
import com.nexoracommerce.order.dto.request.OrderRequest;
import com.nexoracommerce.order.dto.response.OrderResponse;
import com.nexoracommerce.order.service.IOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;

    /**
     * Create a new order
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal String userId) {
        
        OrderResponse response = orderService.createOrder(request, userId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    /**
     * Get a paginated list of all orders (ADMIN only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders(Pageable pageable) {
        Page<OrderResponse> orderPage = orderService.getAllOrdersPageable(pageable);
        
        return ResponseEntity.ok(
            ApiResponse.okWithPagination(
                orderPage.getContent(),
                ApiResponse.PaginationInfo.from(orderPage)
            )
        );
    }

    /**
     * Get order details by ID
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable String orderId) {
        OrderResponse response = orderService.getOrderById(orderId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Update order status (ADMIN only)
     */
    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable String orderId,
            @RequestParam String status) {
        
        OrderResponse response = orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(ApiResponse.ok(response, "Order status updated successfully"));
    }

    /**
     * Delete/Cancel an order (Resource deletion standard)
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable String orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Order cancelled successfully"));
    }
}
```
