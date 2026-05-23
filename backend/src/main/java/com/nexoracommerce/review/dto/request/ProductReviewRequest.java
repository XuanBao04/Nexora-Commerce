package com.nexoracommerce.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ProductReviewRequest(
    @NotBlank(message = "Variant SKU is required")
    String variantSku,

    String orderId,

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    Integer rating,

    @NotBlank(message = "Comment is required")
    String comment,

    List<String> imageUrls
) {}
