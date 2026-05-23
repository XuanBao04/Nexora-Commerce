import { useEffect, useState } from 'react';
import { productService } from '@features/products/services/productService';
import { Product } from '../types/product';
import ProductCard from './ProductCard';
import { FaChevronLeft, FaChevronRight, FaSearch, FaStore } from 'react-icons/fa';

const ProductList = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  // Pagination State
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const pageSize = 6;

  useEffect(() => {
    const fetchProducts = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const response = await productService.getAllProductsPaginated(currentPage, pageSize, 'id');
        setProducts(response.items);
        setTotalPages(response.pagination.totalPages);
        setTotalElements(response.pagination.totalElements);
      } catch (err) {
        setError((err as Error).message || "Không thể tải danh sách sản phẩm.");
      } finally {
        setIsLoading(false);
      }
    };

    fetchProducts();
  }, [currentPage]);

  const handlePageChange = (page: number) => {
    if (page >= 0 && page < totalPages) {
      setCurrentPage(page);
      // Smooth scroll to top of product list
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-8 animate-fade-in">
        <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
          <div className="space-y-3">
            <div className="skeleton h-8 w-64" />
            <div className="skeleton h-4 w-80 max-w-full" />
          </div>
          <div className="skeleton h-11 w-full md:w-72" />
        </div>
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: pageSize }).map((_, index) => (
            <div key={index} className="surface overflow-hidden border border-zinc-200/50">
              <div className="skeleton aspect-[4/3] rounded-none bg-zinc-200/50" />
              <div className="space-y-4 p-6">
                <div className="skeleton h-5 w-3/4" />
                <div className="skeleton h-4 w-full" />
                <div className="skeleton h-11 w-full" />
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="empty-state mx-auto max-w-lg border-rose-200/60 bg-rose-50/50 p-8 rounded-3xl text-center">
        <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-rose-100 text-rose-600 mb-4">
          <FaStore className="h-5 w-5" />
        </div>
        <p className="mb-2 text-lg font-extrabold text-rose-950">Đã xảy ra lỗi</p>
        <p className="mb-6 text-sm font-medium text-rose-600/80">{error}</p>
        <button 
          onClick={() => setCurrentPage(0)}
          className="btn-danger inline-flex px-6"
        >
          Thử lại
        </button>
      </div>
    );
  }

  if (products.length === 0) {
    return (
      <div className="empty-state bg-white/40 border border-dashed border-zinc-200/60 p-12">
        <FaStore className="mb-4 h-12 w-12 text-zinc-300" />
        <p className="text-base font-extrabold text-zinc-700">Không có sản phẩm nào khả dụng.</p>
        <p className="text-xs font-semibold text-zinc-400 mt-1">Vui lòng quay lại hoặc liên hệ quản trị viên.</p>
      </div>
    );
  }

  const pageNumbers = [];
  for (let i = 0; i < totalPages; i++) {
    pageNumbers.push(i);
  }

  const startElement = currentPage * pageSize + 1;
  const endElement = Math.min((currentPage + 1) * pageSize, totalElements);

  return (
    <div className="animate-fade-in space-y-8 lg:space-y-10">
      {/* Editorial Header list section */}
      <div className="flex flex-col justify-between gap-6 md:flex-row md:items-end">
        <div className="space-y-2">
          <div className="inline-flex items-center gap-1.5 rounded-full border border-amber-200/50 bg-amber-50/60 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-amber-800">
            Aetheris Selection
          </div>
          <h1 className="page-heading">Danh sách sản phẩm</h1>
          <p className="page-subtitle">
            Hiển thị {startElement} - {endElement} trên tổng số {totalElements} sản phẩm cao cấp
          </p>
        </div>
        <div className="surface flex h-11 w-full items-center gap-3 px-4 text-xs text-zinc-500 md:w-80 bg-white/60">
          <FaSearch className="h-3.5 w-3.5 text-zinc-400" />
          <span className="font-bold uppercase tracking-wider text-zinc-400/80">Bộ lọc đang được đồng bộ</span>
        </div>
      </div>

      {/* Elegant Products Grid */}
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
        {products.map((product, index) => (
          <div key={product.id} className={`animate-fade-in stagger-${(index % 6) + 1}`}>
            <ProductCard product={product} />
          </div>
        ))}
      </div>

      {/* Pagination Controls */}
      {totalPages > 1 && (
        <div className="flex flex-col items-center justify-center gap-4 border-t border-zinc-200/50 pt-10 sm:flex-row">
          <div className="flex items-center gap-1.5 rounded-2xl bg-white/80 p-1.5 shadow-sm border border-zinc-200/50 backdrop-blur-sm">
            <button
              onClick={() => handlePageChange(currentPage - 1)}
              disabled={currentPage === 0}
              className="icon-btn border-0 shadow-none hover:bg-zinc-100 disabled:opacity-30"
              title="Trang trước"
            >
              <FaChevronLeft className="h-3.5 w-3.5" />
            </button>

            {pageNumbers.map((page) => (
              <button
                key={page}
                onClick={() => handlePageChange(page)}
                className={`h-10 w-10 rounded-xl text-xs font-bold transition-all duration-200 ${
                  currentPage === page
                    ? 'bg-zinc-950 text-white shadow-md'
                    : 'text-zinc-600 hover:bg-zinc-100 hover:text-zinc-950'
                }`}
              >
                {page + 1}
              </button>
            ))}

            <button
              onClick={() => handlePageChange(currentPage + 1)}
              disabled={currentPage === totalPages - 1}
              className="icon-btn border-0 shadow-none hover:bg-zinc-100 disabled:opacity-30"
              title="Trang sau"
            >
              <FaChevronRight className="h-3.5 w-3.5" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default ProductList;
