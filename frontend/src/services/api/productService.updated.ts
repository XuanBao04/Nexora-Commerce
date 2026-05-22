/**
 * Updated Product Service with ApiResponse support
 * Includes pagination support for product list
 */

import apiClient from './apiClient';
import { Product } from '../../types/product';
import { PaginationInfo, ApiResponse } from '../../types/apiResponse';

const PRODUCT_API = '/products';

export interface PaginatedProductsResponse {
  items: Product[];
  pagination: PaginationInfo;
}

export const productService = {
  /**
   * Get all products (non-paginated)
   * Response: ApiResponse<Product[]>
   */
  async getAllProducts(): Promise<Product[]> {
    const response = await apiClient.get<ApiResponse<Product[]>>(`${PRODUCT_API}`);
    return response.data.data;
  },

  /**
   * Get all products with pagination
   * Response: ApiResponse<Product[]> with pagination info
   * 
   * Example:
   * const { data: products, pagination } = await productService.getAllProductsPaginated(0, 10, 'id');
   */
  async getAllProductsPaginated(
    page: number = 0,
    size: number = 10,
    sort: string = 'id'
  ): Promise<PaginatedProductsResponse> {
    const response = await apiClient.get<ApiResponse<Product[]>>(`${PRODUCT_API}/paginated`, {
      params: { page, size, sort },
    });
    
    return {
      items: response.data.data,
      pagination: response.data.pagination!,
    };
  },

  /**
   * Get product by ID
   * Response: ApiResponse<Product>
   */
  async getProductById(productId: string): Promise<Product> {
    const response = await apiClient.get<ApiResponse<Product>>(`${PRODUCT_API}/${productId}`);
    return response.data.data;
  },

  /**
   * Search products by keyword (non-paginated)
   * Response: ApiResponse<Product[]>
   */
  async searchProducts(keyword: string): Promise<Product[]> {
    const response = await apiClient.get<ApiResponse<Product[]>>(`${PRODUCT_API}/search`, {
      params: { keyword },
    });
    return response.data.data;
  },

  /**
   * Search products by keyword with pagination
   * Response: ApiResponse<Product[]> with pagination info
   * 
   * Example:
   * const { data: products, pagination } = await productService.searchProductsPaginated('shirt', 0, 10);
   */
  async searchProductsPaginated(
    keyword: string,
    page: number = 0,
    size: number = 10
  ): Promise<PaginatedProductsResponse> {
    const response = await apiClient.get<ApiResponse<Product[]>>(`${PRODUCT_API}/search`, {
      params: { keyword, page, size },
    });
    
    return {
      items: response.data.data,
      pagination: response.data.pagination!,
    };
  },

  /**
   * Get available stock for product
   * Response: ApiResponse<number>
   */
  async getAvailableStock(productId: string): Promise<number> {
    const response = await apiClient.get<ApiResponse<number>>(`${PRODUCT_API}/${productId}/stock`);
    return response.data.data;
  },

  /**
   * Check if product is available
   * Response: ApiResponse<boolean>
   */
  async isProductAvailable(productId: string): Promise<boolean> {
    const response = await apiClient.get<ApiResponse<boolean>>(`${PRODUCT_API}/${productId}/availability`);
    return response.data.data;
  },

  /**
   * Create product (ADMIN only)
   * Response: ApiResponse<Product>
   */
  async createProduct(productData: any): Promise<Product> {
    const response = await apiClient.post<ApiResponse<Product>>(`${PRODUCT_API}`, productData);
    return response.data.data;
  },

  /**
   * Update product (ADMIN only)
   * Response: ApiResponse<Product>
   */
  async updateProduct(productId: string, productData: any): Promise<Product> {
    const response = await apiClient.put<ApiResponse<Product>>(`${PRODUCT_API}/${productId}`, productData);
    return response.data.data;
  },

  /**
   * Delete product (ADMIN only)
   * Response: ApiResponse<void>
   */
  async deleteProduct(productId: string): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(`${PRODUCT_API}/${productId}`);
  },
};
