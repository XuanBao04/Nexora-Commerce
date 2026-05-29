import apiClient from '@/services/api/apiClient';
import { BrandResponse, BrandRequest } from '../types/product';
import { ApiResponse } from '@/types/apiResponse';

const BRAND_API = '/v1/brands';

export const brandService = {
  /**
   * Get all brands list
   */
  async getAllBrands(): Promise<BrandResponse[]> {
    const response = await apiClient.get<ApiResponse<BrandResponse[]>>(BRAND_API);
    return response.data.data || [];
  },

  /**
   * Get brand by ID or slug
   */
  async getBrand(idOrSlug: string | number): Promise<BrandResponse> {
    const response = await apiClient.get<ApiResponse<BrandResponse>>(`${BRAND_API}/${idOrSlug}`);
    return response.data.data;
  },

  /**
   * Create a new brand (ADMIN only)
   */
  async createBrand(data: BrandRequest): Promise<BrandResponse> {
    const response = await apiClient.post<ApiResponse<BrandResponse>>(BRAND_API, data);
    return response.data.data;
  },

  /**
   * Update an existing brand (ADMIN only)
   */
  async updateBrand(brandId: number, data: BrandRequest): Promise<BrandResponse> {
    const response = await apiClient.put<ApiResponse<BrandResponse>>(`${BRAND_API}/${brandId}`, data);
    return response.data.data;
  },

  /**
   * Delete an existing brand (ADMIN only)
   */
  async deleteBrand(brandId: number): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(`${BRAND_API}/${brandId}`);
  }
};
