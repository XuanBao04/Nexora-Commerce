import { useCallback, useState } from 'react';
import { PaginationInfo } from '../types/apiResponse';

/**
 * Hook for managing paginated API requests
 * Handles pagination state and provides helper functions
 */
export const usePagination = (initialPage = 0, initialPageSize = 10) => {
  const [page, setPage] = useState(initialPage);
  const [pageSize, setPageSize] = useState(initialPageSize);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(false);

  const updatePaginationInfo = useCallback((pagination: PaginationInfo) => {
    setPage(pagination.page);
    setPageSize(pagination.pageSize);
    setTotalPages(pagination.totalPages);
    setTotalElements(pagination.totalElements);
  }, []);

  const goToPage = useCallback((newPage: number) => {
    if (newPage >= 0 && newPage < totalPages) {
      setPage(newPage);
    }
  }, [totalPages]);

  const nextPage = useCallback(() => {
    if (page < totalPages - 1) {
      setPage(page + 1);
    }
  }, [page, totalPages]);

  const previousPage = useCallback(() => {
    if (page > 0) {
      setPage(page - 1);
    }
  }, [page]);

  const changePageSize = useCallback((newPageSize: number) => {
    setPageSize(newPageSize);
    setPage(0); // Reset to first page when changing page size
  }, []);

  return {
    // State
    page,
    pageSize,
    totalPages,
    totalElements,
    isLoading,
    setIsLoading,

    // Actions
    goToPage,
    nextPage,
    previousPage,
    changePageSize,
    updatePaginationInfo,

    // Computed
    canNextPage: page < totalPages - 1,
    canPreviousPage: page > 0,
  };
};
