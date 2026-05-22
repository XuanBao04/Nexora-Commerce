import apiClient from "./apiClient";
import { CartResponse, CartItemRequest } from "../../types/cart";
import { ApiResponse } from "../../types/apiResponse";

const CART_API = "/cart";

export const cartService = {
  /**
   * Get cart for user
   */
  async getCart(userId: string): Promise<CartResponse> {
    const response = await apiClient.get<ApiResponse<CartResponse>>(`${CART_API}/${userId}`);
    return response.data.data;
  },

  /**
   * Add item to cart
   */
  async addToCart(
    userId: string,
    item: CartItemRequest,
  ): Promise<CartResponse> {
    const response = await apiClient.post<ApiResponse<CartResponse>>(
      `${CART_API}/${userId}/add`,
      item,
    );
    return response.data.data;
  },

  /**
   * Remove item from cart
   */
  async removeFromCart(
    userId: string,
    cartItemId: number,
  ): Promise<CartResponse> {
    const response = await apiClient.delete<ApiResponse<CartResponse>>(
      `${CART_API}/${userId}/items/${cartItemId}`,
    );
    return response.data.data;
  },

  /**
   * Update cart item quantity
   */
  async updateCartItem(
    userId: string,
    cartItemId: number,
    quantity: number,
  ): Promise<CartResponse> {
    try {
      const response = await apiClient.patch<ApiResponse<CartResponse>>(
        `${CART_API}/${userId}/items/${cartItemId}`,
        { quantity: quantity },
      );
      return response.data.data;
    } catch (error) {
      console.error("Error updating cart item quantity:", error);
      throw error;
    }
  },

  /**
   * Clear entire cart
   */
  async clearCart(userId: string): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(`${CART_API}/${userId}`);
  },
};
