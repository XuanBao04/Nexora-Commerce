import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import InventoryManagement from '@features/inventory/components/InventoryManagement';
import OrderManagement from '@features/orders/components/OrderManagement';
import CouponManagement from '@features/coupon/components/CouponManagement';
import ProductManagement from '@features/products/components/ProductManagement';
import CategoryManagement from '@features/products/components/CategoryManagement';
import BrandManagement from '@features/products/components/BrandManagement';
import UserManagement from '@features/admin/components/UserManagement';
import { FaBox, FaClipboardList, FaSignOutAlt, FaStore, FaTag, FaFolder, FaStar, FaUsers } from "react-icons/fa";
import { useAuthStore } from "@/store/useAuthStore";

const AdminDashboard = () => {
  const navigate = useNavigate();
  const { role, username, logout } = useAuthStore();
  const [activeTab, setActiveTab] = useState<"products" | "inventory" | "orders" | "coupons" | "categories" | "brands" | "users">("products");

  useEffect(() => {
    if (role !== "ROLE_ADMIN" && role !== "ADMIN") {
      navigate("/login", { replace: true });
      return;
    }
  }, [role, navigate]);

  const handleLogout = async () => {
    await logout();
  };

  return (
    <div className="app-shell flex flex-col min-h-screen">
      {/* Admin Floating Glass header */}
      <header className="sticky top-0 z-40 border-b border-zinc-200/50 bg-white/75 shadow-sm backdrop-blur-md transition-all duration-300">
        <nav className="mx-auto flex h-16 max-w-7xl items-center justify-between gap-3 px-4 sm:px-6 lg:px-8">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-zinc-950 text-white shadow-md shadow-zinc-900/10 transition">
              <FaStore className="h-4 w-4 text-amber-200" />
            </div>
            <div>
              <h1 className="text-sm font-extrabold tracking-tight text-zinc-950 uppercase">
                Admin Console
              </h1>
              <p className="hidden text-[10px] font-bold text-zinc-400 uppercase tracking-widest sm:block">
                Operations & Management
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2 sm:gap-3">
            <span className="hidden rounded-xl bg-zinc-100/80 border border-zinc-200/40 px-3.5 py-2 text-xs font-bold text-zinc-600 sm:inline-block">
              {username || "Administrator"}
            </span>
            <button
              onClick={handleLogout}
              className="inline-flex h-10 items-center gap-2 rounded-xl bg-zinc-950 px-3.5 text-xs font-bold text-white transition-all duration-200 hover:bg-rose-600 hover:shadow-lg hover:shadow-rose-600/10 active:scale-95"
            >
              <FaSignOutAlt className="h-3 w-3" />
              <span className="hidden sm:inline">ĐĂNG XUẤT</span>
            </button>
          </div>
        </nav>
      </header>

      {/* Main Admin layout section */}
      <main className="page-wrap space-y-8 lg:space-y-10 animate-fade-in flex-1">
        <div className="space-y-2">
          <div className="inline-flex items-center gap-1.5 rounded-full border border-indigo-200/50 bg-indigo-50/60 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-indigo-700">
            Operations
          </div>
          <h2 className="page-heading">Trung tâm quản trị</h2>
          <p className="page-subtitle">
            Quản lý tồn kho hàng hóa, kiểm soát vận chuyển đơn đặt hàng và cấu hình các chiến dịch chiết khấu.
          </p>
        </div>

        {/* Tab switchers */}
        <div className="surface flex flex-wrap gap-1.5 p-2 bg-white/60">
          <button
            onClick={() => setActiveTab("products")}
            className={`inline-flex h-11 items-center gap-2 rounded-xl px-4 text-xs font-bold uppercase tracking-wider transition-all duration-200 active:scale-95 ${
              activeTab === "products"
                ? "bg-zinc-950 text-white shadow-md"
                : "text-zinc-600 hover:bg-zinc-100 hover:text-zinc-950"
            }`}
          >
            <FaStore className="h-3.5 w-3.5 text-amber-200" />
            Danh mục sản phẩm
          </button>

          <button
            onClick={() => setActiveTab("inventory")}
            className={`inline-flex h-11 items-center gap-2 rounded-xl px-4 text-xs font-bold uppercase tracking-wider transition-all duration-200 active:scale-95 ${
              activeTab === "inventory"
                ? "bg-zinc-950 text-white shadow-md"
                : "text-zinc-600 hover:bg-zinc-100 hover:text-zinc-950"
            }`}
          >
            <FaBox className="h-3.5 w-3.5" />
            Tồn kho sản phẩm
          </button>

          <button
            onClick={() => setActiveTab("orders")}
            className={`inline-flex h-11 items-center gap-2 rounded-xl px-4 text-xs font-bold uppercase tracking-wider transition-all duration-200 active:scale-95 ${
              activeTab === "orders"
                ? "bg-zinc-950 text-white shadow-md"
                : "text-zinc-600 hover:bg-zinc-100 hover:text-zinc-950"
            }`}
          >
            <FaClipboardList className="h-3.5 w-3.5" />
            Vận đơn & Đơn hàng
          </button>

          <button
            onClick={() => setActiveTab("coupons")}
            className={`inline-flex h-11 items-center gap-2 rounded-xl px-4 text-xs font-bold uppercase tracking-wider transition-all duration-200 active:scale-95 ${
              activeTab === "coupons"
                ? "bg-zinc-950 text-white shadow-md"
                : "text-zinc-600 hover:bg-zinc-100 hover:text-zinc-950"
            }`}
          >
            <FaTag className="h-3.5 w-3.5" />
            Quản lý mã giảm giá
          </button>
          <button
            onClick={() => setActiveTab("categories")}
            className={`inline-flex h-11 items-center gap-2 rounded-xl px-4 text-xs font-bold uppercase tracking-wider transition-all duration-200 active:scale-95 ${
              activeTab === "categories"
                ? "bg-zinc-950 text-white shadow-md"
                : "text-zinc-600 hover:bg-zinc-100 hover:text-zinc-950"
            }`}
          >
            <FaFolder className="h-3.5 w-3.5" />
            Danh mục
          </button>

          <button
            onClick={() => setActiveTab("brands")}
            className={`inline-flex h-11 items-center gap-2 rounded-xl px-4 text-xs font-bold uppercase tracking-wider transition-all duration-200 active:scale-95 ${
              activeTab === "brands"
                ? "bg-zinc-950 text-white shadow-md"
                : "text-zinc-600 hover:bg-zinc-100 hover:text-zinc-950"
            }`}
          >
            <FaStar className="h-3.5 w-3.5" />
            Thương hiệu
          </button>
          
          <button
            onClick={() => setActiveTab("users")}
            className={`inline-flex h-11 items-center gap-2 rounded-xl px-4 text-xs font-bold uppercase tracking-wider transition-all duration-200 active:scale-95 ${
              activeTab === "users"
                ? "bg-zinc-950 text-white shadow-md"
                : "text-zinc-600 hover:bg-zinc-100 hover:text-zinc-950"
            }`}
          >
            <FaUsers className="h-3.5 w-3.5" />
            Tài khoản
          </button>
        </div>

        {/* Active view renderer */}
        <div className="animate-fade-in">
          <div className={activeTab === "products" ? "block" : "hidden"}>
            <ProductManagement />
          </div>
          <div className={activeTab === "inventory" ? "block" : "hidden"}>
            <InventoryManagement />
          </div>
          <div className={activeTab === "orders" ? "block" : "hidden"}>
            <OrderManagement />
          </div>
          <div className={activeTab === "coupons" ? "block" : "hidden"}>
            <CouponManagement />
          </div>
          <div className={activeTab === "categories" ? "block" : "hidden"}>
            <CategoryManagement />
          </div>
          <div className={activeTab === "brands" ? "block" : "hidden"}>
            <BrandManagement />
          </div>
          <div className={activeTab === "users" ? "block" : "hidden"}>
            <UserManagement />
          </div>
        </div>
      </main>
    </div>
  );
};

export default AdminDashboard;
