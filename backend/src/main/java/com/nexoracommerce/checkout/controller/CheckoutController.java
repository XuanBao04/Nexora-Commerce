package com.nexoracommerce.checkout.controller;

import com.nexoracommerce.checkout.dto.PaymentRequest;
import com.nexoracommerce.checkout.dto.PaymentResponse;
import com.nexoracommerce.checkout.service.ICheckoutService;
import com.nexoracommerce.common.response.ApiResponse;
import com.nexoracommerce.order.dto.request.OrderRequest;
import com.nexoracommerce.order.dto.response.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller for checkout and payment processing operations
 * Handles order creation with concurrency protection and payment transaction processing
 */
@RestController
@RequestMapping("/api/v1/checkouts")
@RequiredArgsConstructor
@Tag(name = "Checkout Module", description = "Endpoints for order checkout process, Redis-protected inventory reservation, and payment processing")
public class CheckoutController {

    private final ICheckoutService checkoutService;

    /**
     * POST /api/v1/checkouts - Initiate checkout with Redis-based inventory protection
     * Processes shopping cart items, validates inventory, creates pending order, and reserves stock atomically.
     * Returns orderId and optionally a payment URL if payment method is VNPAY.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwner(authentication, #request.userId())")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Initiate checkout with order creation",
        description = "Processes the checkout request with Redis-based inventory protection to prevent overselling. " +
                "Creates a pending order, reserves stock atomically, and prepares payment. " +
                "Supports both COD (Cash on Delivery) and VNPAY payment methods."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order created successfully, payment URL provided if applicable"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or inventory insufficient"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Checkout conflict - inventory already reserved or order exists")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @Valid @RequestBody OrderRequest request) {
        OrderResponse order = checkoutService.checkoutWithRedisProtection(request, request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(order));
    }

    /**
     * POST /api/v1/checkouts/orders/{orderId}/payments - Process payment confirmation
     * Updates payment status and order status based on payment success/failure.
     * Called after external payment gateway (e.g., VNPAY) completes transaction.
     */
    @PostMapping("/orders/{orderId}/payments")
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwnerOfOrder(authentication, #orderId)")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Process and confirm payment for an order",
        description = "Confirms payment success or failure for a pending order. " +
                "Updates order status from PENDING to CONFIRMED or CANCELLED based on payment result. " +
                "Triggers inventory commit or rollback accordingly."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment processed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid payment status or order not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Parameter(description = "The order ID to process payment for", example = "ORD-001")
            @PathVariable String orderId,
            @Valid @RequestBody PaymentRequest paymentRequest) {
        PaymentResponse response = checkoutService.processPayment(orderId, paymentRequest.successful());
        return ResponseEntity.ok(ApiResponse.ok(response, response.message()));
    }
}
