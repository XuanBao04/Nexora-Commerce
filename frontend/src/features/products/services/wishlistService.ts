import apiClient from "@/services/api/apiClient";
import { ApiResponse } from "@/types/apiResponse";
import { PaginationInfo } from "@/types/apiResponse";

export interface WishlistRequest {
  productId: string;
}

export interface WishlistResponse {
  id: number;
  userId: string;
  productId: string;
  productName: string;
  productPrice: number;
  createdAt: string;
}

export interface PaginatedWishlistResponse {
  items: WishlistResponse[];
  pagination: PaginationInfo;
}

const WISHLISTS_API = "/v1/wishlists";

export const wishlistService = {
  /**
   * Add item to wishlist
   * POST /v1/wishlists/{userId}/items
   */
  async addToWishlist(
    userId: string,
    wishlistData: WishlistRequest,
  ): Promise<WishlistResponse> {
    const response = await apiClient.post<ApiResponse<WishlistResponse>>(
      `${WISHLISTS_API}/${userId}/items`,
      wishlistData,
    );
    return response.data.data;
  },

  /**
   * Get user wishlist with pagination
   * GET /v1/wishlists/{userId}
   */
  async getWishlist(
    userId: string,
    page: number = 0,
    size: number = 10,
  ): Promise<PaginatedWishlistResponse> {
    const response = await apiClient.get<ApiResponse<WishlistResponse[]>>(
      `${WISHLISTS_API}/${userId}`,
      {
        params: { page, size },
      },
    );

    return {
      items: response.data.data || [],
      pagination: response.data.pagination || {
        page,
        pageSize: size,
        totalElements: (response.data.data || []).length,
        totalPages: 1,
        hasNext: false,
        hasPrevious: false,
      },
    };
  },

  /**
   * Remove item from wishlist
   * DELETE /v1/wishlists/{userId}/items/{productId}
   */
  async removeFromWishlist(
    userId: string,
    productId: string,
  ): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(
      `${WISHLISTS_API}/${userId}/items/${productId}`,
    );
  },
};
