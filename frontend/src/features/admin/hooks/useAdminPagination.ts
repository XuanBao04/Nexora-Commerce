import { useState, useCallback } from 'react';

export interface PaginationState {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export const useAdminPagination = (initialSize: number = 10) => {
  const [pagination, setPagination] = useState<PaginationState>({
    page: 0,
    size: initialSize,
    totalElements: 0,
    totalPages: 0,
    hasNext: false,
    hasPrevious: false,
  });

  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const nextPage = useCallback(() => {
    if (pagination.hasNext) {
      setPagination((prev) => ({ ...prev, page: prev.page + 1 }));
    }
  }, [pagination.hasNext]);

  const previousPage = useCallback(() => {
    if (pagination.hasPrevious) {
      setPagination((prev) => ({ ...prev, page: Math.max(0, prev.page - 1) }));
    }
  }, [pagination.hasPrevious]);

  const setPage = useCallback((pageNumber: number) => {
    if (pageNumber >= 0 && pageNumber < pagination.totalPages) {
      setPagination((prev) => ({ ...prev, page: pageNumber }));
    }
  }, [pagination.totalPages]);

  const setPageSize = useCallback((size: number) => {
    setPagination((prev) => ({ ...prev, size, page: 0 })); // Reset to first page on size change
  }, []);

  const updatePaginationData = useCallback((data: Partial<PaginationState>) => {
    setPagination((prev) => ({ ...prev, ...data }));
  }, []);

  return {
    page: pagination.page,
    size: pagination.size,
    pagination,
    isLoading,
    error,
    setIsLoading,
    setError,
    nextPage,
    previousPage,
    setPage,
    setPageSize,
    updatePaginationData,
  };
};
