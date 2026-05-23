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

import java.util.List;

@RestController
@RequestMapping("/api/v1/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final IWishlistService wishlistService;

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #userId)")
    public ResponseEntity<ApiResponse<List<WishlistResponse>>> getWishlist(
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
    public ResponseEntity<ApiResponse<WishlistResponse>> addToWishlist(
            @PathVariable String userId,
            @Valid @RequestBody WishlistRequest request) {
        WishlistResponse response = wishlistService.addToWishlist(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @DeleteMapping("/{userId}/items/{productId}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #userId)")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
            @PathVariable String userId,
            @PathVariable String productId) {
        wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Product removed from wishlist successfully"));
    }
}
