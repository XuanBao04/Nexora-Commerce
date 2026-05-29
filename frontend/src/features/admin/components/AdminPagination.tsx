import React from 'react';
import { FaChevronLeft, FaChevronRight } from 'react-icons/fa';
import { PaginationState } from '@/features/admin/hooks/useAdminPagination';

interface AdminPaginationProps {
  pagination: PaginationState;
  onPageChange: (page: number) => void;
}

export const AdminPagination: React.FC<AdminPaginationProps> = ({ pagination, onPageChange }) => {
  const { totalPages, page: currentPage } = pagination;

  if (totalPages <= 1) return null;

  // Calculate the range of page numbers to show (e.g. max 10 pages)
  const maxVisiblePages = 10;
  let startPage = Math.max(0, currentPage - Math.floor(maxVisiblePages / 2));
  let endPage = Math.min(totalPages - 1, startPage + maxVisiblePages - 1);

  if (endPage - startPage + 1 < maxVisiblePages) {
    startPage = Math.max(0, endPage - maxVisiblePages + 1);
  }

  const pageNumbers = Array.from({ length: endPage - startPage + 1 }, (_, i) => startPage + i);

  return (
    <div className="flex flex-col items-center justify-center gap-4 border-t border-zinc-200/50 pt-10 pb-6 sm:flex-row">
      <div className="flex items-center gap-1.5 rounded-2xl bg-white/80 p-1.5 shadow-sm border border-zinc-200/50 backdrop-blur-sm">
        <button
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage === 0}
          className="icon-btn h-10 w-10 flex items-center justify-center rounded-xl border-0 shadow-none hover:bg-zinc-100 disabled:opacity-30 transition-all text-zinc-600"
          title="Trang trước"
        >
          <FaChevronLeft className="h-3.5 w-3.5" />
        </button>

        {pageNumbers.map((page) => (
          <button
            key={page}
            onClick={() => onPageChange(page)}
            className={`h-10 w-10 flex items-center justify-center rounded-xl text-xs font-bold transition-all duration-200 ${
              currentPage === page
                ? 'bg-zinc-950 text-white shadow-md'
                : 'text-zinc-600 hover:bg-zinc-100 hover:text-zinc-950'
            }`}
          >
            {page + 1}
          </button>
        ))}

        <button
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage === totalPages - 1}
          className="icon-btn h-10 w-10 flex items-center justify-center rounded-xl border-0 shadow-none hover:bg-zinc-100 disabled:opacity-30 transition-all text-zinc-600"
          title="Trang sau"
        >
          <FaChevronRight className="h-3.5 w-3.5" />
        </button>
      </div>
    </div>
  );
};
