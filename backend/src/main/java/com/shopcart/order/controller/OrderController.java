package com.shopcart.order.controller;

import com.shopcart.order.dto.request.OrderRequest;
import com.shopcart.order.dto.response.OrderPreviewResponse;
import com.shopcart.order.dto.response.OrderResponse;
import com.shopcart.order.service.IOrderService;
import com.shopcart.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

/**
 * REST Controller for Order operations
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;

    /**
     * Create a new order
     * @param request OrderRequest
     * @param userId the user ID
     * @return ApiResponse with created OrderResponse
     */
    @PostMapping("/{userId}")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request, 
            @PathVariable String userId) {
        OrderResponse response = orderService.createOrder(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    /**
     * Preview order before checkout
     * @param request OrderRequest
     * @return ApiResponse with OrderPreviewResponse
     */
    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<OrderPreviewResponse>> previewOrder(
            @Valid @RequestBody OrderRequest request) {
        OrderPreviewResponse response = orderService.previewOrder(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Get all orders (ADMIN only) without pagination
     * @return ApiResponse with list of OrderResponse
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders() {
        List<OrderResponse> responses = orderService.getAllOrders();
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    /**
     * Get all orders with pagination (ADMIN only)
     * @param page page number (0-indexed)
     * @param size page size
     * @param sort sorting field (default: createdAt)
     * @return ApiResponse with paginated OrderResponse
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrdersPaginated(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "createdAt") String sort) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).descending());
        Page<OrderResponse> orderPage = orderService.getAllOrdersPageable(pageable);
        
        return ResponseEntity.ok(
                ApiResponse.okWithPagination(orderPage.getContent(),
                    ApiResponse.PaginationInfo.from(orderPage))
        );
    }

    /**
     * Get a specific order by ID
     * @param orderId the order ID
     * @return ApiResponse with OrderResponse
     */
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwnerOfOrder(authentication, #orderId)")
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable String orderId) {
        OrderResponse response = orderService.getOrderById(orderId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Get orders for a specific user without pagination
     * @param userId the user ID
     * @return ApiResponse with list of OrderResponse
     */
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwner(authentication, #userId)")
    @GetMapping("/user/{userId}/all")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getUserOrders(@PathVariable String userId) {
        List<OrderResponse> responses = orderService.getUserOrders(userId);
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    /**
     * Get orders for a specific user with pagination
     * @param userId the user ID
     * @param page page number (0-indexed)
     * @param size page size
     * @param sort sorting field (default: createdAt)
     * @return ApiResponse with paginated OrderResponse
     */
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwner(authentication, #userId)")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getUserOrdersPaginated(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "createdAt") String sort) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).descending());
        Page<OrderResponse> orderPage = orderService.getUserOrdersPageable(userId, pageable);
        
        return ResponseEntity.ok(
                ApiResponse.okWithPagination(orderPage.getContent(),
                    ApiResponse.PaginationInfo.from(orderPage))
        );
    }

    /**
     * Cancel an order
     * @param orderId the order ID
     * @return ApiResponse with updated OrderResponse
     */
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwnerOfOrder(authentication, #orderId)")
    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable String orderId) {
        OrderResponse response = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Update order status (ADMIN only)
     * Valid status transitions:
     * PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
     * Any status → CANCELLED (except DELIVERED)
     * 
     * @param orderId the order ID
     * @param status the new OrderStatus (must be valid enum value)
     * @return ApiResponse with updated OrderResponse
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{orderId}/{status}")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable String orderId,
            @PathVariable String status) {
        OrderResponse response = orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}