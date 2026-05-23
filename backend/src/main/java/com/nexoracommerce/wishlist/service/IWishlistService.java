package com.nexoracommerce.wishlist.service;

import com.nexoracommerce.wishlist.dto.request.WishlistRequest;
import com.nexoracommerce.wishlist.dto.response.WishlistResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IWishlistService {
    Page<WishlistResponse> getWishlistByUserId(String userId, Pageable pageable);
    WishlistResponse addToWishlist(String userId, WishlistRequest request);
    void removeFromWishlist(String userId, String productId);
}
