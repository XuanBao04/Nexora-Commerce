package com.nexoracommerce.checkout.controller;

import com.nexoracommerce.checkout.dto.PaymentRequest;
import com.nexoracommerce.checkout.service.CheckoutService;
import com.nexoracommerce.order.dto.request.OrderRequest;
import com.nexoracommerce.order.dto.response.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Checkout controller with Redis stock protection
 */
@Slf4j
@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;
    
    /**
     * Create order with Redis stock protection
     * POST /api/checkout
     * 
     * This endpoint:
     * 1. Checks Redis stock
     * 2. Decrements Redis stock atomically
     * 3. Creates order in DB
     * 4. Returns order ready for payment
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrderResponse> checkout(
            @RequestBody OrderRequest request,
            @RequestHeader("Authorization") String token) {
        
        try {
            // Extract userId from token or security context
            // For now, using a placeholder - integrate with actual user extraction
            String userId = extractUserIdFromToken(token);
            
            OrderResponse order = checkoutService.checkoutWithRedisProtection(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
            
        } catch (RuntimeException e) {
            log.warn("Checkout failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(null);
        } catch (Exception e) {
            log.error("Checkout error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
    
    /**
     * Process payment for a pending order
     * POST /api/checkout/payment/{orderId}
     * 
     * Request body:
     * {
     *   "paymentMethod": "CREDIT_CARD|BANK_TRANSFER|E_WALLET",
     *   "successful": true|false
     * }
     * 
     * If payment is successful:
     * - Order transitions to CONFIRMED
     * 
     * If payment fails:
     * - Stock is rolled back in both Redis and DB
     * - Order is marked CANCELLED
     */
    @PostMapping("/payment/{orderId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> processPayment(
            @PathVariable String orderId,
            @RequestBody PaymentRequest paymentRequest) {
        
        try {
            checkoutService.processPayment(orderId, paymentRequest.isSuccessful());
            
            if (paymentRequest.isSuccessful()) {
                return ResponseEntity.ok("Payment processed successfully. Order confirmed.");
            } else {
                return ResponseEntity.ok("Payment failed. Order cancelled and stock restored.");
            }
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Order not found: " + e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid order state: " + e.getMessage());
        } catch (Exception e) {
            log.error("Payment processing error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Payment processing error: " + e.getMessage());
        }
    }
    
    /**
     * TODO: Implement actual JWT token parsing
     * For now, returns a placeholder userId
     */
    private String extractUserIdFromToken(String token) {
        // This should be implemented with actual JWT parsing
        // For testing, use a default value
        return "user-" + System.nanoTime();
    }
}
