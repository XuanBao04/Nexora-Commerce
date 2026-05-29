import apiClient from '@/services/api/apiClient';
import { CategoryResponse, CategoryRequest } from '../types/product';
import { ApiResponse } from '@/types/apiResponse';

const CATEGORY_API = '/v1/categories';

export const categoryService = {
  /**
   * Get full category tree structure
   */
  async getCategoryTree(): Promise<CategoryResponse[]> {
    const response = await apiClient.get<ApiResponse<CategoryResponse[]>>(CATEGORY_API);
    return response.data.data || [];
  },

  /**
   * Get category by ID or slug
   */
  async getCategory(idOrSlug: string | number): Promise<CategoryResponse> {
    const response = await apiClient.get<ApiResponse<CategoryResponse>>(`${CATEGORY_API}/${idOrSlug}`);
    return response.data.data;
  },

  /**
   * Create a new category (ADMIN only)
   */
  async createCategory(data: CategoryRequest): Promise<CategoryResponse> {
    const response = await apiClient.post<ApiResponse<CategoryResponse>>(CATEGORY_API, data);
    return response.data.data;
  },

  /**
   * Update an existing category (ADMIN only)
   */
  async updateCategory(categoryId: number, data: CategoryRequest): Promise<CategoryResponse> {
    const response = await apiClient.put<ApiResponse<CategoryResponse>>(`${CATEGORY_API}/${categoryId}`, data);
    return response.data.data;
  },

  /**
   * Delete an existing category (ADMIN only)
   */
  async deleteCategory(categoryId: number): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(`${CATEGORY_API}/${categoryId}`);
  }
};
