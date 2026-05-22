package com.nexoracommerce.order.controller;

import com.nexoracommerce.common.response.ApiResponse;
import com.nexoracommerce.order.dto.request.OrderRequest;
import com.nexoracommerce.order.dto.response.OrderPreviewResponse;
import com.nexoracommerce.order.dto.response.OrderResponse;
import com.nexoracommerce.order.service.IOrderService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwner(authentication, #request.userId())")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.createOrder(request, request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @PostMapping("/previews")
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwner(authentication, #request.userId())")
    public ResponseEntity<ApiResponse<OrderPreviewResponse>> previewOrder(@Valid @RequestBody OrderRequest request) {
        OrderPreviewResponse response = orderService.previewOrder(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<OrderResponse> orderPage = orderService.getAllOrdersPageable(pageable);
        return ResponseEntity.ok(
                ApiResponse.okWithPagination(
                        orderPage.getContent(),
                        ApiResponse.PaginationInfo.from(orderPage)
                )
        );
    }

    @GetMapping(params = "userId")
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwner(authentication, #userId)")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getUserOrders(
            @RequestParam String userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<OrderResponse> orderPage = orderService.getUserOrdersPageable(userId, pageable);
        return ResponseEntity.ok(
                ApiResponse.okWithPagination(
                        orderPage.getContent(),
                        ApiResponse.PaginationInfo.from(orderPage)
                )
        );
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwnerOfOrder(authentication, #orderId)")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable String orderId) {
        OrderResponse response = orderService.getOrderById(orderId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable String orderId,
            @RequestParam String status) {
        OrderResponse response = orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(ApiResponse.ok(response, "Order status updated successfully"));
    }

    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwnerOfOrder(authentication, #orderId)")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable String orderId) {
        OrderResponse response = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Order cancelled successfully"));
    }
}
