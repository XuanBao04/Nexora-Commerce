import { ReactElement, useEffect, lazy, Suspense } from "react";
import {
  BrowserRouter,
  Routes,
  Route,
  Navigate,
  useNavigate,
} from "react-router-dom";
import HeaderLayout from "@components/HeaderLayout";
import { useAuthStore } from "@/store/useAuthStore";
import { useCartStore } from "@/store/useCartStore";
import { ToastContainer, Slide, toast } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

// Lazy-loaded components for optimal initial paint & route splitting
const ProductList = lazy(() =>
  import("@features/products").then((m) => ({ default: m.ProductList }))
);
const Cart = lazy(() =>
  import("@features/cart").then((m) => ({ default: m.Cart }))
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

function RequireAuth({ children }: { children: ReactElement }) {
  const navigate = useNavigate();
  const { isAuthenticated, isLoading } = useAuthStore();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      toast.error("Vui lòng đăng nhập để tiếp tục.");
      navigate("/login", { replace: true });
    }
  }, [isAuthenticated, isLoading, navigate]);

  if (isLoading) return <PageLoadingFallback />;
  if (!isAuthenticated) return null;
  return children;
}

function RequireAdmin({ children }: { children: ReactElement }) {
  const navigate = useNavigate();
  const { user, isAuthenticated, isLoading } = useAuthStore();

  useEffect(() => {
    if (!isLoading) {
      if (!isAuthenticated) {
        toast.error("Vui lòng đăng nhập để tiếp tục.");
        navigate("/login", { replace: true });
      } else if (user?.role !== "ROLE_ADMIN") {
        toast.error("Bạn không có quyền truy cập trang quản trị.");
        navigate("/authenticated/products", { replace: true });
      }
    }
  }, [isAuthenticated, isLoading, user, navigate]);

  if (isLoading) return <PageLoadingFallback />;
  if (!isAuthenticated || user?.role !== "ROLE_ADMIN") return null;
  return children;
}

function App() {
  const { initializeAuth } = useAuthStore();
  const { initSessionAndCart } = useCartStore();

  useEffect(() => {
    const init = async () => {
      await initializeAuth();
      await initSessionAndCart();
    };
    init();
  }, [initializeAuth, initSessionAndCart]);

  return (
    <BrowserRouter>
          <Suspense fallback={<PageLoadingFallback />}>
            <Routes>
              <Route path="/" element={<Navigate to="/authenticated/products" />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route
                path="/admin/dashboard"
                element={
                  <RequireAdmin>
                    <AdminDashboard />
                  </RequireAdmin>
                }
              />
              <Route path="/authenticated" element={<HeaderLayout />}>
                <Route path="products" element={<ProductList />} />
                <Route
                  path="cart"
                  element={
                    <RequireAuth>
                      <Cart />
                    </RequireAuth>
                  }
                />
                <Route
                  path="orders"
                  element={
                    <RequireAuth>
                      <Order />
                    </RequireAuth>
                  }
                />
              </Route>
            </Routes>
          </Suspense>
          <ToastContainer
            position="top-right"
            autoClose={2000}
            limit={3}
            transition={Slide}
            hideProgressBar={true}
            newestOnTop={true}
            closeOnClick
            rtl={false}
            pauseOnFocusLoss={false}
            draggable
            pauseOnHover={false}
            theme="light"
            toastClassName="rounded-xl shadow-md border border-zinc-100 bg-white/95 backdrop-blur-md"
          />
    </BrowserRouter>
  );
}

export default App;
