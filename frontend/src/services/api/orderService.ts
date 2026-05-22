/**
 * Updated Order Service with ApiResponse support
 * Includes pagination support for order lists
 */

import apiClient from './apiClient';
import { OrderResponse, OrderPreviewResponse, OrderRequest } from '../../types/order';
import { PaginationInfo, ApiResponse } from '../../types/apiResponse';

const ORDER_API = '/orders';

export interface PaginatedOrdersResponse {
  items: OrderResponse[];
  pagination: PaginationInfo;
}

export const orderService = {
  /**
   * Create a new order
   * Response: ApiResponse<OrderResponse>
   */
  async createOrder(userId: string, orderRequest: OrderRequest): Promise<OrderResponse> {
    const response = await apiClient.post<ApiResponse<OrderResponse>>(
      `${ORDER_API}/${userId}`,
      orderRequest
    );
    return response.data.data;
  },

  /**
   * Preview order before checkout
   * Response: ApiResponse<OrderPreviewResponse>
   */
  async previewOrder(orderRequest: OrderRequest): Promise<OrderPreviewResponse> {
    const response = await apiClient.post<ApiResponse<OrderPreviewResponse>>(
      `${ORDER_API}/preview`,
      orderRequest
    );
    return response.data.data;
  },

  /**
   * Get all orders (ADMIN only, non-paginated)
   * Response: ApiResponse<OrderResponse[]>
   */
  async getAllOrders(): Promise<OrderResponse[]> {
    const response = await apiClient.get<ApiResponse<OrderResponse[]>>(`${ORDER_API}/all`);
    return response.data.data;
  },

  /**
   * Get all orders with pagination (ADMIN only)
   * Response: ApiResponse<OrderResponse[]> with pagination info
   * 
   * Example:
   * const { data: orders, pagination } = await orderService.getAllOrdersPaginated(0, 10, 'createdAt');
   */
  async getAllOrdersPaginated(
    page: number = 0,
    size: number = 10,
    sort: string = 'createdAt'
  ): Promise<PaginatedOrdersResponse> {
    const response = await apiClient.get<ApiResponse<OrderResponse[]>>(`${ORDER_API}`, {
      params: { page, size, sort },
    });
    
    return {
      items: response.data.data,
      pagination: response.data.pagination!,
    };
  },

  /**
   * Get order by ID
   * Response: ApiResponse<OrderResponse>
   */
  async getOrderById(orderId: string): Promise<OrderResponse> {
    const response = await apiClient.get<ApiResponse<OrderResponse>>(`${ORDER_API}/${orderId}`);
    return response.data.data;
  },

  /**
   * Get user orders (non-paginated)
   * Response: ApiResponse<OrderResponse[]>
   */
  async getUserOrders(userId: string): Promise<OrderResponse[]> {
    const response = await apiClient.get<ApiResponse<OrderResponse[]>>(
      `${ORDER_API}/user/${userId}/all`
    );
    return response.data.data;
  },

  /**
   * Get user orders with pagination
   * Response: ApiResponse<OrderResponse[]> with pagination info
   * 
   * Example:
   * const { data: orders, pagination } = await orderService.getUserOrdersPaginated(userId, 0, 10);
   */
  async getUserOrdersPaginated(
    userId: string,
    page: number = 0,
    size: number = 10,
    sort: string = 'createdAt'
  ): Promise<PaginatedOrdersResponse> {
    const response = await apiClient.get<ApiResponse<OrderResponse[]>>(
      `${ORDER_API}/user/${userId}`,
      {
        params: { page, size, sort },
      }
    );
    
    return {
      items: response.data.data,
      pagination: response.data.pagination!,
    };
  },

  /**
   * Cancel order
   * Response: ApiResponse<OrderResponse>
   */
  async cancelOrder(orderId: string): Promise<OrderResponse> {
    const response = await apiClient.delete<ApiResponse<OrderResponse>>(
      `${ORDER_API}/${orderId}`
    );
    return response.data.data;
  },

  /**
   * Update order status (ADMIN only)
   * Response: ApiResponse<OrderResponse>
   * 
   * Valid status values: PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED
   */
  async updateOrderStatus(orderId: string, status: string): Promise<OrderResponse> {
    const response = await apiClient.patch<ApiResponse<OrderResponse>>(
      `${ORDER_API}/${orderId}/${status}`
    );
    return response.data.data;
  },
};
