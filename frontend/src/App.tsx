import { ReactElement, useEffect, lazy, Suspense } from "react";
import {
  BrowserRouter,
  Routes,
  Route,
  Navigate,
  useNavigate,
} from "react-router-dom";
import HeaderLayout from "./components/HeaderLayout/HeaderLayout";
import ProductList from "./components/ProductList/ProductList";
import { CartProvider } from "./context/CartContext";
import { ToastContainer, Slide } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

// Lazy-loaded components for optimal initial paint & route splitting
const Cart = lazy(() => import("./components/Cart/Cart"));
const LoginPage = lazy(() => import("./pages/login/LoginPage"));
const RegisterPage = lazy(() => import("./pages/register/RegisterPage"));
const Order = lazy(() => import("./components/Order/Order"));
const AdminDashboard = lazy(() => import("./pages/admin/AdminDashboard"));

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
  const userId = localStorage.getItem("userId");

  useEffect(() => {
    if (!userId) {
      const shouldGoToLogin = confirm("Vui lòng đăng nhập để tiếp tục.");
      if (shouldGoToLogin) {
        navigate("/login", { replace: true });
      } else {
        navigate("/authenticated/products", { replace: true });
      }
    }
  }, [userId, navigate]);

  if (!userId) return null;
  return children;
}

function App() {
  return (
    <BrowserRouter>
      <CartProvider>
        <Suspense fallback={<PageLoadingFallback />}>
          <Routes>
            <Route path="/" element={<Navigate to="/authenticated/products" />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/admin/dashboard" element={<AdminDashboard />} />
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
      </CartProvider>
    </BrowserRouter>
  );
}

export default App;
