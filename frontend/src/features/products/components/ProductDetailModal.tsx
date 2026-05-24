import React, { useState, useEffect, useMemo } from 'react';
import { Product, ProductVariant, ProductImage } from '../types/product';
import { formatPrice } from '@features/cart/utils/priceCalculation';
import { useCartStore } from '@/store/useCartStore';
import { toast } from 'react-toastify';
import { FaTimes, FaShoppingCart, FaMinus, FaPlus, FaBox } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';

interface ProductDetailModalProps {
  product: Product;
  onClose: () => void;
}

export const ProductDetailModal: React.FC<ProductDetailModalProps> = ({ product, onClose }) => {
  const { addItem } = useCartStore();
  const navigate = useNavigate();
  const [quantity, setQuantity] = useState(1);
  const [isAdding, setIsAdding] = useState(false);

  // Parse dynamic attribute groups from variants
  const attributeGroups = useMemo(() => {
    const groups: Record<string, string[]> = {};
    
    if (product.variants) {
      product.variants.forEach((v) => {
        if (v.attributes && Array.isArray(v.attributes)) {
          v.attributes.forEach((attr) => {
            if (!groups[attr.name]) {
              groups[attr.name] = [];
            }
            if (!groups[attr.name].includes(attr.value)) {
              groups[attr.name].push(attr.value);
            }
          });
        }
      });
    }
    return groups;
  }, [product.variants]);

  const hasAttributes = Object.keys(attributeGroups).length > 0;

  // Selected attributes state (e.g. { "Màu sắc": "Đen", "Dung lượng": "128GB" })
  const [selectedAttributes, setSelectedAttributes] = useState<Record<string, string>>({});

  // Auto-select first variant's attributes to make UX seamless
  useEffect(() => {
    if (hasAttributes && product.variants && product.variants.length > 0) {
      const firstVariant = product.variants[0];
      if (firstVariant.attributes) {
        const initialAttrs: Record<string, string> = {};
        firstVariant.attributes.forEach(attr => {
          initialAttrs[attr.name] = attr.value;
        });
        setSelectedAttributes(initialAttrs);
      }
    } else if (!hasAttributes && product.variants && product.variants.length > 0) {
      // Fallback for variants without structured attributes (select first SKU)
      setSelectedSku(product.variants[0].sku);
    }
  }, [product.variants, hasAttributes]);

  // Fallback SKU selection if there are no structured attributes
  const [selectedSku, setSelectedSku] = useState<string | null>(null);

  // Find currently selected variant based on selected attributes or fallback SKU
  const currentVariant = useMemo<ProductVariant | null>(() => {
    if (!product.variants || product.variants.length === 0) {
      return {
        sku: product.id,
        price: product.price || 0,
        quantity: product.quantity || 0,
      };
    }

    if (hasAttributes) {
      // Check if all attribute groups have a selection
      const allSelected = Object.keys(attributeGroups).every(group => selectedAttributes[group]);
      if (!allSelected) return null;

      // Find variant matching selected attributes
      return product.variants.find(v => {
        if (!v.attributes) return false;
        return Object.entries(selectedAttributes).every(([name, value]) => 
          v.attributes?.some(attr => attr.name === name && attr.value === value)
        );
      }) || null;
    } else {
      // Fallback SKU selection
      return product.variants.find(v => v.sku === selectedSku) || product.variants[0] || null;
    }
  }, [product.variants, product.id, product.price, product.quantity, selectedAttributes, selectedSku, attributeGroups, hasAttributes]);

  // Gallery primary image state
  const [activeImageUrl, setActiveImageUrl] = useState<string>('');

  // Primary image logic: updates when selected variant changes or on mount
  useEffect(() => {
    if (currentVariant) {
      // Find image matching the current variant SKU
      const variantImage = product.images?.find(img => img.sku === currentVariant.sku);
      if (variantImage) {
        setActiveImageUrl(variantImage.imageUrl);
        return;
      }
    }

    // Fallback to primary product image or first image
    const primaryImg = product.images?.find(img => img.isPrimary) || product.images?.[0];
    if (primaryImg) {
      setActiveImageUrl(primaryImg.imageUrl);
    } else if (product.imageUrl) {
      setActiveImageUrl(product.imageUrl);
    } else {
      setActiveImageUrl('');
    }
  }, [currentVariant, product.images, product.imageUrl]);

  // List of all images to render in thumbnails
  const allImages = useMemo<ProductImage[]>(() => {
    const list: ProductImage[] = [];
    if (product.images && product.images.length > 0) {
      return product.images;
    }
    if (product.imageUrl) {
      list.push({ id: 0, imageUrl: product.imageUrl, isPrimary: true, sku: null });
    }
    return list;
  }, [product.images, product.imageUrl]);

  // Stock status calculation
  const stockLimit = currentVariant ? currentVariant.quantity : 0;
  const isOutOfStock = stockLimit <= 0;
  const isFullySelected = !hasAttributes || Object.keys(attributeGroups).every(group => selectedAttributes[group]);

  const handleAttributeClick = (group: string, value: string) => {
    setSelectedAttributes(prev => ({
      ...prev,
      [group]: value
    }));
  };

  const handleAddToCart = async () => {
    const userId = localStorage.getItem("userId");
    if (!userId) {
      toast.info("Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng.");
      navigate("/login");
      return;
    }

    if (!isFullySelected || !currentVariant) {
      toast.warning("Vui lòng chọn đầy đủ các phân loại sản phẩm.");
      return;
    }

    setIsAdding(true);
    try {
      // Pass SKU as the productId to register the correct variant in cart
      await addItem({
        productId: currentVariant.sku,
        quantity: quantity,
      });
      toast.success(`Đã thêm ${quantity} sản phẩm vào giỏ hàng thành công!`);
      setQuantity(1);
    } catch (error: unknown) {
      toast.error((error as Error).message || "Không thể thêm vào giỏ hàng");
    } finally {
      setIsAdding(false);
    }
  };

  // Close on backdrop click
  const handleBackdropClick = (e: React.MouseEvent) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <div 
      className="fixed inset-0 z-50 flex items-center justify-center bg-zinc-950/60 backdrop-blur-md p-4 overflow-y-auto animate-fade-in"
      onClick={handleBackdropClick}
    >
      <div className="relative w-full max-w-4xl bg-white rounded-3xl shadow-2xl border border-zinc-200/80 overflow-hidden flex flex-col md:flex-row max-h-[90vh] md:max-h-[85vh] animate-scale-up">
        
        {/* Close Button */}
        <button 
          onClick={onClose}
          className="absolute top-4 right-4 z-10 w-9 h-9 flex items-center justify-center bg-zinc-100 hover:bg-zinc-200 text-zinc-500 hover:text-zinc-800 rounded-full transition shadow-sm"
          title="Đóng"
        >
          <FaTimes className="w-4 h-4" />
        </button>

        {/* Left Side: Images Gallery */}
        <div className="w-full md:w-1/2 p-6 md:p-8 bg-zinc-50 border-r border-zinc-100 flex flex-col justify-between overflow-y-auto">
          <div className="flex-1 flex items-center justify-center min-h-[250px] max-h-[350px] aspect-square rounded-2xl bg-white border border-zinc-100 overflow-hidden shadow-inner p-4">
            {activeImageUrl ? (
              <img 
                src={activeImageUrl} 
                alt={product.name} 
                className="max-w-full max-h-full object-contain transition-all duration-300"
              />
            ) : (
              <div className="flex flex-col items-center justify-center text-zinc-300 gap-2">
                <FaBox className="w-16 h-16 text-zinc-200" />
                <span className="text-xs font-bold text-zinc-400">Không có ảnh minh họa</span>
              </div>
            )}
          </div>

          {/* Thumbnails list */}
          {allImages.length > 1 && (
            <div className="flex gap-2.5 mt-4 overflow-x-auto pb-2 scrollbar-thin">
              {allImages.map((img, idx) => (
                <button
                  key={img.id || idx}
                  onClick={() => setActiveImageUrl(img.imageUrl)}
                  className={`w-14 h-14 rounded-xl border-2 flex-shrink-0 bg-white overflow-hidden transition ${
                    activeImageUrl === img.imageUrl 
                      ? 'border-zinc-950 scale-105 shadow-sm' 
                      : 'border-zinc-200/60 hover:border-zinc-400'
                  }`}
                >
                  <img src={img.imageUrl} alt="Thumbnail" className="w-full h-full object-cover" />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Right Side: Info & Attributes Selection */}
        <div className="w-full md:w-1/2 p-6 md:p-8 flex flex-col justify-between overflow-y-auto bg-white">
          <div className="space-y-5">
            {/* Header info */}
            <div>
              <div className="flex flex-wrap gap-2 mb-2">
                {product.brandName && (
                  <span className="px-2.5 py-0.5 rounded-md bg-zinc-100 text-[10px] font-black uppercase tracking-wider text-zinc-600 border border-zinc-200/50">
                    {product.brandName}
                  </span>
                )}
                {product.categoryName && (
                  <span className="px-2.5 py-0.5 rounded-md bg-zinc-100 text-[10px] font-black uppercase tracking-wider text-zinc-600 border border-zinc-200/50">
                    {product.categoryName}
                  </span>
                )}
              </div>
              <h2 className="text-xl font-extrabold text-zinc-950 leading-tight">
                {product.name}
              </h2>
              <p className="text-[10px] font-bold text-zinc-400 font-mono mt-1">MÃ SẢN PHẨM: {product.id}</p>
            </div>

            {/* Description - Cấm dangerouslySetInnerHTML để tránh XSS */}
            <div className="text-zinc-500 text-xs font-semibold leading-relaxed border-t border-b border-zinc-100 py-3">
              {product.description || "Sản phẩm này chưa có mô tả chi tiết từ quản trị viên."}
            </div>

            {/* Dynamic Attribute Selector Chips */}
            {hasAttributes ? (
              <div className="space-y-4">
                {Object.entries(attributeGroups).map(([groupName, values]) => (
                  <div key={groupName} className="space-y-2">
                    <span className="block text-[10px] font-black text-zinc-400 uppercase tracking-widest">
                      {groupName}
                    </span>
                    <div className="flex flex-wrap gap-2">
                      {values.map((val) => {
                        const isSelected = selectedAttributes[groupName] === val;
                        return (
                          <button
                            key={val}
                            onClick={() => handleAttributeClick(groupName, val)}
                            className={`h-9 px-4 rounded-xl text-xs font-bold transition-all duration-200 active:scale-95 ${
                              isSelected
                                ? 'bg-zinc-950 text-white shadow-sm border border-zinc-950'
                                : 'bg-zinc-50 hover:bg-zinc-100 text-zinc-700 border border-zinc-200/50'
                            }`}
                          >
                            {val}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                ))}
              </div>
            ) : product.variants && product.variants.length > 0 ? (
              /* Fallback Selector for raw list of SKUs */
              <div className="space-y-2">
                <span className="block text-[10px] font-black text-zinc-400 uppercase tracking-widest">
                  Chọn mã phân loại (SKU)
                </span>
                <div className="flex flex-wrap gap-2">
                  {product.variants.map((v) => {
                    const isSelected = selectedSku === v.sku;
                    return (
                      <button
                        key={v.sku}
                        onClick={() => setSelectedSku(v.sku)}
                        className={`h-9 px-4 rounded-xl text-xs font-bold transition-all duration-200 active:scale-95 ${
                          isSelected
                            ? 'bg-zinc-950 text-white shadow-sm border border-zinc-950'
                            : 'bg-zinc-50 hover:bg-zinc-100 text-zinc-700 border border-zinc-200/50'
                        }`}
                      >
                        {v.sku}
                      </button>
                    );
                  })}
                </div>
              </div>
            ) : null}

            {/* Price Tag & Stock Status */}
            <div className="bg-zinc-50 border border-zinc-100/80 rounded-2xl p-4 flex items-center justify-between">
              <div>
                <span className="block text-[9px] font-bold uppercase tracking-wider text-zinc-400">
                  Giá thành sản phẩm
                </span>
                <span className="text-xl font-black text-zinc-950 mt-1 block">
                  {currentVariant 
                    ? formatPrice(currentVariant.price) 
                    : "---"
                  }
                </span>
              </div>

              <div className="text-right">
                <span className="block text-[9px] font-bold uppercase tracking-wider text-zinc-400">
                  Tình trạng tồn kho
                </span>
                {isFullySelected ? (
                  <span className={`inline-block px-2.5 py-1 rounded-lg text-[9px] font-black uppercase tracking-widest mt-1.5 ${
                    !isOutOfStock 
                      ? 'bg-emerald-50 text-emerald-700 border border-emerald-200/40' 
                      : 'bg-rose-50 text-rose-700 border border-rose-200/40'
                  }`}>
                    {!isOutOfStock ? `Còn hàng (${stockLimit})` : "Hết hàng"}
                  </span>
                ) : (
                  <span className="inline-block px-2.5 py-1 rounded-lg text-[9px] font-black uppercase tracking-widest mt-1.5 bg-zinc-100 text-zinc-500 border border-zinc-200/30">
                    Chọn phân loại
                  </span>
                )}
              </div>
            </div>
          </div>

          {/* Add to Cart Actions Footer */}
          <div className="mt-8 pt-4 border-t border-zinc-100 flex items-center gap-3">
            {/* Quantity adjustment */}
            <div className={`flex items-center gap-1 bg-zinc-150 rounded-xl p-0.5 border border-zinc-200/60 ${isOutOfStock || !isFullySelected ? 'opacity-40 pointer-events-none' : ''}`}>
              <button
                onClick={() => setQuantity(prev => Math.max(1, prev - 1))}
                className="w-9 h-9 inline-flex items-center justify-center rounded-lg text-zinc-500 hover:bg-white hover:text-zinc-800 transition duration-150"
                disabled={quantity <= 1}
              >
                <FaMinus className="w-2.5 h-2.5" />
              </button>
              <input
                type="text"
                readOnly
                value={isOutOfStock ? 0 : quantity}
                className="w-10 text-center text-xs font-black text-zinc-800 bg-transparent outline-none"
              />
              <button
                onClick={() => setQuantity(prev => Math.min(stockLimit, prev + 1))}
                className="w-9 h-9 inline-flex items-center justify-center rounded-lg text-zinc-500 hover:bg-white hover:text-zinc-800 transition duration-150"
                disabled={quantity >= stockLimit}
              >
                <FaPlus className="w-2.5 h-2.5" />
              </button>
            </div>

            {/* Add to cart button */}
            <button
              onClick={handleAddToCart}
              disabled={isAdding || isOutOfStock || !isFullySelected}
              className={`flex-1 h-11 flex items-center justify-center gap-2 rounded-xl text-xs font-bold uppercase tracking-wider transition-all duration-300 active:scale-95 shadow-sm ${
                isFullySelected && !isOutOfStock
                  ? 'bg-zinc-950 text-white hover:bg-zinc-800'
                  : 'bg-zinc-100 text-zinc-400 cursor-not-allowed'
              }`}
            >
              {isAdding ? (
                <span className="w-4 h-4 rounded-full border-2 border-white/30 border-t-white animate-spin" />
              ) : (
                <>
                  <FaShoppingCart className="w-3.5 h-3.5" />
                  <span>
                    {!isFullySelected 
                      ? 'Chọn phân loại' 
                      : isOutOfStock 
                      ? 'Hết hàng' 
                      : 'Thêm vào giỏ'
                    }
                  </span>
                </>
              )}
            </button>
          </div>
        </div>

      </div>
    </div>
  );
};
