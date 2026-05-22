/**
 * API Response Types
 * Defines the standardized response format from the backend
 */

export interface PaginationInfo {
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface ApiResponse<T> {
  timestamp: string;
  status: number;
  message: string;
  data: T;
  pagination?: PaginationInfo;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

/**
 * Helper to extract data from ApiResponse
 * Usage: const products = unwrap(response)
 */
export const unwrap = <T>(response: ApiResponse<T>): T => {
  return response.data;
};

/**
 * Helper to get pagination info from ApiResponse
 * Usage: const pagination = getPagination(response)
 */
export const getPagination = <T>(response: ApiResponse<T>): PaginationInfo | undefined => {
  return response.pagination;
};

/**
 * Helper to check if there are more pages
 */
export const hasMorePages = <T>(response: ApiResponse<T>): boolean => {
  return response.pagination?.hasNext ?? false;
};
