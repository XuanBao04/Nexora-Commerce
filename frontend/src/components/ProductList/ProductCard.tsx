import { useEffect, useState, memo } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { FaBox, FaShoppingCart, FaMinus, FaPlus } from "react-icons/fa";
import { Product } from "../../types/product";
import { useCart } from "../../hooks/useCart";
import { formatPrice } from "../../utils/priceCalculation";
import { productService } from "../../services/api/productService";

interface ProductCardProps {
  product: Product;
}

const ProductCard = memo(({ product }: ProductCardProps) => {
  const { cart, addItem } = useCart();
  const navigate = useNavigate();
  const [quantity, setQuantity] = useState(1);
  const [isAdding, setIsAdding] = useState(false);
  const [availableStock, setAvailableStock] = useState<number>(0);

  const quantityInCart =
    cart?.items.find((item) => item.productId === product.id)?.quantity || 0;
  const displayStock = Math.max(0, availableStock - quantityInCart);
  const isOutOfStock = product.status !== "ACTIVE" || displayStock === 0;

  useEffect(() => {
    const fetchStock = async () => {
      try {
        const stock = await productService.getAvailableStock(product.id);
        setAvailableStock(stock);
      } catch (error) {
        console.error("Error fetching stock:", error);
      }
    };

    if (product.status === "ACTIVE") {
      fetchStock();
    }
  }, [product.id, product.status]);

  const handleAddToCart = async () => {
    const userId = localStorage.getItem("userId");
    if (!userId) {
      const shouldGoToLogin = confirm(
        "Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng.",
      );
      if (shouldGoToLogin) {
        navigate("/login");
      }
      return;
    }

    setIsAdding(true);
    try {
      await addItem({
        productId: product.id,
        quantity,
      });
      setQuantity(1);
      toast.success("Đã thêm sản phẩm vào giỏ hàng!");
    } catch (error: unknown) {
      console.error("Error adding to cart:", error);
      const message =
        error instanceof Error
          ? error.message
          : "Có lỗi xảy ra khi thêm vào giỏ hàng.";
      toast.error(message);
    } finally {
      setIsAdding(false);
    }
  };

  return (
    <div
      className="surface group relative flex h-full flex-col overflow-hidden border border-zinc-200/50 bg-white/70 backdrop-blur-md transition-all duration-500 ease-premium hover:-translate-y-1 hover:border-zinc-300/80 hover:shadow-xl hover:shadow-zinc-200/30 rounded-2xl"
      data-testid="product-card"
    >
      {/* Editorial Image container */}
      <div className="relative aspect-[4/3] w-full overflow-hidden bg-zinc-50 border-b border-zinc-100 flex items-center justify-center">
        {product.imageUrl ? (
          <img
            src={product.imageUrl}
            alt={product.name}
            className="h-full w-full object-cover transition-transform duration-700 ease-premium group-hover:scale-[1.06]"
            onError={(e) => {
              e.currentTarget.style.display = "none";
              const fallback = e.currentTarget
                .nextElementSibling as HTMLElement | null;
              if (fallback) fallback.style.display = "flex";
            }}
          />
        ) : null}

        {/* Fallback frame */}
        <div
          className={`absolute inset-0 flex flex-col items-center justify-center gap-2 bg-zinc-50 text-zinc-400 ${
            product.imageUrl ? "hidden" : "flex"
          }`}
        >
          <FaBox className="h-8 w-8 text-zinc-300 transition duration-300 group-hover:scale-105" />
          <span className="text-[9px] font-bold uppercase tracking-widest text-zinc-400">
            Sản phẩm #{product.id.substring(0, 4)}
          </span>
        </div>

        {/* Dynamic stock capsule tag */}
        <div className="absolute right-4 top-4">
          <span
            className={`badge text-[9px] uppercase tracking-widest font-black shadow-sm ${
              !isOutOfStock
                ? "bg-emerald-50 text-emerald-700 border-emerald-200/40"
                : "bg-rose-50 text-rose-700 border-rose-200/40"
            }`}
          >
            {!isOutOfStock ? `Còn hàng (${displayStock})` : "Hết hàng"}
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

        <p className="mt-2 min-h-[40px] line-clamp-2 text-xs font-semibold leading-relaxed text-zinc-400">
          {product.description || "Không có mô tả chi tiết cho sản phẩm này."}
        </p>

        {/* Price tag summary */}
        <div className="mb-6 mt-5 flex items-center justify-between gap-3 rounded-2xl bg-zinc-50 border border-zinc-100/50 p-4">
          <span className="text-[9px] font-bold uppercase tracking-wider text-zinc-400">
            Đơn giá
          </span>
          <span
            className="text-lg font-black tracking-tight text-zinc-950"
            data-testid="product-price"
          >
            {formatPrice(product.price)}
          </span>
        </div>

        {/* Add to Cart Actions */}
        <div className="mt-auto flex items-center gap-2">
          <div className={`flex items-center gap-1 bg-zinc-100 rounded-xl p-0.5 border border-zinc-200/50 ${isOutOfStock ? "opacity-50 pointer-events-none" : ""}`}>
            <button
              onClick={() => setQuantity((prev) => Math.max(1, prev - 1))}
              className="inline-flex h-10 w-8 items-center justify-center rounded-lg text-zinc-500 hover:bg-white hover:text-zinc-800 active:scale-90 transition-all duration-200"
              aria-label="Giảm"
              disabled={isOutOfStock || quantity <= 1}
            >
              <FaMinus className="h-2 w-2" />
            </button>
            <input
              type="number"
              min="1"
              max={displayStock}
              value={isOutOfStock ? 0 : quantity}
              onChange={(e) => {
                const val = Math.max(1, parseInt(e.target.value) || 1);
                setQuantity(Math.min(val, displayStock));
              }}
              className="w-10 bg-transparent text-center text-xs font-black text-zinc-800 outline-none"
              disabled={isOutOfStock}
              data-testid="quantity-input"
            />
            <button
              onClick={() => setQuantity((prev) => Math.min(displayStock, prev + 1))}
              className="inline-flex h-10 w-8 items-center justify-center rounded-lg text-zinc-500 hover:bg-white hover:text-zinc-800 active:scale-90 transition-all duration-200"
              aria-label="Tăng"
              disabled={isOutOfStock || quantity >= displayStock}
            >
              <FaPlus className="h-2 w-2" />
            </button>
          </div>

          <button
            onClick={handleAddToCart}
            disabled={isAdding || isOutOfStock}
            className={`flex h-11 flex-1 items-center justify-center gap-2 rounded-xl text-xs font-bold uppercase tracking-wider transition-all duration-300 ease-premium active:scale-[0.96] hover:shadow-lg hover:shadow-zinc-950/5 ${
              !isOutOfStock
                ? "bg-zinc-950 text-white shadow-md hover:bg-zinc-800"
                : "cursor-not-allowed bg-zinc-100 text-zinc-400"
            }`}
            data-testid="add-to-cart-btn"
          >
            {isAdding ? (
              <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white" />
            ) : (
              <>
                <FaShoppingCart className="h-3 w-3" />
                <span>{!isOutOfStock ? "Thêm vào giỏ" : "Hết hàng"}</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
});

export default ProductCard;
