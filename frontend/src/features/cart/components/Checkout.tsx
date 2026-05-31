import { useEffect, useState } from "react";
import { useCartStore } from "@/store/useCartStore";
import CouponInput from "./CouponInput";
import PriceBreakdown from "./PriceBreakdown";
import AddressForm from "./AddressForm";
import { orderService } from "@/features/orders/services/orderService";
import { inventoryService } from "@/features/inventory/services/inventoryService";
import { Navigate, useNavigate } from "react-router-dom";
import { OrderItemRequest, OrderRequest, ShippingAddress } from "@/features/orders/types/order";
import { productService } from "@/features/products/services/productService";
import { CartItemResponse } from "@/features/cart/types/cart";
import { Product } from "@/features/products/types/product";
import { toast } from "react-toastify";
import { FaArrowLeft, FaLock, FaMoneyBillWave, FaCreditCard } from "react-icons/fa";

const SHIPPING_FEE = 29900;

type OrderPreview = {
  subtotal: number;
  discountAmount: number;
  shippingFee: number;
  totalPrice: number;
  couponCode: string | null;
};

const getVariantName = (product: Product, variantSku: string): string => {
  const variant = product.variants?.find((item) => item.sku === variantSku);
  if (!variant?.attributes?.length) {
    return "Default";
  }

  return variant.attributes
    .map((attribute) => `${attribute.name}: ${attribute.value}`)
    .join(", ");
};

const buildOrderItemRequest = async (
  item: CartItemResponse,
): Promise<OrderItemRequest> => {
  try {
    const product = await productService.getProductById(item.productId);

    return {
      variantSku: item.productId,
      productName: product.name || item.productId,
      variantName: getVariantName(product, item.productId),
      quantity: item.quantity,
      price: item.price,
    };
  } catch {
    return {
      variantSku: item.productId,
      productName: item.productId,
      variantName: "Default",
      quantity: item.quantity,
      price: item.price,
    };
  }
};

const Checkout = () => {
  const userId = localStorage.getItem("userId") || "";
  const navigate = useNavigate();
  const { cart, isLoading, error, fetchCart, clear } = useCartStore();

  const [couponCode, setCouponCode] = useState<string | null>(null);
  const [discountAmount, setDiscountAmount] = useState(0);
  const [orderPreview, setOrderPreview] = useState<OrderPreview | null>(null);
  const [shippingAddress, setShippingAddress] = useState<ShippingAddress>({
    shippingAddress: "",
    city: "",
    district: "",
    ward: "",
    postalCode: "",
    phoneNumber: "",
  });
  const [addressErrors, setAddressErrors] = useState<Partial<ShippingAddress>>({});
  const [paymentMethod, setPaymentMethod] = useState<'COD' | 'VNPAY'>('COD');

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

  const handlePlaceOrder = async () => {
    try {
      if (!cart || cart.items.length === 0) {
        toast.error("Giỏ hàng trống. Vui lòng thêm sản phẩm vào giỏ hàng.");
        return navigate("/authenticated/products");
      }

      const errors: Partial<ShippingAddress> = {};
      
      const trimmedShippingAddress = shippingAddress.shippingAddress.trim();
      if (!trimmedShippingAddress) {
        errors.shippingAddress = "Vui lòng nhập địa chỉ giao hàng";
      } else if (trimmedShippingAddress.length < 5 || trimmedShippingAddress.length > 255) {
        errors.shippingAddress = "Địa chỉ giao hàng phải từ 5 đến 255 ký tự";
      }
      
      const trimmedCity = shippingAddress.city.trim();
      if (!trimmedCity) {
        errors.city = "Vui lòng nhập Tỉnh / Thành phố";
      } else if (trimmedCity.length < 2 || trimmedCity.length > 100) {
        errors.city = "Tỉnh / Thành phố phải từ 2 đến 100 ký tự";
      }
      
      const trimmedDistrict = shippingAddress.district.trim();
      if (!trimmedDistrict) {
        errors.district = "Vui lòng nhập Quận / Huyện";
      } else if (trimmedDistrict.length < 2 || trimmedDistrict.length > 100) {
        errors.district = "Quận / Huyện phải từ 2 đến 100 ký tự";
      }
      
      const trimmedWard = shippingAddress.ward.trim();
      if (!trimmedWard) {
        errors.ward = "Vui lòng nhập Phường / Xã";
      } else if (trimmedWard.length < 2 || trimmedWard.length > 100) {
        errors.ward = "Phường / Xã phải từ 2 đến 100 ký tự";
      }
      
      const trimmedPhone = shippingAddress.phoneNumber.trim();
      if (!trimmedPhone) {
        errors.phoneNumber = "Vui lòng nhập số điện thoại";
      } else if (!/^0\d{9}$/.test(trimmedPhone)) {
        errors.phoneNumber = "Số điện thoại không hợp lệ (phải gồm 10 chữ số và bắt đầu bằng số 0)";
      }

      if (Object.keys(errors).length > 0) {
        setAddressErrors(errors);
        toast.error("Vui lòng điền đầy đủ thông tin giao hàng.");
        return;
      }

      const orderItems = await Promise.all(
        cart.items.map((item) => buildOrderItemRequest(item)),
      );

      
      const combinedAddress = [
        shippingAddress.shippingAddress.trim(),
        shippingAddress.ward.trim() ? `Phường ${shippingAddress.ward.trim()}` : "",
        shippingAddress.district.trim() ? `Quận ${shippingAddress.district.trim()}` : "",
        shippingAddress.city.trim() ? `${shippingAddress.city.trim()}` : ""
      ].filter(Boolean).join(", ");

      const orderRequest: OrderRequest = {
        userId,
        orderItems,
        couponCode: couponCode ?? undefined,
        paymentMethod,
        ...shippingAddress,
        shippingAddress: combinedAddress,
      };

      const stockChecks = await Promise.all(
        cart.items.map((item) =>
          inventoryService.checkStock(item.productId, item.quantity),
        ),
      );

      if (stockChecks.some((isAvailable) => !isAvailable)) {
        toast.error(
          "Một hoặc nhiều sản phẩm không còn đủ tồn kho. Vui lòng cập nhật giỏ hàng.",
        );
        return;
      }

      const response = await orderService.checkout(orderRequest);
      await clear();
      
      if (paymentMethod === 'VNPAY') {
        if (response.paymentUrl) {
          window.location.href = response.paymentUrl;
        } else {
          toast.error("Không thể tạo đường dẫn thanh toán.");
          navigate("/authenticated/orders");
        }
      } else {
        // COD: Show success message
        toast.success("Đặt hàng thành công! Vui lòng chờ xác nhận.");
        navigate("/authenticated/orders");
      }
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : "Không xác định được lỗi.";
      toast.error(`Đã xảy ra lỗi khi tạo đơn hàng: ${message}`);
    }
  };

  if (isLoading) {
    return (
      <div className="grid gap-6 lg:grid-cols-3 animate-fade-in">
        <div className="sr-only">Đang tải giỏ hàng...</div>
        <div className="space-y-4 lg:col-span-2">
          <div className="skeleton h-64 w-full" />
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
    return <Navigate to="/authenticated/cart" />;
  }

  return (
    <div className="animate-fade-in space-y-8 lg:space-y-10">
      <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
        <div className="space-y-2">
          <div className="inline-flex items-center gap-1.5 rounded-full border border-indigo-200/50 bg-indigo-50/60 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-indigo-700">
            Secure Checkout
          </div>
          <h1 className="page-heading">Thanh toán</h1>
          <p className="page-subtitle">
            Cung cấp địa chỉ giao hàng và chọn phương thức thanh toán.
          </p>
        </div>
        <button onClick={() => navigate('/authenticated/cart')} className="btn-secondary h-11 px-5 text-xs font-bold uppercase tracking-wider">
          <FaArrowLeft className="h-3 w-3" />
          Quay lại giỏ hàng
        </button>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3 lg:items-start">
        {/* Left Form details */}
        <div className="space-y-6 lg:col-span-2">
          <AddressForm
            onAddressChange={setShippingAddress}
            errors={addressErrors}
            setErrors={setAddressErrors}
          />

          <section className="surface p-6 sm:p-8">
            <h2 className="text-lg font-extrabold text-zinc-950 mb-6">Phương thức thanh toán</h2>
            <div className="space-y-4">
              <label className={`flex items-center justify-between p-4 rounded-xl border-2 cursor-pointer transition-all ${paymentMethod === 'COD' ? 'border-indigo-600 bg-indigo-50/30' : 'border-zinc-200 hover:border-indigo-300'}`}>
                <div className="flex items-center gap-4">
                  <div className={`flex h-10 w-10 items-center justify-center rounded-full ${paymentMethod === 'COD' ? 'bg-indigo-600 text-white' : 'bg-zinc-100 text-zinc-500'}`}>
                    <FaMoneyBillWave />
                  </div>
                  <div>
                    <h3 className="font-bold text-zinc-900">Thanh toán khi nhận hàng (COD)</h3>
                    <p className="text-xs text-zinc-500 font-medium">Thanh toán bằng tiền mặt khi đơn hàng được giao đến</p>
                  </div>
                </div>
                <input type="radio" name="paymentMethod" value="COD" checked={paymentMethod === 'COD'} onChange={() => setPaymentMethod('COD')} className="h-5 w-5 text-indigo-600 focus:ring-indigo-600 border-zinc-300" />
              </label>

              <label className={`flex items-center justify-between p-4 rounded-xl border-2 cursor-pointer transition-all ${paymentMethod === 'VNPAY' ? 'border-indigo-600 bg-indigo-50/30' : 'border-zinc-200 hover:border-indigo-300'}`}>
                <div className="flex items-center gap-4">
                  <div className={`flex h-10 w-10 items-center justify-center rounded-full ${paymentMethod === 'VNPAY' ? 'bg-indigo-600 text-white' : 'bg-zinc-100 text-zinc-500'}`}>
                    <FaCreditCard />
                  </div>
                  <div>
                    <h3 className="font-bold text-zinc-900">Thanh toán qua VNPAY</h3>
                    <p className="text-xs text-zinc-500 font-medium">Thanh toán an toàn qua cổng VNPAY bằng thẻ ATM/Visa</p>
                  </div>
                </div>
                <input type="radio" name="paymentMethod" value="VNPAY" checked={paymentMethod === 'VNPAY'} onChange={() => setPaymentMethod('VNPAY')} className="h-5 w-5 text-indigo-600 focus:ring-indigo-600 border-zinc-300" />
              </label>


            </div>
          </section>
        </div>

        {/* Right receipts column and Actions */}
        <aside className="space-y-6 lg:sticky lg:top-24">
          <CouponInput
            onCouponApply={setCouponCode}
            onDiscountChange={setDiscountAmount}
            orderAmount={
              cart?.items.reduce(
                (sum, item) => sum + item.price * item.quantity,
                0
              ) || 0
            }
          />

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
              onClick={handlePlaceOrder}
              data-testid="checkout-btn"
            >
              <FaLock className="h-3 w-3 text-amber-200" />
              Thanh toán ngay
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

export default Checkout;
