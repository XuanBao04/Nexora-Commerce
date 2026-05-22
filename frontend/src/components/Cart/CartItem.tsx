import { useEffect, useState } from "react";
import { FaMinus, FaPlus, FaRegTrashAlt } from "react-icons/fa";
import { CartItemResponse } from "../../types/cart";
import { productService } from "../../services/api/productService";
import { formatPrice } from "../../utils/priceCalculation";

interface CartItemProps {
  item: CartItemResponse;
  onRemove: () => void;
  onUpdateQuantity: (quantity: number) => void;
}

const CartItem = ({ item, onRemove, onUpdateQuantity }: CartItemProps) => {
  const [quantity, setQuantity] = useState(item.quantity);
  const [price, setPrice] = useState(0);
  const [productName, setProductName] = useState("");
  const [productImageUrl, setProductImageUrl] = useState<string | undefined>(
    undefined,
  );
  const [availableStock, setAvailableStock] = useState<number>(0);

  useEffect(() => {
    const fetchProduct = async () => {
      try {
        const product = await productService.getProductById(item.productId);
        setProductName(product.name);
        setPrice(product.price);
        setProductImageUrl(product.imageUrl);

        const stock = await productService.getAvailableStock(item.productId);
        setAvailableStock(stock);
      } catch (error) {
        console.error("Error fetching product:", error);
      }
    };

    fetchProduct();
  }, [item.productId]);

  const handleQuantityChange = (newQuantity: number) => {
    const maxQuantity = Math.max(1, availableStock);
    const validQuantity = Math.min(Math.max(1, newQuantity), maxQuantity);
    if (validQuantity !== quantity) {
      setQuantity(validQuantity);
      onUpdateQuantity(validQuantity);
    }
  };

  return (
    <div
      className="grid grid-cols-[72px_1fr] gap-4 border-b border-zinc-100 py-6 last:border-b-0 sm:grid-cols-[80px_1fr_auto_auto] items-center"
      data-testid="cart-item"
    >
      {/* High-End Image box */}
      <div className="flex h-[72px] w-[72px] items-center justify-center overflow-hidden rounded-xl border border-zinc-200/50 bg-zinc-50 sm:h-20 sm:w-20 transition hover:border-zinc-300">
        {productImageUrl ? (
          <img
            src={productImageUrl}
            alt={productName}
            className="h-full w-full object-cover"
            onError={(e) => {
              e.currentTarget.style.display = "none";
              const fallback = e.currentTarget
                .nextElementSibling as HTMLElement | null;
              if (fallback) fallback.style.display = "block";
            }}
          />
        ) : null}
        <span
          className={`px-2 text-center text-[9px] font-bold uppercase tracking-wider text-zinc-400 ${
            productImageUrl ? "hidden" : "block"
          }`}
        >
          No image
        </span>
      </div>

      {/* Details Area */}
      <div className="min-w-0">
        <h3 className="line-clamp-1 text-sm font-extrabold text-zinc-950">
          {productName || "Đang tải dữ liệu..."}
        </h3>
        <p className="mt-1 truncate text-[10px] font-bold text-zinc-400 uppercase tracking-widest">
          ID: {item.productId.substring(0, 8)}
        </p>
        <p className="mt-2 text-sm font-black text-zinc-950 sm:hidden">
          {formatPrice(price * quantity)}
        </p>
      </div>

      {/* Elegant Controls */}
      <div className="col-start-2 flex items-center bg-zinc-100 rounded-xl p-0.5 border border-zinc-200/50 sm:col-start-auto self-center justify-self-start sm:justify-self-center">
        <button
          onClick={() => handleQuantityChange(quantity - 1)}
          disabled={quantity <= 1}
          className="inline-flex h-9 w-8 items-center justify-center rounded-lg text-zinc-500 hover:bg-white active:scale-95 transition disabled:opacity-30"
          aria-label="Giảm số lượng"
        >
          <FaMinus className="h-2 w-2" />
        </button>
        <input
          type="number"
          min="1"
          max={availableStock}
          value={quantity}
          onChange={(e) => handleQuantityChange(parseInt(e.target.value) || 1)}
          className="w-10 bg-transparent text-center text-xs font-black text-zinc-800 outline-none"
          aria-label="Số lượng"
        />
        <button
          onClick={() => handleQuantityChange(quantity + 1)}
          disabled={availableStock > 0 && quantity >= availableStock}
          className="inline-flex h-9 w-8 items-center justify-center rounded-lg text-zinc-500 hover:bg-white active:scale-95 transition disabled:opacity-30"
          aria-label="+"
        >
          <FaPlus className="h-2 w-2" />
        </button>
      </div>

      {/* Price matrix actions */}
      <div className="col-start-2 flex items-center justify-between gap-6 sm:col-start-auto sm:block sm:w-36 sm:text-right">
        <div>
          <p className="hidden text-base font-black text-zinc-950 sm:block">
            {formatPrice(price * quantity)}
          </p>
          <p className="text-[10px] font-bold text-zinc-400 uppercase tracking-widest mt-0.5">
            {formatPrice(price)} x {quantity}
          </p>
        </div>
        <button
          onClick={onRemove}
          className="icon-btn h-9 w-9 text-rose-600 hover:border-rose-200 hover:bg-rose-50 hover:text-rose-700 sm:mt-2 transition-all"
          aria-label="Xóa"
        >
          <FaRegTrashAlt className="h-3.5 w-3.5" />
        </button>
      </div>
    </div>
  );
};

export default CartItem;
