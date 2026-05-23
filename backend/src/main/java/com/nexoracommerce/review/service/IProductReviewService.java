package com.nexoracommerce.review.service;

import com.nexoracommerce.review.dto.request.AdminReplyRequest;
import com.nexoracommerce.review.dto.request.ProductReviewRequest;
import com.nexoracommerce.review.dto.response.ProductReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IProductReviewService {
    Page<ProductReviewResponse> getReviewsByVariantSku(String variantSku, Pageable pageable);
    ProductReviewResponse createReview(String userId, ProductReviewRequest request);
    ProductReviewResponse adminReply(String adminUserId, Long reviewId, AdminReplyRequest request);
    void deleteReview(Long reviewId);
}
