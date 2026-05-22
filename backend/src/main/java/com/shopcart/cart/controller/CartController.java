package com.shopcart.cart.controller;

import com.shopcart.cart.dto.request.CartItemRequest;
import com.shopcart.cart.dto.request.UpdateQuantityRequest;
import com.shopcart.cart.dto.response.CartResponse;
import com.shopcart.cart.service.ICartService;
import com.shopcart.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

/**
 * REST Controller for Cart operations
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final ICartService cartService;

    /**
     * Get cart for a user
     * @param userId the user ID
     * @return ApiResponse with CartResponse
     */
    @PreAuthorize("hasRole('ADMIN') or @cartSecurity.isOwner(authentication, #userId)")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@PathVariable String userId) {
        CartResponse response = cartService.getCart(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Add item to cart
     * @param userId the user ID
     * @param request CartItemRequest with product ID and quantity
     * @return ApiResponse with updated CartResponse
     */
    @PreAuthorize("hasRole('ADMIN') or @cartSecurity.isOwner(authentication, #userId)")
    @PostMapping("/{userId}/add")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @PathVariable String userId,
            @Valid @RequestBody CartItemRequest request) {
        CartResponse response = cartService.addToCart(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    /**
     * Remove item from cart
     * @param userId the user ID
     * @param productId the product ID to remove
     * @return ApiResponse with updated CartResponse
     */
    @PreAuthorize("hasRole('ADMIN') or @cartSecurity.isOwner(authentication, #userId)")
    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(
            @PathVariable String userId,
            @PathVariable String productId) {
        CartResponse response = cartService.removeFromCart(userId, productId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Update quantity of cart item
     * @param userId the user ID
     * @param productId the product ID to update
     * @param request UpdateQuantityRequest with new quantity
     * @return ApiResponse with updated CartResponse
     */
    @PreAuthorize("hasRole('ADMIN') or @cartSecurity.isOwner(authentication, #userId)")
    @PatchMapping("/{userId}/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateQuantity(
            @PathVariable String userId,
            @PathVariable String productId,
            @Valid @RequestBody UpdateQuantityRequest request) {
        CartResponse response = cartService.updateQuantity(userId, productId, request.getQuantity());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Clear entire cart
     * @param userId the user ID
     * @return ApiResponse with success message
     */
    @PreAuthorize("hasRole('ADMIN') or @cartSecurity.isOwner(authentication, #userId)")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> clearCart(@PathVariable String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
