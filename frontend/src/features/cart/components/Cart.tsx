import { useEffect, useState } from "react";
import { useCartStore } from "@/store/useCartStore";
import CartItem from "./CartItem";
import PriceBreakdown from "./PriceBreakdown";
import { FaArrowLeft, FaShoppingCart, FaCreditCard } from "react-icons/fa";
import { useNavigate } from "react-router-dom";

const SHIPPING_FEE = 29900;

type OrderPreview = {
  subtotal: number;
  discountAmount: number;
  shippingFee: number;
  totalPrice: number;
  couponCode: string | null;
};

const Cart = () => {
  const { cart, isLoading, error, fetchCart, removeItem, updateItem, clear } =
    useCartStore();

  const couponCode = null;
  const discountAmount = 0;
  const [orderPreview, setOrderPreview] = useState<OrderPreview | null>(
    null
  );
  const navigate = useNavigate();

  useEffect(() => {
    fetchCart();
  }, [fetchCart]);

  useEffect(() => {
    if (cart && cart.items.length > 0) {
      const subtotal = cart.items.reduce(
        (sum, item) => sum + item.price * item.quantity,
        0
      );

      setOrderPreview({
        subtotal,
        discountAmount,
        shippingFee: SHIPPING_FEE,
        totalPrice: subtotal - discountAmount + SHIPPING_FEE,
        couponCode: couponCode,
      });
    }
  }, [cart, discountAmount, couponCode]);

  const handleRedirectToCheckout = () => {
    navigate("/authenticated/checkout");
  };


  if (isLoading) {
    return (
      <div className="grid gap-6 lg:grid-cols-3 animate-fade-in">
        <div className="sr-only">Đang tải giỏ hàng...</div>
        <div className="space-y-4 lg:col-span-2">
          <div className="skeleton h-28 w-full" />
          <div className="skeleton h-28 w-full" />
          <div className="skeleton h-64 w-full" />
        </div>
        <div className="skeleton h-80 w-full" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="empty-state border-rose-200/60 bg-rose-50/50 p-8 text-center text-rose-700">
        <p className="font-extrabold">Lỗi hệ thống: {error}</p>
      </div>
    );
  }

  if (!cart || cart.items.length === 0) {
    return (
      <div className="empty-state bg-white/40 border border-dashed border-zinc-200 p-12 text-center" data-testid="empty-cart-message">
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-3xl bg-zinc-100 text-zinc-400 mb-6">
          <FaShoppingCart className="h-6 w-6" />
        </div>
        <h1 className="text-xl font-extrabold text-zinc-950">Giỏ hàng trống</h1>
        <p className="mt-2 max-w-sm text-sm font-semibold leading-relaxed text-zinc-400">
          Hãy thêm các sản phẩm tuyệt vời của Aetheris vào giỏ để bắt đầu thanh toán.
        </p>
        <a
          href="/authenticated/products"
          className="btn-primary mt-8 inline-flex px-6 h-12 rounded-xl text-xs uppercase tracking-wider font-bold"
        >
          Tiếp tục mua sắm
        </a>
      </div>
    );
  }

  return (
    <div className="animate-fade-in space-y-8 lg:space-y-10">
      <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
        <div className="space-y-2">
          <div className="inline-flex items-center gap-1.5 rounded-full border border-indigo-200/50 bg-indigo-50/60 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-indigo-700">
            Aetheris Cart
          </div>
          <h1 className="page-heading">Giỏ hàng của bạn</h1>
          <p className="page-subtitle">
            Kiểm tra danh mục sản phẩm, áp dụng ưu đãi và cung cấp địa chỉ giao hàng.
          </p>
        </div>
        <a href="/authenticated/products" className="btn-secondary h-11 px-5 text-xs font-bold uppercase tracking-wider">
          <FaArrowLeft className="h-3 w-3" />
          Quay lại mua sắm
        </a>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3 lg:items-start">
        <div className="space-y-6 lg:col-span-2">
          <section className="surface p-6 sm:p-8" data-testid="cart-items-container">
            <div className="mb-6 flex items-center justify-between border-b border-zinc-100 pb-4">
              <h2 className="text-lg font-extrabold text-zinc-950">
                Sản phẩm trong giỏ
              </h2>
              <span className="badge bg-zinc-100 text-zinc-600 border-zinc-200/40 text-[10px] font-black">
                {cart.totalItems} MẶT HÀNG
              </span>
            </div>
            <div className="divide-y divide-zinc-100">
              {cart.items.map((item) => (
                <CartItem
                  key={item.id}
                  item={item}
                  onRemove={() => removeItem(item.productId)}
                  onUpdateQuantity={(quantity) => updateItem(item.productId, quantity)}
                />
              ))}
            </div>
          </section>
        </div>

        <aside className="space-y-6 lg:sticky lg:top-24">


          {orderPreview && (
            <PriceBreakdown
              subtotal={orderPreview.subtotal}
              discountAmount={orderPreview.discountAmount}
              couponCode={orderPreview.couponCode ?? undefined}
              shippingFee={orderPreview.shippingFee}
              totalPrice={orderPreview.totalPrice}
            />
          )}

          <div className="surface p-6 space-y-4">
            <button
              className="btn-primary w-full h-12 rounded-xl text-xs font-bold uppercase tracking-widest"
              onClick={handleRedirectToCheckout}
              data-testid="checkout-btn"
            >
              <FaCreditCard className="h-3 w-3 text-amber-200" />
              Tiến hành thanh toán
            </button>
            <button
              onClick={clear}
              className="btn-secondary w-full h-11 rounded-xl text-xs font-bold uppercase tracking-wider text-rose-600 hover:border-rose-200 hover:bg-rose-50"
              data-testid="clear-cart-btn"
            >
              Xóa giỏ hàng
            </button>
            <p className="text-center text-[10px] font-bold leading-relaxed text-zinc-400 uppercase tracking-wider">
              Dữ liệu được bảo mật an toàn 100%
            </p>
          </div>
        </aside>
      </div>
    </div>
  );
};

export default Cart;
