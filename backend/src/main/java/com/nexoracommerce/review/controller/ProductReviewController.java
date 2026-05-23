package com.nexoracommerce.review.controller;

import com.nexoracommerce.common.response.ApiResponse;
import com.nexoracommerce.review.dto.request.AdminReplyRequest;
import com.nexoracommerce.review.dto.request.ProductReviewRequest;
import com.nexoracommerce.review.dto.response.ProductReviewResponse;
import com.nexoracommerce.review.service.IProductReviewService;
import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductReviewController {

    private final IProductReviewService reviewService;

    @GetMapping("/variants/{variantSku}/reviews")
    public ResponseEntity<ApiResponse<List<ProductReviewResponse>>> getVariantReviews(
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
    public ResponseEntity<ApiResponse<ProductReviewResponse>> createReview(
            @Valid @RequestBody ProductReviewRequest request,
            @AuthenticationPrincipal String userId) {
        ProductReviewResponse response = reviewService.createReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @PostMapping("/admin/reviews/{reviewId}/replies")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductReviewResponse>> replyToReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody AdminReplyRequest request,
            @AuthenticationPrincipal String userId) {
        ProductReviewResponse response = reviewService.adminReply(userId, reviewId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @DeleteMapping("/reviews/{reviewId}")
    @PreAuthorize("hasRole('ADMIN') or @reviewSecurity.isReviewOwner(authentication, #reviewId)")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Review deleted successfully"));
    }
}
