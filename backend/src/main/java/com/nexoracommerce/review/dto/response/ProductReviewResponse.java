package com.nexoracommerce.review.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ProductReviewResponse(
    Long id,
    String userId,
    String username,
    String variantSku,
    String orderId,
    Integer rating,
    String comment,
    LocalDateTime createdAt,
    List<String> imageUrls,
    List<ProductReviewResponse> replies
) {}
