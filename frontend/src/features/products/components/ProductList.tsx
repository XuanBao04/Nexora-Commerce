import { useEffect, useState, useTransition, useMemo } from 'react';
import { productService } from '@features/products/services/productService';
import { categoryService } from '@features/products/services/categoryService';
import { brandService } from '@features/products/services/brandService';
import { Product, CategoryResponse, BrandResponse } from '../types/product';
import ProductCard from './ProductCard';
import { ProductDetailModal } from './ProductDetailModal';
import { flattenCategoryTree } from '../utils/categoryHelper';
import { FaChevronLeft, FaChevronRight, FaSearch, FaStore, FaSlidersH, FaTimes } from 'react-icons/fa';

const ProductList = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Categories & Brands filter lists
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [brands, setBrands] = useState<BrandResponse[]>([]);

  // Selected filters
  const [selectedCategory, setSelectedCategory] = useState<number | null>(null);
  
  const flatCategories = useMemo(() => flattenCategoryTree(categories), [categories]);
  const [selectedBrand, setSelectedBrand] = useState<number | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [, startTransition] = useTransition();

  // Price Filters State
  const [minPriceInput, setMinPriceInput] = useState<string>('');
  const [maxPriceInput, setMaxPriceInput] = useState<string>('');
  const [debouncedMinPrice, setDebouncedMinPrice] = useState<number | null>(null);
  const [debouncedMaxPrice, setDebouncedMaxPrice] = useState<number | null>(null);
  const [priceError, setPriceError] = useState<string | null>(null);

  // Currency Masking helper functions
  const formatNumberWithCommas = (value: string) => {
    const cleanValue = value.replace(/\D/g, '');
    if (!cleanValue) return '';
    return new Intl.NumberFormat('en-US').format(Number(cleanValue));
  };

  const getRawNumber = (value: string) => {
    const cleanValue = value.replace(/\D/g, '');
    return cleanValue ? Number(cleanValue) : null;
  };

  // Validation and Debounce Effect for Price Inputs
  useEffect(() => {
    const rawMin = getRawNumber(minPriceInput);
    const rawMax = getRawNumber(maxPriceInput);

    if (rawMin !== null && rawMax !== null && rawMin > rawMax) {
      setPriceError('Giá tối thiểu không được lớn hơn giá tối đa');
      return;
    }

    setPriceError(null);

    const handler = setTimeout(() => {
      setDebouncedMinPrice(rawMin);
      setDebouncedMaxPrice(rawMax);
      setCurrentPage(0);
    }, 400);

    return () => clearTimeout(handler);
  }, [minPriceInput, maxPriceInput]);

  // Detail Modal state
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  
  // Pagination State
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const pageSize = 12;

  // Search input change with debounce transition
  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setSearchTerm(value);
    
    // Simulate simple 300ms debounce
    const handler = setTimeout(() => {
      startTransition(() => {
        setDebouncedSearch(value);
        setCurrentPage(0);
      });
    }, 300);
    return () => clearTimeout(handler);
  };

  // Fetch Category Tree and Brands on mount
  useEffect(() => {
    const fetchMetadata = async () => {
      try {
        const [cats, brs] = await Promise.all([
          categoryService.getCategoryTree(),
          brandService.getAllBrands(),
        ]);
        setCategories(cats);
        setBrands(brs);
      } catch (err) {
        console.error("Error loading categories or brands:", err);
      }
    };
    fetchMetadata();
  }, []);

  // Fetch Products based on page, keyword search, category, brand, and price filters
  useEffect(() => {
    if (priceError) return; // API Guard: block fetch when price inputs are invalid

    const fetchProducts = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const response = await productService.getAllProductsPaginated(
          currentPage,
          pageSize,
          'id',
          debouncedSearch,
          selectedCategory,
          selectedBrand,
          debouncedMinPrice,
          debouncedMaxPrice
        );
        
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
  }, [currentPage, debouncedSearch, selectedCategory, selectedBrand, debouncedMinPrice, debouncedMaxPrice, priceError]);

  const handlePageChange = (page: number) => {
    if (page >= 0 && page < totalPages) {
      setCurrentPage(page);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  const handleClearFilters = () => {
    setSelectedCategory(null);
    setSelectedBrand(null);
    setSearchTerm('');
    setDebouncedSearch('');
    setMinPriceInput('');
    setMaxPriceInput('');
    setDebouncedMinPrice(null);
    setDebouncedMaxPrice(null);
    setPriceError(null);
    setCurrentPage(0);
  };



  const pageNumbers = Array.from({ length: totalPages }, (_, i) => i);
  const startElement = totalElements > 0 ? currentPage * pageSize + 1 : 0;
  const endElement = Math.min((currentPage + 1) * pageSize, totalElements);

  return (
    <div className="animate-fade-in space-y-8 lg:space-y-10">
      
      {/* Editorial Header Section */}
      <div className="flex flex-col justify-between gap-6 md:flex-row md:items-end border-b border-zinc-150 pb-6">
        <div className="space-y-2">
          <div className="inline-flex items-center gap-1.5 rounded-full border border-amber-200/50 bg-amber-50/60 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-amber-800">
            Aetheris Catalogue
          </div>
          <h1 className="page-heading">Danh sách sản phẩm</h1>
          <p className="page-subtitle">
            {totalElements > 0 
              ? `Hiển thị ${startElement} - ${endElement} trên tổng số ${totalElements} sản phẩm cao cấp`
              : "Không tìm thấy sản phẩm nào phù hợp với bộ lọc"
            }
          </p>
        </div>

        {/* Search input with live synch */}
        <div className="relative w-full md:w-80">
          <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-zinc-400">
            <FaSearch className="h-3.5 w-3.5" />
          </div>
          <input
            type="text"
            placeholder="Tìm sản phẩm theo tên..."
            value={searchTerm}
            onChange={handleSearchChange}
            className="w-full pl-10 pr-4 h-11 bg-zinc-50 border border-zinc-200/60 focus:bg-white focus:border-zinc-950 focus:ring-4 focus:ring-zinc-900/5 rounded-xl text-xs font-semibold outline-none transition duration-200 text-zinc-800 placeholder:text-zinc-400"
          />
        </div>
      </div>

      {/* Filter Options Bar */}
      <div className="surface p-4 bg-white/60 border border-zinc-250/50 rounded-2xl flex flex-wrap gap-4 items-center justify-between">
        <div className="flex flex-wrap items-center gap-3.5">
          <span className="flex items-center gap-1.5 text-[10px] font-black text-zinc-400 uppercase tracking-widest">
            <FaSlidersH className="w-3 h-3" />
            Bộ lọc:
          </span>

          {/* Category Selector */}
          <select
            value={selectedCategory || ''}
            onChange={(e) => {
              setSelectedCategory(e.target.value ? Number(e.target.value) : null);
              setCurrentPage(0);
            }}
            className="h-10 px-3.5 bg-zinc-50 border border-zinc-200/60 hover:border-zinc-300 focus:bg-white focus:border-zinc-950 rounded-xl text-xs font-bold text-zinc-600 outline-none transition"
          >
            <option value="">Tất cả danh mục</option>
            {flatCategories.map((cat) => (
              <option key={cat.id} value={cat.id}>
                {cat.displayName}
              </option>
            ))}
          </select>

          {/* Brand Selector */}
          <select
            value={selectedBrand || ''}
            onChange={(e) => {
              setSelectedBrand(e.target.value ? Number(e.target.value) : null);
              setCurrentPage(0);
            }}
            className="h-10 px-3.5 bg-zinc-50 border border-zinc-200/60 hover:border-zinc-300 focus:bg-white focus:border-zinc-950 rounded-xl text-xs font-bold text-zinc-600 outline-none transition"
          >
            <option value="">Tất cả thương hiệu</option>
            {brands.map((brand) => (
              <option key={brand.id} value={brand.id}>
                {brand.name}
              </option>
            ))}
          </select>

          {/* Price Filters with dynamic thousand separator masking */}
          <div className="flex items-center gap-2">
            <input
              type="text"
              placeholder="Giá tối thiểu..."
              value={minPriceInput}
              onChange={(e) => setMinPriceInput(formatNumberWithCommas(e.target.value))}
              className="h-10 w-28 sm:w-32 px-3.5 bg-zinc-50 border border-zinc-200/60 focus:bg-white focus:border-zinc-950 rounded-xl text-xs font-bold text-zinc-800 outline-none transition placeholder:text-zinc-400"
            />
            <span className="text-[10px] font-bold text-zinc-400">—</span>
            <input
              type="text"
              placeholder="Giá tối đa..."
              value={maxPriceInput}
              onChange={(e) => setMaxPriceInput(formatNumberWithCommas(e.target.value))}
              className="h-10 w-28 sm:w-32 px-3.5 bg-zinc-50 border border-zinc-200/60 focus:bg-white focus:border-zinc-950 rounded-xl text-xs font-bold text-zinc-800 outline-none transition placeholder:text-zinc-400"
            />
          </div>
        </div>

        {/* Clear Filters Button */}
        {(selectedCategory !== null || selectedBrand !== null || searchTerm.trim() !== '' || minPriceInput !== '' || maxPriceInput !== '') && (
          <button
            onClick={handleClearFilters}
            className="h-10 px-4 bg-zinc-100 hover:bg-zinc-200 text-zinc-600 rounded-xl text-xs font-extrabold flex items-center gap-1.5 active:scale-95 transition"
          >
            <FaTimes className="w-2.5 h-2.5" />
            <span>Xóa lọc</span>
          </button>
        )}
      </div>

      {priceError && (
        <div className="text-[11px] font-bold text-rose-600 animate-fade-in px-4">
          * {priceError}
        </div>
      )}
 
      {/* Elegant Products Grid */}
      {isLoading ? (
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-4">
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
      ) : error ? (
        <div className="empty-state mx-auto max-w-lg border-rose-200/60 bg-rose-50/50 p-8 rounded-3xl text-center">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-rose-100 text-rose-600 mb-4">
            <FaStore className="h-5 w-5" />
          </div>
          <p className="mb-2 text-lg font-extrabold text-rose-950">Đã xảy ra lỗi</p>
          <p className="mb-6 text-sm font-medium text-rose-600/80">{error}</p>
          <button onClick={() => setCurrentPage(0)} className="btn-danger inline-flex px-6">
            Thử lại
          </button>
        </div>
      ) : products.length === 0 ? (
        <div className="empty-state bg-white/40 border border-dashed border-zinc-200/60 p-16 text-center rounded-3xl flex flex-col items-center justify-center">
          <FaStore className="mb-4 h-12 w-12 text-zinc-300 animate-pulse" />
          <p className="text-sm font-extrabold text-zinc-700">Không tìm thấy sản phẩm nào.</p>
          <p className="text-xs font-semibold text-zinc-400 mt-1">Vui lòng thử lại với các tiêu chí lọc khác.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-4">
          {products.map((product, index) => (
            <div key={product.id} className={`animate-fade-in stagger-${(index % 6) + 1}`}>
              <ProductCard 
                product={product} 
                onOpenDetail={(prod) => setSelectedProduct(prod)}
              />
            </div>
          ))}
        </div>
      )}

      {/* Pagination Controls */}
      {!isLoading && totalPages > 1 && (
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

      {/* Premium Product Details Modal with Selection */}
      {selectedProduct && (
        <ProductDetailModal
          product={selectedProduct}
          onClose={() => setSelectedProduct(null)}
        />
      )}
    </div>
  );
};

export default ProductList;
