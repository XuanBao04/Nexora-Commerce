package com.nexoracommerce.review.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexoracommerce.common.response.ApiResponse;
import com.nexoracommerce.review.dto.request.AdminReplyRequest;
import com.nexoracommerce.review.dto.request.ProductReviewRequest;
import com.nexoracommerce.review.dto.response.ProductReviewResponse;
import com.nexoracommerce.review.service.IProductReviewService;
import jakarta.validation.Valid;

import java.util.List;

/**
 * REST Controller for product reviews and ratings
 * Handles review creation, display, admin replies, and review management
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Review Module", description = "Endpoints for product reviews, customer ratings, and admin review moderation")
public class ProductReviewController {

    private final IProductReviewService reviewService;

    @GetMapping("/variants/{variantSku}/reviews")
    @Operation(
        summary = "Get all reviews for a product variant",
        description = "Retrieves a paginated list of customer reviews and ratings for a specific product variant."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reviews retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Variant not found")
    })
    public ResponseEntity<ApiResponse<List<ProductReviewResponse>>> getVariantReviews(
            @Parameter(description = "Product variant SKU", example = "SKU-001")
            @PathVariable String variantSku,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<ProductReviewResponse> reviewPage = reviewService.getReviewsByVariantSku(variantSku, pageable);
        return ResponseEntity.ok(
            ApiResponse.okWithPagination(
                reviewPage.getContent(),
                ApiResponse.PaginationInfo.from(reviewPage)
            )
        );
    }

    @PostMapping("/reviews")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Create a new product review",
        description = "Allows authenticated users to create a review and rating for a product variant they have purchased."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Review created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or duplicate review"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product variant not found")
    })
    public ResponseEntity<ApiResponse<ProductReviewResponse>> createReview(
            @Valid @RequestBody ProductReviewRequest request,
            @AuthenticationPrincipal String userId) {
        ProductReviewResponse response = reviewService.createReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @PostMapping("/admin/reviews/{reviewId}/replies")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Add admin reply to a review (Admin only)",
        description = "Requires ADMIN role. Allows admins to respond to customer reviews."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Reply added successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions (must be ADMIN)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Review not found")
    })
    public ResponseEntity<ApiResponse<ProductReviewResponse>> replyToReview(
            @Parameter(description = "Review database ID", example = "1")
            @PathVariable Long reviewId,
            @Valid @RequestBody AdminReplyRequest request,
            @AuthenticationPrincipal String userId) {
        ProductReviewResponse response = reviewService.adminReply(userId, reviewId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @DeleteMapping("/reviews/{reviewId}")
    @PreAuthorize("hasRole('ADMIN') or @reviewSecurity.isReviewOwner(authentication, #reviewId)")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Delete a review",
        description = "Allows review owner or ADMIN to delete a review. Removes review and all associated replies."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Review deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Review not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @Parameter(description = "Review database ID", example = "1")
            @PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Review deleted successfully"));
    }
}
