import { useEffect } from "react";
import { BrowserRouter } from "react-router-dom";
import { useAuthStore } from "@/store/useAuthStore";
import { useCartStore } from "@/store/useCartStore";
import AppRoutes from "@/routes/AppRoutes";
import { ToastContainer, Slide } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

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
      <AppRoutes />
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
