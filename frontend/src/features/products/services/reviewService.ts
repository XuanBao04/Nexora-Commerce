import apiClient from "@/services/api/apiClient";
import { ApiResponse } from "@/types/apiResponse";
import { PaginationInfo } from "@/types/apiResponse";

export interface ProductReviewRequest {
  variantSku: string;
  orderId?: string;
  rating: number;
  comment: string;
  imageUrls?: string[];
}

export interface ProductReviewResponse {
  id: number;
  userId: string;
  username: string;
  variantSku: string;
  orderId?: string;
  rating: number;
  comment: string;
  createdAt: string;
  imageUrls?: string[];
  replies?: AdminReplyResponse[];
}

export interface AdminReplyResponse {
  id: number;
  reviewId: number;
  adminUsername: string;
  comment: string;
  createdAt: string;
}

export interface AdminReplyRequest {
  comment: string;
}

export interface PaginatedReviewsResponse {
  items: ProductReviewResponse[];
  pagination: PaginationInfo;
}

const REVIEWS_API = "/v1/reviews";
const VARIANTS_API = "/v1/variants";
const ADMIN_API = "/v1/admin/reviews";

export const reviewService = {
  /**
   * Create a product review
   * POST /v1/reviews
   */
  async createReview(
    reviewData: ProductReviewRequest,
  ): Promise<ProductReviewResponse> {
    const response = await apiClient.post<ApiResponse<ProductReviewResponse>>(
      REVIEWS_API,
      reviewData,
    );
    return response.data.data;
  },

  /**
   * Get reviews for a variant with pagination
   * GET /v1/variants/{variantSku}/reviews
   */
  async getVariantReviews(
    variantSku: string,
    page: number = 0,
    size: number = 10,
  ): Promise<PaginatedReviewsResponse> {
    const response = await apiClient.get<
      ApiResponse<ProductReviewResponse[]>
    >(`${VARIANTS_API}/${variantSku}/reviews`, {
      params: { page, size },
    });

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
   * Delete a product review
   * DELETE /v1/reviews/{reviewId}
   */
  async deleteReview(reviewId: number): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(`${REVIEWS_API}/${reviewId}`);
  },

  /**
   * Admin reply to a review
   * POST /v1/admin/reviews/{reviewId}/replies
   */
  async replyToReview(
    reviewId: number,
    replyData: AdminReplyRequest,
  ): Promise<AdminReplyResponse> {
    const response = await apiClient.post<ApiResponse<AdminReplyResponse>>(
      `${ADMIN_API}/${reviewId}/replies`,
      replyData,
    );
    return response.data.data;
  },
};
