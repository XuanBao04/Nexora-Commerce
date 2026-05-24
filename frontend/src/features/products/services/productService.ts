import apiClient from '@/services/api/apiClient';
import { Product, ProductResponse } from '../types/product';
import { PaginationInfo, ApiResponse } from '@/types/apiResponse';

const PRODUCT_API = '/v1/products';

export interface PaginatedProductsResponse {
  items: Product[];
  pagination: PaginationInfo;
}

export const productService = {
  /**
   * Get all products by paging through them
   */
  async getAllProducts(keyword: string = ''): Promise<Product[]> {
    const pageSize = 50;
    let page = 0;
    let hasNext = true;
    const allProducts: Product[] = [];

    while (hasNext) {
      const { items, pagination } = await this.getAllProductsPaginated(page, pageSize, 'id', keyword);
      allProducts.push(...items);
      hasNext = pagination?.hasNext ?? false;
      page += 1;
    }

    return allProducts;
  },

  /**
   * Get paginated products with required keyword
   */
  async getAllProductsPaginated(
    page: number = 0,
    size: number = 10,
    sort: string = 'id',
    keyword: string = '',
    categoryId: number | null = null,
    brandId: number | null = null,
    minPrice: number | null = null,
    maxPrice: number | null = null
  ): Promise<PaginatedProductsResponse> {
    const searchParams = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      sort: sort,
    });

    const trimmedKeyword = keyword.trim();
    if (trimmedKeyword) {
      searchParams.append('keyword', trimmedKeyword);
    }
    if (categoryId !== null) {
      searchParams.append('categoryId', categoryId.toString());
    }
    if (brandId !== null) {
      searchParams.append('brandId', brandId.toString());
    }
    if (minPrice !== null) {
      searchParams.append('minPrice', minPrice.toString());
    }
    if (maxPrice !== null) {
      searchParams.append('maxPrice', maxPrice.toString());
    }

    const response = await apiClient.get<ApiResponse<ProductResponse[]>>(`${PRODUCT_API}?${searchParams.toString()}`);

    // The data might be inside response.data.data
    const data = response.data;
    
    return {
      items: data.data || [],
      pagination: data.pagination || {
        page: page,
        pageSize: size,
        totalElements: (data.data || []).length,
        totalPages: 1,
        hasNext: false,
        hasPrevious: false,
      },
    };
  },

  /**
   * Get product by ID
   */
  async getProductById(productId: string): Promise<Product> {
    const response = await apiClient.get<ApiResponse<ProductResponse>>(`${PRODUCT_API}/${productId}`);
    return response.data.data;
  },

  /**
   * Search products by keyword
   */
  async searchProducts(keyword: string): Promise<Product[]> {
    return this.getAllProducts(keyword);
  },

  /**
   * Search products by keyword with pagination
   */
  async searchProductsPaginated(
    keyword: string,
    page: number = 0,
    size: number = 10
  ): Promise<PaginatedProductsResponse> {
    return this.getAllProductsPaginated(page, size, 'id', keyword);
  },

  /**
   * Create product (ADMIN only)
   * Sends productData as FormData to POST /api/v1/products
   */
  async createProduct(productData: FormData): Promise<Product> {
    const response = await apiClient.post<ApiResponse<ProductResponse>>(PRODUCT_API, productData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data.data;
  },

  /**
   * Update product (ADMIN only)
   * Sends productData as FormData to PUT /api/v1/products/{productId}
   */
  async updateProduct(productId: string, productData: FormData): Promise<Product> {
    const response = await apiClient.put<ApiResponse<ProductResponse>>(`${PRODUCT_API}/${productId}`, productData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data.data;
  },

  /**
   * Delete product (ADMIN only)
   */
  async deleteProduct(productId: string): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(`${PRODUCT_API}/${productId}`);
  },

  /**
   * Utility helper to get available stock of a product (sum of variant quantities)
   */
  async getAvailableStock(productId: string): Promise<number> {
    try {
      const product = await this.getProductById(productId);
      if (!product || !product.variants) return 0;
      return product.variants.reduce((sum, v) => sum + (v.quantity || 0), 0);
    } catch {
      return 0;
    }
  }
};
