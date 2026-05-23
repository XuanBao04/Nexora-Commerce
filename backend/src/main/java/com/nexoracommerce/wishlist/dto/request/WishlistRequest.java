package com.nexoracommerce.wishlist.dto.request;

import jakarta.validation.constraints.NotBlank;

public record WishlistRequest(
    @NotBlank(message = "Product ID is required")
    String productId
) {}
