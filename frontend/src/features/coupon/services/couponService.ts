import apiClient from "@/services/api/apiClient";
import { Coupon, OrderRequest, OrderPreviewResponse } from "@/features/orders/types/order";
import { CouponResponse, CouponRequest, UpdateCouponRequest } from "../types/coupon";
import { ApiResponse } from "@/types/apiResponse";

const COUPON_API = "/v1/coupons";
const ORDER_API = "/v1/orders";

export const couponService = {
  /**
   * Get coupon by code
   */
  async getCoupon(code: string): Promise<Coupon> {
    const response = await apiClient.get<ApiResponse<Coupon>>(`${COUPON_API}/${code}`);
    return response.data.data;
  },

  /**
   * Get all coupons (Admin only)
   */
  async getAllCoupons(): Promise<CouponResponse[]> {
    const response = await apiClient.get<ApiResponse<CouponResponse[]>>(`${COUPON_API}`);
    return response.data.data;
  },

  /**
   * Validate if coupon is valid and active
   */
  async validateCoupon(code: string): Promise<boolean> {
    try {
      const response = await apiClient.get<ApiResponse<boolean>>(
        `${COUPON_API}/${code}/validity`
      );
      return response.data.data;
    } catch {
      return false;
    }
  },

  /**
   * Calculate discount amount for coupon and order amount
   */
  async calculateDiscount(code: string, orderAmount: number): Promise<number> {
    try {
      const response = await apiClient.get<ApiResponse<number>>(
        `${COUPON_API}/${code}/discounts?orderAmount=${orderAmount}`
      );
      return response.data.data;
    } catch {
      return 0;
    }
  },

  /**
   * Create new coupon (Admin only)
   */
  async createCoupon(request: CouponRequest): Promise<CouponResponse> {
    const response = await apiClient.post<ApiResponse<CouponResponse>>(
      COUPON_API,
      request
    );
    return response.data.data;
  },

  /**
   * Update coupon (Admin only)
   */
  async updateCoupon(code: string, request: UpdateCouponRequest): Promise<CouponResponse> {
    const response = await apiClient.put<ApiResponse<CouponResponse>>(
      `${COUPON_API}/${code}`,
      request
    );
    return response.data.data;
  },

  /**
   * Delete coupon (Admin only)
   */
  async deleteCoupon(code: string): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(`${COUPON_API}/${code}`);
  },

  /**
   * Preview order with coupon applied
   */
  async previewOrder(
    request: OrderRequest
  ): Promise<OrderPreviewResponse> {
    const response = await apiClient.post<ApiResponse<OrderPreviewResponse>>(
      `${ORDER_API}/previews`,
      request
    );
    return response.data.data;
  },
};
