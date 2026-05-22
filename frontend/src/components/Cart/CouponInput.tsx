import { useState } from "react";
import { FaCheck, FaTag, FaTimes } from "react-icons/fa";
import { couponService } from "../../services/api/couponService";

interface CouponInputProps {
  onCouponApply: (couponCode: string | null) => void;
  onDiscountChange: (discount: number) => void;
  orderAmount: number;
}

export default function CouponInput({
  onCouponApply,
  onDiscountChange,
  orderAmount,
}: CouponInputProps) {
  const [couponCode, setCouponCode] = useState("");
  const [isValidating, setIsValidating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [appliedCoupon, setAppliedCoupon] = useState<string | null>(null);

  const handleValidateCoupon = async () => {
    if (!couponCode.trim()) {
      setError("Vui lòng nhập mã giảm giá");
      return;
    }

    setIsValidating(true);
    setError(null);
    setSuccess(false);

    try {
      const isValid = await couponService.validateCoupon(couponCode);

      if (!isValid) {
        setError("Mã giảm giá không hợp lệ hoặc đã hết hạn");
        onCouponApply(null);
        onDiscountChange(0);
        return;
      }

      const discount = await couponService.calculateDiscount(
        couponCode,
        orderAmount
      );

      if (discount > 0) {
        setSuccess(true);
        setAppliedCoupon(couponCode);
        onCouponApply(couponCode);
        onDiscountChange(discount);
        setError(null);
      } else {
        setError(
          "Đơn hàng không đủ điều kiện để áp dụng mã giảm giá này"
        );
        onCouponApply(null);
        onDiscountChange(0);
      }
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : "Lỗi khi kiểm tra mã giảm giá";
      setError(errorMessage);
      onCouponApply(null);
      onDiscountChange(0);
    } finally {
      setIsValidating(false);
    }
  };

  const handleRemoveCoupon = () => {
    setCouponCode("");
    setAppliedCoupon(null);
    setError(null);
    setSuccess(false);
    onCouponApply(null);
    onDiscountChange(0);
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") {
      handleValidateCoupon();
    }
  };

  return (
    <div className="surface p-6">
      <div className="mb-4 flex items-center gap-3">
        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-zinc-50 border border-zinc-200/50 text-zinc-700">
          <FaTag className="h-3.5 w-3.5 text-zinc-500" />
        </div>
        <h3 className="text-sm font-extrabold text-zinc-950">Mã giảm giá</h3>
      </div>

      {!appliedCoupon ? (
        <div className="flex flex-col gap-2 sm:flex-row">
          <input
            type="text"
            value={couponCode}
            onChange={(e) => {
              setCouponCode(e.target.value.toUpperCase());
              setError(null);
            }}
            onKeyDown={handleKeyPress}
            placeholder="NHẬP MÃ GIẢM GIÁ..."
            className="field flex-1 uppercase tracking-widest text-xs font-black"
            disabled={isValidating}
            data-testid="coupon-input"
          />
          <button
            onClick={handleValidateCoupon}
            disabled={isValidating || !couponCode.trim()}
            className="btn-primary h-11 rounded-xl px-5 text-xs font-bold uppercase tracking-wider"
            data-testid="apply-coupon-btn"
          >
            {isValidating ? "Đang kiểm tra" : "Áp dụng"}
          </button>
        </div>
      ) : (
        <div className="flex items-center justify-between rounded-xl border border-emerald-200/60 bg-emerald-50/50 p-4" data-testid="coupon-badge">
          <div className="flex items-center gap-2">
            <FaCheck className="h-3.5 w-3.5 text-emerald-600" />
            <span className="text-xs font-semibold text-emerald-800">
              Đã áp dụng mã: <strong className="font-extrabold tracking-widest text-emerald-950">{appliedCoupon}</strong>
            </span>
          </div>
          <button
            onClick={handleRemoveCoupon}
            className="icon-btn h-8 w-8 border-emerald-200/40 bg-white text-emerald-600 hover:bg-emerald-50 transition"
            data-testid="remove-coupon-btn"
            aria-label="Hủy mã giảm giá"
          >
            <FaTimes className="h-3 w-3" />
          </button>
        </div>
      )}

      {error && (
        <div className="mt-3 rounded-xl border border-rose-100 bg-rose-50/50 p-4 text-xs font-semibold text-rose-700" data-testid="coupon-error">
          {error}
        </div>
      )}

      {success && !error && (
        <div className="mt-3 rounded-xl border border-emerald-100 bg-emerald-50/40 p-4 text-xs font-semibold text-emerald-700" data-testid="coupon-success">
          Mã giảm giá đã được áp dụng thành công.
        </div>
      )}
    </div>
  );
}
