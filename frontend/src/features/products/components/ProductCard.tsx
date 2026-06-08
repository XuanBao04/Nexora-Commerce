import { memo, useMemo } from "react";
import { FaBox, FaShoppingCart } from "react-icons/fa";
import { Product } from '../types/product';
import { formatPrice } from '@features/cart/utils/priceCalculation';

interface ProductCardProps {
  product: Product;
  onOpenDetail: (product: Product) => void;
}

const ProductCard = memo(({ product, onOpenDetail }: ProductCardProps) => {
  // Calculate total available stock across variants (with fallback to direct product.quantity)
  const totalStock = useMemo(() => {
    if (!product.variants || product.variants.length === 0) return product.quantity || 0;
    return product.variants.reduce((sum, v) => sum + ((v.quantity || 0) - (v.reservedQuantity || 0)), 0);
  }, [product.variants, product.quantity]);

  const isOutOfStock = (product.status && product.status !== "ACTIVE") || totalStock === 0;

  // Calculate price range across variants (with fallback to direct product.price)
  const priceRange = useMemo(() => {
    if (!product.variants || product.variants.length === 0) {
      return product.price ? formatPrice(product.price) : "---";
    }
    const prices = product.variants.map((v) => v.price);
    const minPrice = Math.min(...prices);
    const maxPrice = Math.max(...prices);

    if (minPrice === maxPrice) {
      return formatPrice(minPrice);
    }
    return `${formatPrice(minPrice)} - ${formatPrice(maxPrice)}`;
  }, [product.variants, product.price]);

  // Find primary image url from product images
  const displayImageUrl = useMemo(() => {
    const primaryImg = product.images?.find((img) => img.isPrimary);
    if (primaryImg) return primaryImg.imageUrl;
    if (product.images && product.images.length > 0) return product.images[0].imageUrl;
    return product.imageUrl || '';
  }, [product.images, product.imageUrl]);

  return (
    <div
      onClick={() => onOpenDetail(product)}
      className="surface group relative flex h-full flex-col overflow-hidden border border-zinc-200/50 bg-white/70 backdrop-blur-md transition-all duration-500 ease-premium hover:-translate-y-1 hover:border-zinc-300/80 hover:shadow-xl hover:shadow-zinc-200/30 rounded-2xl cursor-pointer"
      data-testid="product-card"
    >
      {/* Editorial Image container */}
      <div className="relative aspect-[4/3] w-full overflow-hidden bg-zinc-50 border-b border-zinc-100 flex items-center justify-center">
        {displayImageUrl ? (
          <img
            src={displayImageUrl}
            alt={product.name}
            className="h-full w-full object-cover transition-transform duration-700 ease-premium group-hover:scale-[1.06]"
            onError={(e) => {
              e.currentTarget.style.display = "none";
              const fallback = e.currentTarget.nextElementSibling as HTMLElement | null;
              if (fallback) fallback.style.display = "flex";
            }}
          />
        ) : null}

        {/* Fallback frame */}
        <div
          className={`absolute inset-0 flex flex-col items-center justify-center gap-2 bg-zinc-50 text-zinc-400 ${
            displayImageUrl ? "hidden" : "flex"
          }`}
        >
          <FaBox className="h-8 w-8 text-zinc-300 transition duration-300 group-hover:scale-105" />
          <span className="text-[9px] font-bold uppercase tracking-widest text-zinc-400">
            Sản phẩm #{product.id.substring(0, 4)}
          </span>
        </div>

        {/* Dynamic stock capsule tag */}
        <div className="absolute right-3 top-3 z-10">
          <span
            className={`px-2.5 py-1 text-[9px] font-extrabold uppercase tracking-wider rounded-full shadow-sm backdrop-blur-md border ${
              !isOutOfStock
                ? "bg-white/80 text-emerald-700 border-emerald-200/30"
                : "bg-white/80 text-rose-700 border-rose-200/30"
            }`}
          >
            {!isOutOfStock ? `Còn hàng: ${totalStock}` : "Hết hàng"}
          </span>
        </div>
      </div>

      {/* Product Card Details */}
      <div className="flex flex-1 flex-col p-6">
        <h2
          className="line-clamp-1 text-base font-extrabold text-zinc-950 transition-colors group-hover:text-zinc-700"
          data-testid="product-name"
          title={product.name}
        >
          {product.name}
        </h2>

        {/* Categories/Brand tags */}
        <div className="flex flex-wrap gap-1.5 mt-2">
          {product.brandName && (
            <span className="text-[8px] font-black uppercase tracking-wider text-zinc-400 bg-zinc-100/60 px-2 py-0.5 rounded border border-zinc-200/20">
              {product.brandName}
            </span>
          )}
          {product.categoryName && (
            <span className="text-[8px] font-black uppercase tracking-wider text-zinc-400 bg-zinc-100/60 px-2 py-0.5 rounded border border-zinc-200/20">
              {product.categoryName}
            </span>
          )}
        </div>

        <p className="mt-3 h-10 line-clamp-2 text-xs font-semibold leading-relaxed text-zinc-400 overflow-hidden">
          {product.description || "Không có mô tả chi tiết cho sản phẩm này."}
        </p>

        {/* Price tag summary */}
        <div className="mb-5 mt-5 flex items-center justify-start rounded-2xl bg-zinc-50 border border-zinc-100/50 p-4">
          <span
            className="text-lg font-black tracking-tight text-zinc-950"
            data-testid="product-price"
          >
            {priceRange}
          </span>
        </div>

        {/* Open Details Action */}
        <button
          className={`mt-auto flex h-11 w-full items-center justify-center gap-2 rounded-xl text-xs font-bold uppercase tracking-wider transition-all duration-300 ease-premium active:scale-[0.96] hover:shadow-lg hover:shadow-zinc-950/5 ${
            !isOutOfStock
              ? "bg-zinc-950 text-white shadow-md hover:bg-zinc-800"
              : "cursor-not-allowed bg-zinc-100 text-zinc-400"
          }`}
        >
          <FaShoppingCart className="h-3 w-3" />
          <span>{!isOutOfStock ? "Chọn mua" : "Hết hàng"}</span>
        </button>
      </div>
    </div>
  );
});

ProductCard.displayName = "ProductCard";

export default ProductCard;
