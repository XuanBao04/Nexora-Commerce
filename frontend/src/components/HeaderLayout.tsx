import { useState, useEffect, useRef } from "react";
import { Outlet, useNavigate, useLocation } from "react-router-dom";
import { FaShoppingCart, FaClipboardList, FaSignOutAlt, FaSignInAlt, FaStore, FaSearch, FaSpinner, FaBox } from "react-icons/fa";
import { useCartStore } from '@/store/useCartStore';
import { useAuthStore } from '@/store/useAuthStore';
import { toast } from "react-toastify";
import { searchAiProducts } from "@/api/aiApi";
import { Product } from "@/features/products/types/product";
import { formatPrice } from '@features/cart/utils/priceCalculation';

export default function HeaderLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { cart } = useCartStore();
  const { user, isAuthenticated, logout, role } = useAuthStore();
  const [badgePop, setBadgePop] = useState(false);
  const [lastTotalItems, setLastTotalItems] = useState(cart?.totalItems || 0);

  const [searchQuery, setSearchQuery] = useState("");
  const [isSearching, setIsSearching] = useState(false);
  const [searchResults, setSearchResults] = useState<Product[]>([]);
  const [showResults, setShowResults] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const searchRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!searchQuery.trim()) {
      setSearchResults([]);
      setShowResults(false);
      setHasSearched(false);
      return;
    }
    const timer = setTimeout(async () => {
      setIsSearching(true);
      try {
        const results = await searchAiProducts(searchQuery, 5);
        setSearchResults(results);
        setShowResults(true);
        setHasSearched(true);
      } catch (err) {
        setSearchResults([]);
        setHasSearched(true);
      } finally {
        setIsSearching(false);
      }
    }, 500);
    return () => clearTimeout(timer);
  }, [searchQuery]);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (searchRef.current && !searchRef.current.contains(event.target as Node)) {
        setShowResults(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  useEffect(() => {
    const currentTotal = cart?.totalItems || 0;
    if (currentTotal !== lastTotalItems) {
      setLastTotalItems(currentTotal);
      setBadgePop(true);
      const t = setTimeout(() => setBadgePop(false), 350);
      return () => clearTimeout(t);
    }
  }, [cart?.totalItems, lastTotalItems]);

  useEffect(() => {
    if (isAuthenticated && role === "ROLE_ADMIN") {
      toast.error("Quản trị viên không được phép truy cập giao diện khách hàng.");
      navigate("/admin/dashboard", { replace: true });
    }
  }, [isAuthenticated, role, navigate]);

  const requireLogin = () => {
    if (!isAuthenticated) {
      toast.error("Vui lòng đăng nhập để sử dụng tính năng này.");
      navigate("/login");
      return false;
    }
    return true;
  };

  const handleLogout = async () => {
    await logout();
  };

  const handleProductClick = (product: Product) => {
    setShowResults(false);
    setSearchQuery("");
    navigate(`/authenticated/products?search=${encodeURIComponent(product.name)}`);
  };

  const handleSearchSubmit = () => {
    if (searchQuery.trim()) {
      setShowResults(false);
      navigate(`/authenticated/products?search=${encodeURIComponent(searchQuery.trim())}`);
    }
  };

  const getProductImage = (product: Product): string | null => {
    const primaryImg = product.images?.find((img) => img.isPrimary);
    if (primaryImg) return primaryImg.imageUrl;
    if (product.images && product.images.length > 0) return product.images[0].imageUrl;
    return product.imageUrl || null;
  };

  const isActive = (path: string) => location.pathname === path;

  return (
    <div className="app-shell flex flex-col min-h-screen">
      {/* Floating Glassmorphic Header */}
      <header className="sticky top-0 z-50 border-b border-zinc-200/50 bg-white/75 shadow-sm backdrop-blur-md transition-all duration-300">
        <nav className="mx-auto flex h-16 max-w-7xl items-center justify-between gap-3 px-4 sm:px-6 lg:px-8">
          {/* Elegant Luxury Logo */}
          <div
            className="group flex cursor-pointer items-center gap-3 active:scale-95 transition-all duration-200"
            onClick={() => navigate("/authenticated/products")}
          >
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-zinc-950 text-white shadow-md shadow-zinc-900/10 transition-all duration-300 group-hover:rotate-6 group-hover:scale-105">
              <FaStore className="h-4 w-4 text-amber-200" />
            </div>
            <span className="text-lg font-black tracking-tight text-zinc-950 sm:text-xl">
              Nexora <span className="text-[10px] font-bold text-zinc-400 uppercase tracking-widest block -mt-1">Aetheris</span>
            </span>
          </div>

          {/* AI Search Box */}
          <div ref={searchRef} className="relative mx-4 flex-1 max-w-md hidden md:block">
            <div className="relative flex items-center w-full h-10 rounded-xl bg-zinc-100/50 border border-zinc-200/50 px-3 focus-within:bg-white focus-within:ring-2 focus-within:ring-zinc-900/20 focus-within:border-zinc-400 transition-all shadow-inner">
              {isSearching ? (
                <FaSpinner className="animate-spin text-zinc-400 h-4 w-4 mr-2" />
              ) : (
                <FaSearch className="text-zinc-400 h-4 w-4 mr-2" />
              )}
              <input
                type="text"
                placeholder="Tìm kiếm thông minh (VD: áo khoác ấm mùa đông)..."
                className="w-full bg-transparent border-none outline-none text-sm font-medium placeholder-zinc-400 text-zinc-800"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onFocus={() => { if(searchResults.length > 0 || hasSearched) setShowResults(true); }}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    handleSearchSubmit();
                  }
                  if (e.key === 'Escape') {
                    setShowResults(false);
                  }
                }}
              />
            </div>
            
            {/* Search Results Dropdown */}
            {showResults && (
              <div className="absolute top-full mt-2 w-full bg-white/95 backdrop-blur-md border border-zinc-200/60 rounded-xl shadow-xl overflow-hidden z-50 animate-fade-in flex flex-col">
                <div className="px-3 py-2 text-[10px] font-bold text-zinc-400 uppercase tracking-widest bg-zinc-50 border-b border-zinc-100 flex items-center justify-between">
                  <span>AI Khuyên Dùng</span>
                  {isSearching && <FaSpinner className="animate-spin h-3 w-3 text-zinc-400" />}
                </div>

                {/* Loading skeleton */}
                {isSearching && searchResults.length === 0 && (
                  <div className="p-3 space-y-3">
                    {[1, 2, 3].map((i) => (
                      <div key={i} className="flex items-center gap-3 animate-pulse">
                        <div className="h-12 w-12 bg-zinc-200 rounded-lg flex-shrink-0" />
                        <div className="flex-1 space-y-2">
                          <div className="h-3 bg-zinc-200 rounded w-3/4" />
                          <div className="h-2.5 bg-zinc-100 rounded w-1/2" />
                        </div>
                      </div>
                    ))}
                  </div>
                )}

                {/* Results list */}
                {!isSearching && searchResults.length > 0 && searchResults.map((product) => {
                  const imgUrl = getProductImage(product);
                  return (
                    <div 
                      key={product.id} 
                      className="flex items-center gap-3 p-3 hover:bg-zinc-50 cursor-pointer transition-colors border-b border-zinc-100 last:border-0 group/item"
                      onMouseDown={() => handleProductClick(product)}
                    >
                      <div className="h-12 w-12 bg-zinc-100 rounded-lg flex-shrink-0 flex items-center justify-center overflow-hidden border border-zinc-200/40">
                        {imgUrl ? (
                          <img src={imgUrl} alt={product.name} className="h-full w-full object-cover" />
                        ) : (
                          <FaBox className="text-zinc-300 h-4 w-4" />
                        )}
                      </div>
                      <div className="flex flex-col min-w-0 flex-1">
                        <span className="text-sm font-semibold text-zinc-800 truncate group-hover/item:text-zinc-950">{product.name}</span>
                        <div className="flex items-center gap-2 mt-0.5">
                          <span className="text-xs text-amber-600 font-bold">
                            {product.price ? formatPrice(product.price) : '---'}
                          </span>
                          {product.brandName && (
                            <span className="text-[9px] font-bold text-zinc-400 bg-zinc-100 px-1.5 py-0.5 rounded">
                              {product.brandName}
                            </span>
                          )}
                          {product.categoryName && (
                            <span className="text-[9px] font-bold text-zinc-400 bg-zinc-100 px-1.5 py-0.5 rounded">
                              {product.categoryName}
                            </span>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}

                {/* Empty state */}
                {!isSearching && hasSearched && searchResults.length === 0 && (
                  <div className="px-4 py-6 text-center">
                    <FaSearch className="mx-auto h-6 w-6 text-zinc-300 mb-2" />
                    <p className="text-xs font-semibold text-zinc-500">Không tìm thấy sản phẩm nào.</p>
                    <p className="text-[10px] text-zinc-400 mt-0.5">Thử từ khóa khác hoặc nhấn Enter để tìm theo bộ lọc.</p>
                  </div>
                )}

                {/* Footer hint */}
                {searchResults.length > 0 && (
                  <div className="px-3 py-2 text-[10px] text-zinc-400 bg-zinc-50 border-t border-zinc-100 text-center">
                    Nhấn <kbd className="px-1 py-0.5 bg-zinc-200 rounded text-zinc-600 font-mono text-[9px]">Enter</kbd> để xem tất cả kết quả
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Navigation Actions */}
          <div className="flex min-w-0 items-center gap-1.5 sm:gap-2.5">
            <button
              onClick={() => navigate("/authenticated/products")}
              className={`relative hidden h-10 items-center gap-2 rounded-xl px-3.5 text-xs font-bold tracking-wide transition-all duration-300 ease-premium active:scale-95 sm:inline-flex ${
                isActive("/authenticated/products")
                  ? "bg-zinc-950 text-white shadow-sm"
                  : "text-zinc-600 hover:bg-zinc-100 hover:text-zinc-950"
              }`}
            >
              <FaStore className="h-3 w-3" />
              <span>SẢN PHẨM</span>
              {isActive("/authenticated/products") && (
                <span className="absolute bottom-1.5 left-1/2 h-0.5 w-3 -translate-x-1/2 rounded-full bg-white/40 animate-fade-in" />
              )}
            </button>

            <button
              onClick={() => {
                if (requireLogin()) {
                  navigate("/authenticated/cart");
                }
              }}
              className={`relative inline-flex h-10 items-center gap-2 rounded-xl px-3.5 text-xs font-bold tracking-wide transition-all duration-300 ease-premium active:scale-95 ${
                isActive("/authenticated/cart")
                  ? "bg-zinc-950 text-white shadow-sm"
                  : "text-zinc-600 hover:bg-zinc-100 hover:text-zinc-950"
              }`}
              data-testid="cart-icon"
            >
              <FaShoppingCart className="h-3 w-3" />
              <span className="hidden sm:inline">GIỎ HÀNG</span>
              {isActive("/authenticated/cart") && (
                <span className="absolute bottom-1.5 left-1/2 h-0.5 w-3 -translate-x-1/2 rounded-full bg-white/40 animate-fade-in" />
              )}
              {cart && cart.totalItems > 0 && (
                <span 
                  className={`absolute -right-1.5 -top-1 flex h-5 min-w-5 items-center justify-center rounded-full bg-zinc-950 px-1 text-[9px] font-black text-white ring-2 ring-white transition-all duration-300 ${
                    badgePop ? "animate-pop" : ""
                  }`} 
                  data-testid="cart-badge"
                >
                  {cart.totalItems}
                </span>
              )}
            </button>

            <button
              onClick={() => {
                if (requireLogin()) {
                  navigate("/authenticated/orders");
                }
              }}
              className={`relative inline-flex h-10 items-center gap-2 rounded-xl px-3.5 text-xs font-bold tracking-wide transition-all duration-300 ease-premium active:scale-95 ${
                isActive("/authenticated/orders")
                  ? "bg-zinc-950 text-white shadow-sm"
                  : "text-zinc-600 hover:bg-zinc-100 hover:text-zinc-950"
              }`}
            >
              <FaClipboardList className="h-3 w-3" />
              <span className="hidden sm:inline">ĐƠN HÀNG</span>
              {isActive("/authenticated/orders") && (
                <span className="absolute bottom-1.5 left-1/2 h-0.5 w-3 -translate-x-1/2 rounded-full bg-white/40 animate-fade-in" />
              )}
            </button>

            <div className="mx-1 hidden h-6 w-px bg-zinc-200 sm:block" />

            {isAuthenticated ? (
              <div className="flex items-center gap-2">
                <span className="hidden rounded-xl bg-zinc-100/80 border border-zinc-200/40 px-3.5 py-2 text-xs font-bold text-zinc-600 md:inline-block">
                  {user?.username || "User"}
                </span>
                <button
                  onClick={handleLogout}
                  className="inline-flex h-10 items-center gap-2 rounded-xl bg-zinc-950 px-3.5 text-xs font-bold text-white transition-all duration-200 hover:bg-rose-600 hover:shadow-lg hover:shadow-rose-600/10 active:scale-95"
                >
                  <FaSignOutAlt className="h-3 w-3" />
                  <span className="hidden sm:inline">ĐĂNG XUẤT</span>
                </button>
              </div>
            ) : (
              <button
                onClick={() => navigate("/login")}
                className="btn-primary h-10 px-4 text-xs tracking-wider"
              >
                <FaSignInAlt className="h-3 w-3" />
                <span>ĐĂNG NHẬP</span>
              </button>
            )}
          </div>
        </nav>
      </header>

      {/* Primary Layout Page Wrapper */}
      <main className="page-wrap flex-1 animate-fade-in">
        <Outlet />
      </main>

      {/* High-End Minimalist Footer */}
      <footer className="mt-auto border-t border-zinc-200/50 bg-white/40 backdrop-blur-sm py-8 transition-all duration-300">
        <div className="mx-auto max-w-7xl px-4 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs font-semibold text-zinc-400 sm:px-6 lg:px-8">
          <div>
            © {new Date().getFullYear()} Nexora Commerce. Design by Aetheris Concept. All rights reserved.
          </div>
          <div className="flex gap-4">
            <span className="hover:text-zinc-600 cursor-pointer transition">Điều khoản</span>
            <span className="hover:text-zinc-600 cursor-pointer transition">Bảo mật</span>
            <span className="hover:text-zinc-600 cursor-pointer transition">Liên hệ</span>
          </div>
        </div>
      </footer>
    </div>
  );
}
