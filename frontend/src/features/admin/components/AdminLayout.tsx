import { useState, useRef, useEffect } from "react";
import {
  BarChart3,
  ShoppingCart,
  Ticket,
  Package,
  FolderTree,
  Award,
  Warehouse,
  Users,
  Settings,
  Search,
  Bell,
  LogOut,
  PanelLeftClose,
  PanelLeftOpen,
  Menu,
  X,
  ChevronDown,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { useAuthStore } from "@/store/useAuthStore";

// Lazy-loaded content panels
import AdminStatistics from "./AdminStatistics";
import ProductManagement from "@features/products/components/ProductManagement";
import CategoryManagement from "@features/products/components/CategoryManagement";
import BrandManagement from "@features/products/components/BrandManagement";
import InventoryManagement from "@features/inventory/components/InventoryManagement";
import OrderManagement from "@features/orders/components/OrderManagement";
import CouponManagement from "@features/coupon/components/CouponManagement";
import UserManagement from "@features/admin/components/UserManagement";

// ─── Type Definitions ─────────────────────────────────────────────

type TabId =
  | "statistics"
  | "orders"
  | "coupons"
  | "products"
  | "categories"
  | "brands"
  | "inventory"
  | "users"
  | "settings";

interface NavItem {
  id: TabId;
  label: string;
  icon: LucideIcon;
  group: string;
}

interface NavGroup {
  title: string;
  items: NavItem[];
}

// ─── Navigation Configuration ─────────────────────────────────────

const NAV_ITEMS: NavItem[] = [
  { id: "statistics", label: "Thống kê tổng quan", icon: BarChart3, group: "Tổng quan" },
  { id: "orders", label: "Đơn hàng", icon: ShoppingCart, group: "Quản lý bán hàng" },
  { id: "coupons", label: "Mã giảm giá", icon: Ticket, group: "Quản lý bán hàng" },
  { id: "products", label: "Sản phẩm", icon: Package, group: "Kho hàng" },
  { id: "categories", label: "Danh mục", icon: FolderTree, group: "Kho hàng" },
  { id: "brands", label: "Thương hiệu", icon: Award, group: "Kho hàng" },
  { id: "inventory", label: "Tồn kho", icon: Warehouse, group: "Kho hàng" },
  { id: "users", label: "Tài khoản", icon: Users, group: "Hệ thống" },
  { id: "settings", label: "Cài đặt", icon: Settings, group: "Hệ thống" },
];

function buildNavGroups(items: NavItem[]): NavGroup[] {
  const map = new Map<string, NavItem[]>();
  for (const item of items) {
    const group = map.get(item.group) ?? [];
    group.push(item);
    map.set(item.group, group);
  }
  return Array.from(map.entries()).map(([title, groupItems]) => ({
    title,
    items: groupItems,
  }));
}

const NAV_GROUPS: NavGroup[] = buildNavGroups(NAV_ITEMS);

// ─── Component ────────────────────────────────────────────────────

const AdminLayout = () => {
  const { username, logout } = useAuthStore();
  const [activeTab, setActiveTab] = useState<TabId>("statistics");
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const profileRef = useRef<HTMLDivElement>(null);

  // Close profile dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (profileRef.current && !profileRef.current.contains(e.target as Node)) {
        setProfileOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Close mobile sidebar on resize to desktop
  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth >= 1024) setMobileOpen(false);
    };
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  const handleNavClick = (id: TabId) => {
    setActiveTab(id);
    setMobileOpen(false);
  };

  const handleLogout = async () => {
    setProfileOpen(false);
    await logout();
  };

  const activeItem = NAV_ITEMS.find((n) => n.id === activeTab);
  const adminInitial = (username ?? "A").charAt(0).toUpperCase();

  // ── Sidebar Content (shared between desktop & mobile) ──────────

  const sidebarContent = (
    <>
      {/* Logo */}
      <div className="flex h-16 items-center gap-3 border-b border-white/[0.06] px-5">
        <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-indigo-500 shadow-lg shadow-indigo-500/20">
          <span className="text-sm font-black text-white">N</span>
        </div>
        {(!collapsed || mobileOpen) && (
          <div className="animate-fade-in overflow-hidden">
            <h1 className="text-sm font-extrabold tracking-tight text-white">Nexora</h1>
            <p className="text-[10px] font-semibold uppercase tracking-widest text-slate-400">
              Commerce
            </p>
          </div>
        )}
      </div>

      {/* Navigation Groups */}
      <nav className="flex-1 overflow-y-auto px-3 py-4 space-y-6">
        {NAV_GROUPS.map((group) => (
          <div key={group.title}>
            {(!collapsed || mobileOpen) && (
              <p className="mb-2 px-3 text-[10px] font-bold uppercase tracking-widest text-slate-500">
                {group.title}
              </p>
            )}
            {collapsed && !mobileOpen && (
              <div className="mb-2 mx-auto h-px w-6 rounded bg-slate-700/60" />
            )}
            <ul className="space-y-0.5">
              {group.items.map((item) => {
                const Icon = item.icon;
                const isActive = activeTab === item.id;
                return (
                  <li key={item.id}>
                    <button
                      onClick={() => handleNavClick(item.id)}
                      title={collapsed && !mobileOpen ? item.label : undefined}
                      className={`group/btn relative flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-[13px] font-semibold transition-all duration-150 ${
                        isActive
                          ? "bg-slate-800 text-white shadow-sm"
                          : "text-slate-400 hover:bg-slate-800/50 hover:text-slate-200"
                      } ${collapsed && !mobileOpen ? "justify-center" : ""}`}
                    >
                      {/* Active indicator bar */}
                      {isActive && (
                        <span className="absolute left-0 top-1/2 h-5 w-[3px] -translate-y-1/2 rounded-r-full bg-indigo-400" />
                      )}
                      <Icon
                        className={`shrink-0 transition-colors duration-150 ${
                          isActive ? "text-indigo-400" : "text-slate-500 group-hover/btn:text-slate-300"
                        }`}
                        size={18}
                        strokeWidth={isActive ? 2.2 : 1.8}
                      />
                      {(!collapsed || mobileOpen) && (
                        <span className="truncate">{item.label}</span>
                      )}
                    </button>
                  </li>
                );
              })}
            </ul>
          </div>
        ))}
      </nav>

      {/* Sidebar Footer — Collapse Toggle (desktop only) */}
      <div className="hidden lg:block border-t border-white/[0.06] p-3">
        <button
          onClick={() => setCollapsed((prev) => !prev)}
          className="flex w-full items-center justify-center gap-2 rounded-lg px-3 py-2.5 text-xs font-semibold text-slate-500 transition-colors hover:bg-slate-800/60 hover:text-slate-300"
        >
          {collapsed ? <PanelLeftOpen size={16} /> : <PanelLeftClose size={16} />}
          {!collapsed && <span>Thu gọn</span>}
        </button>
      </div>
    </>
  );

  // ── Render ─────────────────────────────────────────────────────

  return (
    <div className="flex h-screen overflow-hidden bg-slate-50">
      {/* ── Mobile Overlay ─────────────────────────────────────── */}
      {mobileOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/50 backdrop-blur-sm lg:hidden"
          onClick={() => setMobileOpen(false)}
        />
      )}

      {/* ── Mobile Sidebar ─────────────────────────────────────── */}
      <aside
        className={`fixed inset-y-0 left-0 z-50 flex w-[260px] flex-col bg-slate-900 shadow-2xl transition-transform duration-300 ease-in-out lg:hidden ${
          mobileOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        {/* Close button */}
        <button
          onClick={() => setMobileOpen(false)}
          className="absolute right-3 top-4 rounded-lg p-1.5 text-slate-500 hover:bg-slate-800 hover:text-white transition-colors"
        >
          <X size={18} />
        </button>
        {sidebarContent}
      </aside>

      {/* ── Desktop Sidebar ────────────────────────────────────── */}
      <aside
        className={`hidden lg:flex flex-col bg-slate-900 transition-all duration-300 ease-in-out ${
          collapsed ? "w-[72px]" : "w-[260px]"
        }`}
      >
        {sidebarContent}
      </aside>

      {/* ── Right Panel (Topbar + Content) ─────────────────────── */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* ── Topbar ─────────────────────────────────────────── */}
        <header className="sticky top-0 z-30 flex h-16 shrink-0 items-center justify-between gap-4 border-b border-slate-200/70 bg-white/80 px-4 backdrop-blur-md sm:px-6">
          {/* Left: Mobile hamburger + Breadcrumb */}
          <div className="flex items-center gap-3">
            <button
              onClick={() => setMobileOpen(true)}
              className="rounded-lg p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-700 transition-colors lg:hidden"
            >
              <Menu size={20} />
            </button>
            <div className="hidden sm:block">
              <p className="text-xs font-semibold text-slate-400">Trang quản trị</p>
              <h2 className="text-sm font-bold text-slate-900">
                {activeItem?.label ?? "Dashboard"}
              </h2>
            </div>
          </div>

          {/* Center-Right: Search */}
          <div className="hidden sm:flex flex-1 max-w-md mx-4">
            <div className="relative w-full">
              <Search
                className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                size={16}
              />
              <input
                type="text"
                placeholder="Tìm kiếm..."
                className="h-10 w-full rounded-xl border border-slate-200 bg-slate-50 pl-10 pr-4 text-sm font-medium text-slate-700 outline-none transition-all placeholder:text-slate-400 focus:border-indigo-300 focus:bg-white focus:ring-4 focus:ring-indigo-500/5"
              />
            </div>
          </div>

          {/* Right: Notifications + Profile */}
          <div className="flex items-center gap-2">
            {/* Notification Bell */}
            <button className="relative rounded-xl p-2.5 text-slate-500 hover:bg-slate-100 hover:text-slate-700 transition-colors">
              <Bell size={18} />
              {/* Red dot indicator */}
              <span className="absolute right-2 top-2 h-2 w-2 rounded-full bg-rose-500 ring-2 ring-white" />
            </button>

            {/* Profile Dropdown */}
            <div ref={profileRef} className="relative">
              <button
                onClick={() => setProfileOpen((prev) => !prev)}
                className="flex items-center gap-2 rounded-xl px-2 py-1.5 transition-colors hover:bg-slate-100"
              >
                {/* Avatar */}
                <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-gradient-to-br from-indigo-500 to-violet-600 text-xs font-black text-white shadow-sm">
                  {adminInitial}
                </div>
                <div className="hidden sm:block text-left">
                  <p className="text-xs font-bold text-slate-800 leading-none">
                    {username || "Administrator"}
                  </p>
                  <p className="text-[10px] font-medium text-slate-400">Quản trị viên</p>
                </div>
                <ChevronDown
                  size={14}
                  className={`hidden sm:block text-slate-400 transition-transform duration-200 ${
                    profileOpen ? "rotate-180" : ""
                  }`}
                />
              </button>

              {/* Dropdown Panel */}
              {profileOpen && (
                <div className="absolute right-0 top-full mt-2 w-56 animate-fade-in rounded-xl border border-slate-200/80 bg-white p-1.5 shadow-lg shadow-slate-200/50">
                  <div className="border-b border-slate-100 px-3 py-3">
                    <p className="text-sm font-bold text-slate-800">
                      {username || "Administrator"}
                    </p>
                    <p className="text-xs text-slate-400">Quản trị viên hệ thống</p>
                  </div>
                  <div className="pt-1.5">
                    <button
                      onClick={handleLogout}
                      className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2.5 text-xs font-semibold text-rose-600 transition-colors hover:bg-rose-50"
                    >
                      <LogOut size={15} />
                      Đăng xuất
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </header>

        {/* ── Main Scrollable Content ─────────────────────────── */}
        <main className="flex-1 overflow-y-auto p-4 sm:p-6">
          <div className="mx-auto max-w-7xl animate-fade-in">
            {activeTab === "statistics" && <AdminStatistics />}
            {activeTab === "orders" && <OrderManagement />}
            {activeTab === "coupons" && <CouponManagement />}
            {activeTab === "products" && <ProductManagement />}
            {activeTab === "categories" && <CategoryManagement />}
            {activeTab === "brands" && <BrandManagement />}
            {activeTab === "inventory" && <InventoryManagement />}
            {activeTab === "users" && <UserManagement />}
            {activeTab === "settings" && <SettingsPlaceholder />}
          </div>
        </main>
      </div>
    </div>
  );
};

// ─── Settings Placeholder ────────────────────────────────────────

function SettingsPlaceholder() {
  return (
    <div className="flex min-h-[400px] flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-white/50 p-12">
      <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-100 text-slate-400">
        <Settings size={24} />
      </div>
      <h3 className="mt-5 text-base font-bold text-slate-800">Cài đặt hệ thống</h3>
      <p className="mt-2 max-w-xs text-center text-sm text-slate-400">
        Tính năng cài đặt hệ thống đang được phát triển. Vui lòng quay lại sau.
      </p>
      <span className="mt-4 inline-flex items-center rounded-full border border-amber-200 bg-amber-50 px-3 py-1 text-[10px] font-bold uppercase tracking-widest text-amber-600">
        Đang phát triển
      </span>
    </div>
  );
}

export default AdminLayout;
