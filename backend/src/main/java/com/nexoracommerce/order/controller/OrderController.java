package com.nexoracommerce.order.controller;

import com.nexoracommerce.common.response.ApiResponse;
import com.nexoracommerce.order.dto.request.OrderRequest;
import com.nexoracommerce.order.dto.request.OrderStatusChangeRequest;
import com.nexoracommerce.order.dto.response.OrderPreviewResponse;
import com.nexoracommerce.order.dto.response.OrderResponse;
import com.nexoracommerce.order.service.OrderService;
import com.nexoracommerce.common.enums.OrderStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Module", description = "Endpoints for order calculation previews, creation, user history listings, cancellations, and status management")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwner(authentication, #request.userId())")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Create a new checkout order",
        description = "Processes shopping cart details and creates a pending checkout transaction. Requires matching userId or ADMIN role."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order successfully created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation parameters failed or cart empty"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient role permissions")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.createOrder(request, request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @PostMapping("/previews")
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwner(authentication, #request.userId())")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Generate an order pricing calculation preview",
        description = "Computes subtotal, coupon discounts, shipping costs, and final totals before actual placement. Does not alter cart state."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pricing preview computed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Input validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient role permissions")
    })
    public ResponseEntity<ApiResponse<OrderPreviewResponse>> previewOrder(@Valid @RequestBody OrderRequest request) {
        OrderPreviewResponse response = orderService.previewOrder(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "List all orders in system (Admin)",
        description = "Requires ADMIN role. Fetches a paginated summary of all customer orders sorted by creation time descending by default."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders listings page retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions (must be ADMIN)")
    })
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<OrderResponse> orderPage = orderService.getOrdersWithFilters(null, null, null, null, null, null, null, pageable);
        return ResponseEntity.ok(
                ApiResponse.okWithPagination(
                        orderPage.getContent(),
                        ApiResponse.PaginationInfo.from(orderPage)
                )
        );
    }

    @GetMapping(params = "userId")
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwner(authentication, #userId)")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "List customer-specific order history",
        description = "Fetches a paginated history list of orders for a specific user ID. Requires matching userId or ADMIN role."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Customer orders page retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getUserOrders(
            @Parameter(description = "Customer ID associated with orders", example = "USR-001")
            @RequestParam String userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<OrderResponse> orderPage = orderService.getOrdersWithFilters(userId, null, null, null, null, null, null, pageable);
        return ResponseEntity.ok(
                ApiResponse.okWithPagination(
                        orderPage.getContent(),
                        ApiResponse.PaginationInfo.from(orderPage)
                )
        );
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwnerOfOrder(authentication, #orderId)")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Get single order details",
        description = "Fetches complete details for a single order by its ID. Requires matching owner user or ADMIN role."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order details successfully found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order ID not found")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @Parameter(description = "Order ID to look up", example = "ORD-20260523-999")
            @PathVariable String orderId,
            @Parameter(description = "User ID", example = "USR-001")
            @RequestParam String userId) {
        OrderResponse response = orderService.getOrderById(orderId, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Update order processing status (Admin)",
        description = "Requires ADMIN role. Transitions an order to status e.g., PENDING, PROCESSING, COMPLETED, CANCELLED."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order status updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status state transition"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions (must be ADMIN)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order ID not found")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @Parameter(description = "Order ID to update", example = "ORD-20260523-999")
            @PathVariable String orderId,
            @Parameter(description = "Target status transition state", example = "COMPLETED")
            @RequestParam String status) {
        OrderStatus orderStatus = OrderStatus.valueOf(status);
        OrderStatusChangeRequest request = new OrderStatusChangeRequest(orderId, orderStatus, null);
        OrderResponse response = orderService.updateOrderStatus(orderId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Order status updated successfully"));
    }

    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwnerOfOrder(authentication, #orderId)")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Cancel a customer order",
        description = "Cancels an order. Requires matching owner user or ADMIN role."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order successfully cancelled"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Order cannot be cancelled in its current state"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order ID not found")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @Parameter(description = "Order ID to cancel", example = "ORD-20260523-999")
            @PathVariable String orderId,
            @RequestParam String userId) {
        OrderResponse response = orderService.cancelOrder(orderId, userId, null);
        return ResponseEntity.ok(ApiResponse.ok(response, "Order cancelled successfully"));
    }
}
