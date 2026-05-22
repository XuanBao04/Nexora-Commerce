import { AxiosResponse } from 'axios';
import { ApiResponse } from '../types/apiResponse';

/**
 * Response Interceptor
 * Automatically unwraps ApiResponse<T> to T for easier use
 * 
 * Usage:
 * const response = await apiClient.get<Product[]>('/products');
 * // response.data is already unwrapped to Product[], not ApiResponse<Product[]>
 */
export const createResponseInterceptor = () => {
  return (response: AxiosResponse<any>) => {
    // Check if response has the ApiResponse structure
    if (response.data && response.data.status !== undefined && response.data.data !== undefined) {
      // This is an ApiResponse<T>, extract the data
      return {
        ...response,
        data: response.data.data,
        pagination: response.data.pagination,
      };
    }
    
    // Not an ApiResponse, return as is (for backward compatibility)
    return response;
  };
};

/**
 * Error response type assertion
 */
export const isApiError = (error: any): error is { response: { data: { error: string; message: string } } } => {
  return error?.response?.data?.error !== undefined && error?.response?.data?.message !== undefined;
};
