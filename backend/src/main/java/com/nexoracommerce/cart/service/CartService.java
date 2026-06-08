package com.nexoracommerce.cart.service;

import com.nexoracommerce.cart.dto.response.CartResponse;
import com.nexoracommerce.cart.dto.request.CartItemRequest;

/**
 * Service interface for Cart operations
 */
public interface ICartService {

    /**
     * Get cart for a specific user
     * @param userId the user ID
     * @return CartResponse containing cart items
     */
    CartResponse getCart(String userId);

    /**
     * Add item to cart
     * @param userId the user ID
     * @param request CartItemRequest with product details
     * @return updated CartResponse
     */
    CartResponse addToCart(String userId, CartItemRequest request);

    /**
     * Remove item from cart
     * @param userId the user ID
     * @param productId the product ID to remove
     * @return updated CartResponse
     */
    CartResponse removeFromCart(String userId, String productId);

    /**
     * Update cart item quantity
     * @param userId the user ID
     * @param productId the product ID
     * @param quantity new quantity
     * @return updated CartResponse
     */
    CartResponse updateQuantity(String userId, String productId, Integer quantity);

    /**
     * Clear all items from cart
     * @param userId the user ID
     */
    void clearCart(String userId);
}
