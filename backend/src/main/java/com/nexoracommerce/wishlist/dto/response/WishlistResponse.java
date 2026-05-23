package com.nexoracommerce.wishlist.dto.response;

import java.time.LocalDateTime;

public record WishlistResponse(
    Long id,
    String userId,
    String productId,
    String productName,
    Long productPrice,
    LocalDateTime createdAt
) {}
