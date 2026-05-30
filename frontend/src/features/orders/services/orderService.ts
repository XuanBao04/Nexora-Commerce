/**
 * Updated Order Service with ApiResponse support
 * Includes pagination support for order lists
 */

import apiClient from '@/services/api/apiClient';
import { OrderResponse, OrderPreviewResponse, OrderRequest } from '../types/order';
import { PaginationInfo, ApiResponse } from '@/types/apiResponse';

const ORDER_API = '/v1/orders';
const CHECKOUT_API = '/v1/checkouts';
const ADMIN_ORDER_API = '/v1/admin/orders';

export interface PaginatedOrdersResponse {
  items: OrderResponse[];
  pagination: PaginationInfo;
}

export const orderService = {
  /**
   * Create a new order
   * Response: ApiResponse<OrderResponse>
   */
  async createOrder(_userId: string, orderRequest: OrderRequest): Promise<OrderResponse> {
    const response = await apiClient.post<ApiResponse<OrderResponse>>(
      `${ORDER_API}`,
      orderRequest
    );
    return response.data.data;
  },

  /**
   * Secure Checkout
   * Process checkout with Redis protection and generates payment URLs
   */
  async checkout(orderRequest: OrderRequest): Promise<OrderResponse> {
    const response = await apiClient.post<ApiResponse<OrderResponse>>(
      `${CHECKOUT_API}`,
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
      `${ORDER_API}/previews`,
      orderRequest
    );
    return response.data.data;
  },

  /**
   * Get all orders with pagination (ADMIN only)
   * Response: ApiResponse<OrderResponse[]> with pagination info
   */
  async getAllOrdersPaginated(
    page: number = 0,
    size: number = 10,
    sort: string = 'createdAt',
    search?: string
  ): Promise<PaginatedOrdersResponse> {
    const params: { page: number; size: number; sort: string; search?: string } = { page, size, sort };
    if (search) {
      params.search = search;
    }
    const response = await apiClient.get<ApiResponse<OrderResponse[]>>(ADMIN_ORDER_API, {
      params,
    });
    
    return {
      items: response.data.data,
      pagination: response.data.pagination!,
    };
  },

  /**
   * User confirm order delivery
   * Response: ApiResponse<OrderResponse>
   */
  async confirmDelivery(orderId: string): Promise<OrderResponse> {
    const response = await apiClient.post<ApiResponse<OrderResponse>>(
      `${ORDER_API}/${orderId}/confirm-delivery`
    );
    return response.data.data;
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
   * Get user orders (non-paginated, mock pagination with high size)
   * Response: ApiResponse<OrderResponse[]>
   */
  async getUserOrders(userId: string): Promise<OrderResponse[]> {
    const response = await apiClient.get<ApiResponse<OrderResponse[]>>(
      `${ORDER_API}`,
      {
        params: { userId, page: 0, size: 999 },
      }
    );
    return response.data.data;
  },

  /**
   * Get user orders with pagination
   * Response: ApiResponse<OrderResponse[]> with pagination info
   */
  async getUserOrdersPaginated(
    userId: string,
    page: number = 0,
    size: number = 10,
    sort: string = 'createdAt'
  ): Promise<PaginatedOrdersResponse> {
    const response = await apiClient.get<ApiResponse<OrderResponse[]>>(
      `${ORDER_API}`,
      {
        params: { userId, page, size, sort },
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
   * Update order status (User - PATCH)
   * Response: ApiResponse<OrderResponse>
   */
  async updateOrderStatusUser(orderId: string, status: string): Promise<OrderResponse> {
    const response = await apiClient.patch<ApiResponse<OrderResponse>>(
      `${ORDER_API}/${orderId}/status`,
      null,
      {
        params: { status },
      }
    );
    return response.data.data;
  },

  /**
   * Update order status (ADMIN - PUT)
   * Response: ApiResponse<OrderResponse>
   */
  async updateOrderStatus(orderId: string, status: string): Promise<OrderResponse> {
    const response = await apiClient.put<ApiResponse<OrderResponse>>(
      `${ADMIN_ORDER_API}/${orderId}/status`,
      null,
      {
        params: { status },
      }
    );
    return response.data.data;
  },
};
