package com.nexoracommerce.cart.controller;

import com.nexoracommerce.cart.dto.request.CartItemRequest;
import com.nexoracommerce.cart.dto.request.UpdateQuantityRequest;
import com.nexoracommerce.cart.dto.response.CartResponse;
import com.nexoracommerce.cart.service.ICartService;
import com.nexoracommerce.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
public class CartController {

    private final ICartService cartService;

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @cartSecurity.isOwner(authentication, #userId)")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@PathVariable String userId) {
        CartResponse response = cartService.getCart(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{userId}/items")
    @PreAuthorize("hasRole('ADMIN') or @cartSecurity.isOwner(authentication, #userId)")
    public ResponseEntity<ApiResponse<CartResponse>> addCartItem(
            @PathVariable String userId,
            @Valid @RequestBody CartItemRequest request) {
        CartResponse response = cartService.addToCart(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @PatchMapping("/{userId}/items/{productId}/quantity")
    @PreAuthorize("hasRole('ADMIN') or @cartSecurity.isOwner(authentication, #userId)")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItemQuantity(
            @PathVariable String userId,
            @PathVariable String productId,
            @Valid @RequestBody UpdateQuantityRequest request) {
        CartResponse response = cartService.updateQuantity(userId, productId, request.quantity());
        return ResponseEntity.ok(ApiResponse.ok(response, "Cart item quantity updated successfully"));
    }

    @DeleteMapping("/{userId}/items/{productId}")
    @PreAuthorize("hasRole('ADMIN') or @cartSecurity.isOwner(authentication, #userId)")
    public ResponseEntity<ApiResponse<CartResponse>> removeCartItem(
            @PathVariable String userId,
            @PathVariable String productId) {
        CartResponse response = cartService.removeFromCart(userId, productId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Cart item removed successfully"));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @cartSecurity.isOwner(authentication, #userId)")
    public ResponseEntity<ApiResponse<Void>> clearCart(@PathVariable String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Cart cleared successfully"));
    }
}
