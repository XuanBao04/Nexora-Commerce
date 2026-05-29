package com.nexoracommerce.wishlist.controller;

import com.nexoracommerce.common.response.ApiResponse;
import com.nexoracommerce.wishlist.dto.request.WishlistRequest;
import com.nexoracommerce.wishlist.dto.response.WishlistResponse;
import com.nexoracommerce.wishlist.service.IWishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

import java.util.List;

/**
 * REST Controller for user wishlist management
 * Handles product wishlist operations like adding, removing, and retrieving saved items
 */
@RestController
@RequestMapping("/api/v1/wishlists")
@RequiredArgsConstructor
@Tag(name = "Wishlist Module", description = "Endpoints for user wishlists, saved products, and favorites management")
public class WishlistController {

    private final IWishlistService wishlistService;

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #userId)")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Get user's wishlist items",
        description = "Retrieves a paginated list of products saved in a user's wishlist. Requires matching userId or ADMIN role."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Wishlist retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<ApiResponse<List<WishlistResponse>>> getWishlist(
            @Parameter(description = "Customer ID", example = "USR-001")
            @PathVariable String userId,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<WishlistResponse> wishlistPage = wishlistService.getWishlistByUserId(userId, pageable);
        return ResponseEntity.ok(
            ApiResponse.okWithPagination(
                wishlistPage.getContent(),
                ApiResponse.PaginationInfo.from(wishlistPage)
            )
        );
    }

    @PostMapping("/{userId}/items")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #userId)")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Add product to wishlist",
        description = "Adds a product to a user's wishlist. Requires matching userId or ADMIN role."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product added to wishlist successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or product already in wishlist"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User or product not found")
    })
    public ResponseEntity<ApiResponse<WishlistResponse>> addToWishlist(
            @Parameter(description = "Customer ID", example = "USR-001")
            @PathVariable String userId,
            @Valid @RequestBody WishlistRequest request) {
        WishlistResponse response = wishlistService.addToWishlist(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @DeleteMapping("/{userId}/items/{productId}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #userId)")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Remove product from wishlist",
        description = "Removes a product from a user's wishlist. Requires matching userId or ADMIN role."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product removed from wishlist successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Wishlist item not found")
    })
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
            @Parameter(description = "Customer ID", example = "USR-001")
            @PathVariable String userId,
            @Parameter(description = "The unique product code", example = "P001")
            @PathVariable String productId) {
        wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Product removed from wishlist successfully"));
    }
}
