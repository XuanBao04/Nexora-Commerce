import apiClient from '@/services/api/apiClient';
import { ApiResponse, PaginationInfo } from '@/types/apiResponse';
import { OrderResponse } from '@/features/orders/types/order';

const ADMIN_API = '/v1/admin';

export interface PaginatedAdminResponse<T> {
  items: T[];
  pagination: PaginationInfo;
}

export const adminApiService = {
  /**
   * Fetch all users for admin with pagination
   */
  async getAllUsers(page: number = 0, size: number = 10, keyword: string = '') {
    const searchParams = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });
    if (keyword) {
      searchParams.append('keyword', keyword.trim());
    }
    const response = await apiClient.get<ApiResponse<any[]>>(`${ADMIN_API}/users?${searchParams.toString()}`);
    return {
      items: response.data.data || [],
      pagination: response.data.pagination,
    };
  },

  /**
   * Fetch all products for admin with pagination
   */
  async getAllProducts(page: number = 0, size: number = 10, keyword: string = '') {
    const searchParams = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });
    if (keyword) {
      searchParams.append('keyword', keyword.trim());
    }
    const response = await apiClient.get<ApiResponse<any[]>>(`${ADMIN_API}/products?${searchParams.toString()}`);
    return {
      items: response.data.data || [],
      pagination: response.data.pagination,
    };
  },

  /**
   * Fetch all orders for admin with pagination
   */
  async getAllOrders(page: number = 0, size: number = 10): Promise<PaginatedAdminResponse<OrderResponse>> {
    const searchParams = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });
    const response = await apiClient.get<ApiResponse<OrderResponse[]>>(`${ADMIN_API}/orders?${searchParams.toString()}`);
    const items = response.data.data || [];
    return {
      items,
      pagination: response.data.pagination || {
        page,
        pageSize: size,
        totalElements: items.length,
        totalPages: items.length > 0 ? 1 : 0,
        hasNext: false,
        hasPrevious: false,
      },
    };
  },

  /**
   * Update order status
   */
  async updateOrderStatus(orderId: string, status: string): Promise<OrderResponse> {
    const response = await apiClient.put<ApiResponse<OrderResponse>>(
      `${ADMIN_API}/orders/${orderId}/status`,
      null,
      { params: { status } }
    );
    return response.data.data;
  },

  /**
   * Fetch all categories for admin with pagination
   */
  async getAllCategories(page: number = 0, size: number = 10) {
    const searchParams = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });
    const response = await apiClient.get<ApiResponse<any[]>>(`${ADMIN_API}/categories?${searchParams.toString()}`);
    return {
      items: response.data.data || [],
      pagination: response.data.pagination,
    };
  },

  /**
   * Fetch all brands for admin with pagination
   */
  async getAllBrands(page: number = 0, size: number = 10) {
    const searchParams = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });
    const response = await apiClient.get<ApiResponse<any[]>>(`${ADMIN_API}/brands?${searchParams.toString()}`);
    return {
      items: response.data.data || [],
      pagination: response.data.pagination,
    };
  }
};
