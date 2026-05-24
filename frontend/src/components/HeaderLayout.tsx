import { useState, useEffect } from "react";
import { Outlet, useNavigate, useLocation } from "react-router-dom";
import { FaShoppingCart, FaClipboardList, FaSignOutAlt, FaSignInAlt, FaStore } from "react-icons/fa";
import { useCartStore } from '@/store/useCartStore';
import { useAuthStore } from '@/store/useAuthStore';
import { toast } from "react-toastify";

export default function HeaderLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { cart } = useCartStore();
  const { user, isAuthenticated, logout } = useAuthStore();
  const [badgePop, setBadgePop] = useState(false);
  const [lastTotalItems, setLastTotalItems] = useState(cart?.totalItems || 0);

  useEffect(() => {
    const currentTotal = cart?.totalItems || 0;
    if (currentTotal !== lastTotalItems) {
      setLastTotalItems(currentTotal);
      setBadgePop(true);
      const t = setTimeout(() => setBadgePop(false), 350);
      return () => clearTimeout(t);
    }
  }, [cart?.totalItems, lastTotalItems]);

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
