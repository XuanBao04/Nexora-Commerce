import { formatPrice } from "../utils/priceCalculation";

interface PriceBreakdownProps {
  subtotal: number;
  discountAmount: number;
  couponCode?: string;
  shippingFee: number;
  totalPrice: number;
  compact?: boolean;
}

export default function PriceBreakdown({
  subtotal,
  discountAmount,
  couponCode,
  shippingFee,
  totalPrice,
  compact = false,
}: PriceBreakdownProps) {
  if (compact) {
    return (
      <div className="space-y-3.5 text-xs font-semibold text-zinc-500">
        <div className="flex justify-between gap-4">
          <span className="uppercase tracking-wider">Tổng giá trị sản phẩm</span>
          <span className="font-extrabold text-zinc-900" data-testid="subtotal-display">{formatPrice(subtotal)}</span>
        </div>

        {discountAmount > 0 && (
          <div className="flex justify-between gap-4 font-bold text-emerald-600 rounded-lg border border-emerald-100 bg-emerald-50/50 p-2.5">
            <span className="uppercase tracking-wider">Giảm giá {couponCode ? `(${couponCode})` : ""}</span>
            <span className="font-extrabold" data-testid="discount-display">-{formatPrice(discountAmount)}</span>
          </div>
        )}

        <div className="flex justify-between gap-4">
          <span className="uppercase tracking-wider">Phí vận chuyển</span>
          <span className="font-extrabold text-zinc-900" data-testid="shipping-fee-display">{formatPrice(shippingFee)}</span>
        </div>

        <div className="flex justify-between gap-4 border-t border-zinc-200/60 pt-3.5 font-bold text-zinc-950">
          <span className="uppercase tracking-widest text-zinc-900">Tổng cộng</span>
          <span className="text-base font-black text-zinc-950" data-testid="total-price-display">{formatPrice(totalPrice)}</span>
        </div>
      </div>
    );
  }

  return (
    <div className="surface p-6">
      <h3 className="mb-4 text-sm font-extrabold text-zinc-950 uppercase tracking-widest border-b border-zinc-100 pb-3">Chi tiết giá trị</h3>

      <div className="space-y-4">
        <div className="flex justify-between gap-4 text-xs font-semibold text-zinc-500">
          <span className="uppercase tracking-wider">Tổng giá trị sản phẩm</span>
          <span className="font-extrabold text-zinc-800" data-testid="subtotal-display">{formatPrice(subtotal)}</span>
        </div>

        {discountAmount > 0 && (
          <div className="flex justify-between gap-4 rounded-xl border border-emerald-200/40 bg-emerald-50/50 p-3 text-xs">
            <span className="font-semibold text-emerald-800 uppercase tracking-wider">
              Khuyến mãi {couponCode && `(${couponCode})`}
            </span>
            <span className="font-black text-emerald-600" data-testid="discount-display">
              -{formatPrice(discountAmount)}
            </span>
          </div>
        )}

        <div className="flex justify-between gap-4 text-xs font-semibold text-zinc-500">
          <span className="uppercase tracking-wider">Phí vận chuyển cố định</span>
          <span className="font-extrabold text-zinc-800" data-testid="shipping-fee-display">{formatPrice(shippingFee)}</span>
        </div>

        <div className="flex justify-between gap-4 border-t border-zinc-200/60 pt-4 items-center">
          <span className="text-xs font-black text-zinc-900 uppercase tracking-widest">Tổng thanh toán</span>
          <span className="text-lg font-black text-zinc-950" data-testid="total-price-display">
            {formatPrice(totalPrice)}
          </span>
        </div>
      </div>
    </div>
  );
}
