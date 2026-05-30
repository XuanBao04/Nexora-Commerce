import { ReactElement, useEffect, lazy, Suspense } from "react";
import { Routes, Route, Navigate, useNavigate } from "react-router-dom";
import HeaderLayout from "@components/HeaderLayout";
import { useAuthStore } from "@/store/useAuthStore";
import { toast } from "react-toastify";
import { PATHS } from "./paths";

// Lazy-loaded components for optimal initial paint & route splitting
const ProductList = lazy(() =>
  import("@features/products").then((m) => ({ default: m.ProductList }))
);
const Cart = lazy(() =>
  import("@features/cart").then((m) => ({ default: m.Cart }))
);
const Checkout = lazy(() =>
  import("@features/cart").then((m) => ({ default: m.Checkout }))
);
const PaymentResult = lazy(() =>
  import("@features/cart").then((m) => ({ default: m.PaymentResult }))
);
const LoginPage = lazy(() =>
  import("@features/auth").then((m) => ({ default: m.LoginPage }))
);
const RegisterPage = lazy(() =>
  import("@features/auth").then((m) => ({ default: m.RegisterPage }))
);
const Order = lazy(() =>
  import("@features/orders").then((m) => ({ default: m.Order }))
);
const AdminDashboard = lazy(() =>
  import("@features/admin").then((m) => ({ default: m.AdminDashboard }))
);

// Premium full-screen loading fallback with glassmorphic blur-spinner
function PageLoadingFallback() {
  return (
    <div className="fixed inset-0 z-50 flex flex-col items-center justify-center bg-white/40 backdrop-blur-md">
      <div className="relative flex items-center justify-center">
        <div className="h-12 w-12 rounded-full border-2 border-zinc-950/10 border-t-zinc-950 animate-spin" />
      </div>
      <p className="mt-4 text-[10px] font-black uppercase tracking-[0.2em] text-zinc-500 animate-pulse">
        Aetheris is Loading
      </p>
    </div>
  );
}

// Authentication route guard for protected customer paths
function RequireAuth({ children }: { children: ReactElement }) {
  const navigate = useNavigate();
  const { isAuthenticated, isLoading } = useAuthStore();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      toast.error("Vui lòng đăng nhập để tiếp tục.");
      navigate(PATHS.LOGIN, { replace: true });
    }
  }, [isAuthenticated, isLoading, navigate]);

  if (isLoading) return <PageLoadingFallback />;
  if (!isAuthenticated) return null;
  return children;
}

// Administrative role route guard for admin-only paths
function RequireAdmin({ children }: { children: ReactElement }) {
  const navigate = useNavigate();
  const { user, isAuthenticated, isLoading } = useAuthStore();

  useEffect(() => {
    if (!isLoading) {
      if (!isAuthenticated) {
        toast.error("Vui lòng đăng nhập để tiếp tục.");
        navigate(PATHS.LOGIN, { replace: true });
      } else if (user?.role !== "ROLE_ADMIN") {
        toast.error("Bạn không có quyền truy cập trang quản trị.");
        navigate(PATHS.PRODUCTS, { replace: true });
      }
    }
  }, [isAuthenticated, isLoading, user, navigate]);

  if (isLoading) return <PageLoadingFallback />;
  if (!isAuthenticated || user?.role !== "ROLE_ADMIN") return null;
  return children;
}

// Redirect logic on the root "/" path based on user login state
function HomeRedirect() {
  const { isAuthenticated, isLoading } = useAuthStore();

  if (isLoading) return <PageLoadingFallback />;
  return <Navigate to={isAuthenticated ? PATHS.PRODUCTS : PATHS.LOGIN} replace />;
}

// Unified React Route Definition Tree
export default function AppRoutes() {
  return (
    <Suspense fallback={<PageLoadingFallback />}>
      <Routes>
        <Route path={PATHS.HOME} element={<HomeRedirect />} />
        <Route path={PATHS.LOGIN} element={<LoginPage />} />
        <Route path={PATHS.REGISTER} element={<RegisterPage />} />
        
        {/* Protected Administrative Routes */}
        <Route
          path={PATHS.ADMIN_DASHBOARD}
          element={
            <RequireAdmin>
              <AdminDashboard />
            </RequireAdmin>
          }
        />
        
        {/* Protected Customer Routes inside Main Layout */}
        <Route path={PATHS.AUTHENTICATED} element={<HeaderLayout />}>
          <Route path={PATHS.SEGMENTS.PRODUCTS} element={<ProductList />} />
          <Route
            path={PATHS.SEGMENTS.CART}
            element={
              <RequireAuth>
                <Cart />
              </RequireAuth>
            }
          />
          <Route
            path={PATHS.SEGMENTS.CHECKOUT}
            element={
              <RequireAuth>
                <Checkout />
              </RequireAuth>
            }
          />
          <Route
            path="checkout/result"
            element={
              <RequireAuth>
                <PaymentResult />
              </RequireAuth>
            }
          />
          <Route
            path={PATHS.SEGMENTS.ORDERS}
            element={
              <RequireAuth>
                <Order />
              </RequireAuth>
            }
          />
        </Route>

        {/* Catch-all route mapping back to Home */}
        <Route path="*" element={<Navigate to={PATHS.HOME} replace />} />
      </Routes>
    </Suspense>
  );
}
