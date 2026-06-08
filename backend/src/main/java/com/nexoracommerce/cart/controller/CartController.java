package com.nexoracommerce.cart.controller;

import com.nexoracommerce.cart.dto.request.CartItemRequest;
import com.nexoracommerce.cart.dto.request.UpdateQuantityRequest;
import com.nexoracommerce.cart.dto.response.CartResponse;
import com.nexoracommerce.cart.service.CartService;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
@Tag(name = "Cart Module", description = "Endpoints for active user shopping carts, adding/removing items, and clearing carts")
public class CartController {

    private final CartService cartService;

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @cartSecurity.isOwner(authentication, #userId)")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Retrieve user shopping cart",
        description = "Requires standard authenticated user matching the userId or ADMIN role. Fetches active cart and nested line items."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shopping cart retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions to access this cart")
    })
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @Parameter(description = "Customer ID associated with the cart", example = "USR-001")
            @PathVariable String userId) {
        CartResponse response = cartService.getCart(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{userId}/items")
    @PreAuthorize("hasRole('ADMIN') or @cartSecurity.isOwner(authentication, #userId)")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Add an item to the shopping cart",
        description = "Requires standard authenticated user matching the userId or ADMIN role. Adds product variant item to cart."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Item successfully added to cart"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation parameters failed or product not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<ApiResponse<CartResponse>> addCartItem(
            @Parameter(description = "Customer ID associated with the cart", example = "USR-001")
            @PathVariable String userId,
            @Valid @RequestBody CartItemRequest request) {
        CartResponse response = cartService.addToCart(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @PatchMapping("/{userId}/items/{productId}/quantity")
    @PreAuthorize("hasRole('ADMIN') or @cartSecurity.isOwner(authentication, #userId)")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Update cart item quantity",
        description = "Requires standard authenticated user matching the userId or ADMIN role. Updates stock count for a specific product inside the cart."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart item quantity successfully updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid quantity or out of stock"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItemQuantity(
            @Parameter(description = "Customer ID associated with the cart", example = "USR-001")
            @PathVariable String userId,
            @Parameter(description = "The unique product code to update", example = "P001")
            @PathVariable String productId,
            @Valid @RequestBody UpdateQuantityRequest request) {
        CartResponse response = cartService.updateQuantity(userId, productId, request.quantity());
        return ResponseEntity.ok(ApiResponse.ok(response, "Cart item quantity updated successfully"));
    }

    @DeleteMapping("/{userId}/items/{productId}")
    @PreAuthorize("hasRole('ADMIN') or @cartSecurity.isOwner(authentication, #userId)")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Remove an item from the shopping cart",
        description = "Requires standard authenticated user matching the userId or ADMIN role. Removes product reference from cart."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Item removed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<ApiResponse<CartResponse>> removeCartItem(
            @Parameter(description = "Customer ID associated with the cart", example = "USR-001")
            @PathVariable String userId,
            @Parameter(description = "The unique product code to remove", example = "P001")
            @PathVariable String productId) {
        CartResponse response = cartService.removeFromCart(userId, productId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Cart item removed successfully"));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @cartSecurity.isOwner(authentication, #userId)")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Clear the entire shopping cart",
        description = "Requires standard authenticated user matching the userId or ADMIN role. Removes all items from active cart."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart cleared successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @Parameter(description = "Customer ID associated with the cart", example = "USR-001")
            @PathVariable String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Cart cleared successfully"));
    }
}
