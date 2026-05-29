import apiClient from "@/services/api/apiClient";
import { ApiResponse } from "@/types/apiResponse";

export interface StockQuantityRequest {
  quantity: number;
}

export interface StockStatus {
  productId: string;
  redisStock: number;
  locked: boolean;
  lastSyncTimestamp: number;
}

const ADMIN_STOCKS_API = "/v1/admin/stocks";

export const adminStockService = {
  /**
   * Get product stock status
   * GET /v1/admin/stocks/{productId}
   */
  async getProductStock(productId: string): Promise<StockStatus> {
    const response = await apiClient.get<ApiResponse<StockStatus>>(
      `${ADMIN_STOCKS_API}/${productId}`,
    );
    return response.data.data;
  },

  /**
   * Set product stock quantity
   * PUT /v1/admin/stocks/{productId}
   */
  async setProductStock(
    productId: string,
    stockData: StockQuantityRequest,
  ): Promise<StockStatus> {
    const response = await apiClient.put<ApiResponse<StockStatus>>(
      `${ADMIN_STOCKS_API}/${productId}`,
      stockData,
    );
    return response.data.data;
  },

  /**
   * Delete product stock
   * DELETE /v1/admin/stocks/{productId}
   */
  async deleteProductStock(productId: string): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(
      `${ADMIN_STOCKS_API}/${productId}`,
    );
  },

  /**
   * Clear all product stocks
   * DELETE /v1/admin/stocks
   */
  async clearAllStock(): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(ADMIN_STOCKS_API);
  },

  /**
   * Sync single product stock
   * POST /v1/admin/stocks/{productId}/synchronizations
   */
  async syncProductStock(productId: string): Promise<void> {
    await apiClient.post<ApiResponse<void>>(
      `${ADMIN_STOCKS_API}/${productId}/synchronizations`,
    );
  },

  /**
   * Sync all products stock
   * POST /v1/admin/stocks/synchronizations
   */
  async syncAllStock(): Promise<void> {
    await apiClient.post<ApiResponse<void>>(
      `${ADMIN_STOCKS_API}/synchronizations`,
    );
  },
};
