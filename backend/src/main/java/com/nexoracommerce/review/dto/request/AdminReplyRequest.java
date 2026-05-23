package com.nexoracommerce.review.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdminReplyRequest(
    @NotBlank(message = "Reply comment is required")
    String comment
) {}
