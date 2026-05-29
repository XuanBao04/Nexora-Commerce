import apiClient from "@/services/api/apiClient";
import { InventoryItem } from "../types/inventory";
import { ApiResponse } from "@/types/apiResponse";

const INVENTORY_API = "/v1/inventories";

export const inventoryService = {
  /**
   * Get available stock for a product
   * @param productId the product ID
   * @return available quantity
   */
  async getStock(productId: string): Promise<number> {
    const response = await apiClient.get<ApiResponse<number>>(
      `${INVENTORY_API}/${productId}/stock`,
    );
    return response.data.data;
  },

  /**
   * Get full inventory details for a product
   * @param productId the product ID
   * @return inventory details
   */
  async getInventoryDetails(productId: string): Promise<InventoryItem> {
    const response = await apiClient.get<ApiResponse<InventoryItem>>(
      `${INVENTORY_API}/${productId}`,
    );
    return response.data.data;
  },

  /**
   * Update an inventory item
   * @param productId the product ID
   * @param quantity the quantity to update
   */
  async updateInventoryItem(
    productId: string,
    quantity: number,
  ): Promise<void> {
    await apiClient.patch<ApiResponse<void>>(
      `${INVENTORY_API}/${productId}/stock`,
      { quantity },
    );
  },

  /**
   * Check if a product has enough stock for the requested quantity.
   * This helper is used by checkout flow before creating an order.
   * @param productId the product ID
   * @param requiredQuantity quantity requested by customer
   * @return true if inventory is enough, otherwise false
   */
  async checkStock(
    productId: string,
    requiredQuantity: number,
  ): Promise<boolean> {
    try {
      const response = await apiClient.get<ApiResponse<boolean>>(
        `${INVENTORY_API}/${productId}/availability`,
        { params: { quantity: requiredQuantity } }
      );
      return response.data.data;
    } catch (error) {
      // Fallback to getStock if /availability endpoint is not available or fails
      const stock = await this.getStock(productId);
      return stock >= requiredQuantity;
    }
  },

  /**
   * Reserve stock for an order
   * POST /v1/inventories/{productId}/stock-reservations
   */
  async reserveStock(
    productId: string,
    quantity: number,
  ): Promise<void> {
    await apiClient.post<ApiResponse<void>>(
      `${INVENTORY_API}/${productId}/stock-reservations`,
      { quantity },
    );
  },

  /**
   * Release reserved stock
   * POST /v1/inventories/{productId}/stock-releases
   */
  async releaseStock(
    productId: string,
    quantity: number,
  ): Promise<void> {
    await apiClient.post<ApiResponse<void>>(
      `${INVENTORY_API}/${productId}/stock-releases`,
      { quantity },
    );
  },
};
