import React, { useEffect, useState, useRef, useMemo } from "react";
import { productService } from '../services/productService';
import { categoryService } from '../services/categoryService';
import { brandService } from '../services/brandService';
import { Product, CategoryResponse, BrandResponse } from '../types/product';
import { flattenCategoryTree } from '../utils/categoryHelper';
import { formatPrice } from '@features/cart/utils/priceCalculation';
import { 
  FaSync, FaEdit, FaTrash, FaPlus, FaSearch, FaBox, 
  FaCloudUploadAlt, FaExclamationTriangle, FaCheck, FaTimes, 
  FaRegFileImage, FaTags, FaMinus
} from "react-icons/fa";
import { toast } from "react-toastify";

interface VariantFormState {
  sku: string;
  price: string;
  quantity: string;
  attributes: { name: string; value: string }[];
}

interface ProductFormData {
  id: string;
  name: string;
  description: string;
  status: "ACTIVE" | "INACTIVE";
  categoryId: string;
  brandId: string;
}

const ProductManagement = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  // Search & Filter state
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<"ALL" | "ACTIVE" | "INACTIVE">("ALL");

  // Category and Brand metadata
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [brands, setBrands] = useState<BrandResponse[]>([]);

  const flatCategories = useMemo(() => flattenCategoryTree(categories), [categories]);

  // Form states
  const [showForm, setShowForm] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);
  const [selectedImageFile, setSelectedImageFile] = useState<File | null>(null);
  const [imagePreviewUrl, setImagePreviewUrl] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [formData, setFormData] = useState<ProductFormData>({
    id: "",
    name: "",
    description: "",
    status: "ACTIVE",
    categoryId: "",
    brandId: "",
  });

  // Dynamic Form Array for Variants
  const [variantsForm, setVariantsForm] = useState<VariantFormState[]>([
    { sku: "", price: "", quantity: "", attributes: [] }
  ]);

  const fetchMetadataAndProducts = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [productsData, cats, brs] = await Promise.all([
        productService.getAllProducts(),
        categoryService.getCategoryTree(),
        brandService.getAllBrands(),
      ]);
      setProducts(productsData);
      setCategories(cats);
      setBrands(brs);
    } catch (err) {
      setError((err as Error).message || "Không thể tải danh sách sản phẩm & bộ lọc");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchMetadataAndProducts();
  }, []);

  // Clean up Object URL to prevent memory leaks when preview URL changes
  useEffect(() => {
    return () => {
      if (imagePreviewUrl && imagePreviewUrl.startsWith("blob:")) {
        URL.revokeObjectURL(imagePreviewUrl);
      }
    };
  }, [imagePreviewUrl]);

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      // Safe MIME-type whitelist check
      const allowedTypes = ["image/png", "image/jpeg", "image/jpg", "image/webp"];
      if (!allowedTypes.includes(file.type)) {
        toast.error("Vui lòng chọn tệp hình ảnh hợp lệ (PNG, JPEG, JPG, WEBP)");
        return;
      }
      // Safe File Size check (limit <= 5MB)
      if (file.size > 5 * 1024 * 1024) {
        toast.error("Kích thước hình ảnh không được vượt quá 5MB để đảm bảo an toàn");
        return;
      }

      setSelectedImageFile(file);
      const preview = URL.createObjectURL(file);
      setImagePreviewUrl(preview);
    }
  };

  const handleOpenCreateForm = () => {
    setFormData({
      id: "",
      name: "",
      description: "",
      status: "ACTIVE",
      categoryId: "",
      brandId: "",
    });
    setVariantsForm([
      { sku: "", price: "", quantity: "", attributes: [] }
    ]);
    setSelectedImageFile(null);
    setImagePreviewUrl(null);
    if (fileInputRef.current) fileInputRef.current.value = "";
    setIsEditMode(false);
    setShowForm(true);
  };

  const handleOpenEditForm = async (product: Product) => {
    setIsLoading(true);
    try {
      // Fetch full details of the product including structured variants
      const fullProduct = await productService.getProductById(product.id);
      
      setFormData({
        id: fullProduct.id,
        name: fullProduct.name,
        description: fullProduct.description || "",
        status: fullProduct.status,
        categoryId: fullProduct.categoryId?.toString() || "",
        brandId: fullProduct.brandId?.toString() || "",
      });

      // Map existing variants to VariantFormState
      if (fullProduct.variants && fullProduct.variants.length > 0) {
        setVariantsForm(
          fullProduct.variants.map((v) => ({
            sku: v.sku,
            price: v.price.toString(),
            quantity: v.quantity.toString(),
            attributes: v.attributes || [],
          }))
        );
      } else {
        setVariantsForm([{ sku: "", price: "", quantity: "", attributes: [] }]);
      }

      setSelectedImageFile(null);
      // Retrieve display image URL if available
      const primaryImg = fullProduct.images?.find((img) => img.isPrimary) || fullProduct.images?.[0];
      setImagePreviewUrl(primaryImg?.imageUrl || fullProduct.imageUrl || null);
      
      if (fileInputRef.current) fileInputRef.current.value = "";
      setIsEditMode(true);
      setShowForm(true);

      // Scroll to form smoothly
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } catch {
      toast.error("Không thể tải thông tin chi tiết sản phẩm để chỉnh sửa");
    } finally {
      setIsLoading(false);
    }
  };

  const handleCloseForm = () => {
    setShowForm(false);
    setIsEditMode(false);
    setSelectedImageFile(null);
    setImagePreviewUrl(null);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  // Dynamic Form Array Actions
  const handleAddVariant = () => {
    setVariantsForm([
      ...variantsForm,
      { sku: "", price: "", quantity: "", attributes: [] }
    ]);
  };

  const handleRemoveVariant = (index: number) => {
    if (variantsForm.length === 1) {
      toast.warning("Sản phẩm phải có ít nhất 1 biến thể SKU");
      return;
    }
    setVariantsForm(variantsForm.filter((_, idx) => idx !== index));
  };

  const handleVariantChange = (index: number, field: keyof VariantFormState, value: string | { name: string; value: string }[]) => {
    const updated = [...variantsForm];
    updated[index] = {
      ...updated[index],
      [field]: value
    };
    setVariantsForm(updated);
  };

  // Nested attribute management within variants
  const handleAddAttribute = (vIndex: number) => {
    const updated = [...variantsForm];
    updated[vIndex].attributes = [
      ...updated[vIndex].attributes,
      { name: "", value: "" }
    ];
    setVariantsForm(updated);
  };

  const handleRemoveAttribute = (vIndex: number, attrIndex: number) => {
    const updated = [...variantsForm];
    updated[vIndex].attributes = updated[vIndex].attributes.filter((_, idx) => idx !== attrIndex);
    setVariantsForm(updated);
  };

  const handleAttributeChange = (vIndex: number, attrIndex: number, field: 'name' | 'value', val: string) => {
    const updated = [...variantsForm];
    updated[vIndex].attributes[attrIndex][field] = val;
    setVariantsForm(updated);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Core validations
    if (!formData.name.trim()) {
      toast.error("Tên sản phẩm không được để trống");
      return;
    }

    // Dynamic Variant Form Array validations
    for (let i = 0; i < variantsForm.length; i++) {
      const v = variantsForm[i];
      if (!v.sku.trim()) {
        toast.error(`Mã SKU của biến thể thứ ${i + 1} không được để trống`);
        return;
      }
      const priceNum = parseFloat(v.price);
      if (isNaN(priceNum) || priceNum < 0) {
        toast.error(`Đơn giá của biến thể "${v.sku}" phải lớn hơn hoặc bằng 0`);
        return;
      }
      const quantityNum = parseInt(v.quantity);
      if (isNaN(quantityNum) || quantityNum < 0) {
        toast.error(`Số kho của biến thể "${v.sku}" phải lớn hơn hoặc bằng 0`);
        return;
      }

      // Check key-value attributes inside variant
      for (let j = 0; j < v.attributes.length; j++) {
        const attr = v.attributes[j];
        if (!attr.name.trim() || !attr.value.trim()) {
          toast.error(`Thuộc tính phân loại của SKU "${v.sku}" không được bỏ trống tên/giá trị`);
          return;
        }
      }
    }

    setIsSubmitting(true);
    try {
      const uploadFormData = new FormData();
      if (isEditMode && formData.id) {
        uploadFormData.append("id", formData.id);
      }
      uploadFormData.append("name", formData.name.trim());
      uploadFormData.append("description", formData.description.trim());
      uploadFormData.append("status", formData.status);

      if (formData.categoryId) {
        uploadFormData.append("categoryId", formData.categoryId);
      }
      if (formData.brandId) {
        uploadFormData.append("brandId", formData.brandId);
      }

      // Structured variant-based list mapping directly matching Backend Multipart parameters
      const mappedVariants = variantsForm.map((v) => ({
        sku: v.sku.trim(),
        price: parseFloat(v.price),
        quantity: parseInt(v.quantity),
        attributes: v.attributes.map(a => ({ name: a.name.trim(), value: a.value.trim() }))
      }));

      // Append variants as structured JSON string (Standard practice for multi-part forms)
      uploadFormData.append("variants", JSON.stringify(mappedVariants));

      // Append image files
      if (selectedImageFile) {
        uploadFormData.append("image", selectedImageFile);
      }

      if (isEditMode) {
        await productService.updateProduct(formData.id, uploadFormData);
        toast.success("Cập nhật sản phẩm & các biến thể SKU thành công!");
      } else {
        await productService.createProduct(uploadFormData);
        toast.success("Thêm sản phẩm mới & các biến thể SKU thành công!");
      }

      handleCloseForm();
      fetchMetadataAndProducts();
    } catch (err) {
      toast.error(`Lỗi ${isEditMode ? "cập nhật" : "thêm"} sản phẩm: ` + (err as Error).message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteProduct = async (product: Product) => {
    // Exact permission role matching role is already guarded by the central controller
    // Direct safe confirmation without confirm dialog window bypasses, we ask clearly:
    if (window.confirm(`Bạn có chắc muốn xóa sản phẩm "${product.name}" (${product.id})? Hành động này không thể hoàn tác!`)) {
      setIsLoading(true);
      try {
        await productService.deleteProduct(product.id);
        toast.success("Xóa sản phẩm thành công!");
        fetchMetadataAndProducts();
      } catch (err) {
        toast.error("Lỗi xóa sản phẩm: " + (err as Error).message);
      } finally {
        setIsLoading(false);
      }
    }
  };



  // Local filtering & searching logic
  let filteredProducts = products;

  if (statusFilter !== "ALL") {
    filteredProducts = filteredProducts.filter((p) => p.status === statusFilter);
  }

  if (searchTerm.trim()) {
    const keyword = searchTerm.toLowerCase();
    filteredProducts = filteredProducts.filter(
      (p) =>
        p.name.toLowerCase().includes(keyword) ||
        p.id.toLowerCase().includes(keyword) ||
        (p.description && p.description.toLowerCase().includes(keyword)) ||
        (p.variants && p.variants.some(v => v.sku.toLowerCase().includes(keyword)))
    );
  }

  return (
    <div className="surface p-6 sm:p-8 bg-white/70 backdrop-blur-md border border-zinc-200/50 rounded-2xl space-y-6 sm:space-y-8">
      
      {/* Header Section */}
      <div className="border-b border-zinc-100 pb-5 flex flex-col sm:flex-row justify-between sm:items-center gap-4">
        <div>
          <h2 className="text-lg font-extrabold text-zinc-950">Quản trị Catalogue & Đa phân loại (Variants)</h2>
          <p className="text-xs font-semibold text-zinc-400 mt-1">
            Thiết lập danh mục sản phẩm, cấu hình nhiều biến thể SKU (Giá tiền, tồn kho riêng biệt), quản lý hình ảnh qua Cloudinary.
          </p>
        </div>
        {!showForm && (
          <button
            onClick={handleOpenCreateForm}
            className="self-start sm:self-center h-11 flex items-center justify-center gap-2 px-5 bg-zinc-950 text-white rounded-xl hover:bg-zinc-800 font-extrabold text-xs uppercase tracking-wider transition-all duration-300 active:scale-95 shadow-md shadow-zinc-900/10"
          >
            <FaPlus className="w-3 h-3 text-amber-200" />
            <span>Thêm sản phẩm mới</span>
          </button>
        )}
      </div>

      {error && (
        <div className="bg-rose-50/50 border border-rose-200/40 text-rose-700 px-4 py-3 rounded-xl text-xs font-semibold flex items-center gap-2">
          <FaExclamationTriangle className="flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Dynamic Multi-Variant Form */}
      {showForm && (
        <div className="bg-zinc-50/50 border border-zinc-200/40 p-6 rounded-2xl mb-8 animate-slide-down shadow-inner">
          <div className="flex justify-between items-center mb-5 border-b border-zinc-200/40 pb-3">
            <h3 className="text-xs font-black text-zinc-950 uppercase tracking-widest flex items-center gap-2">
              <FaBox className="text-zinc-500" />
              {isEditMode ? `Cập nhật sản phẩm: ${formData.id}` : "Thêm sản phẩm mới"}
            </h3>
            <button onClick={handleCloseForm} className="text-zinc-400 hover:text-zinc-600 transition" title="Đóng form">
              <FaTimes className="w-4 h-4" />
            </button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              
              {/* Product Info inputs */}
              <div className="lg:col-span-2 space-y-4">
                
                {/* Product Name */}
                <div>
                  <label className="block text-[10px] font-black text-zinc-400 uppercase tracking-widest mb-2">
                    Tên sản phẩm gốc
                  </label>
                  <input
                    type="text"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    placeholder="VD: Điện thoại Apple iPhone 15 Pro"
                    className="h-11 w-full rounded-xl border border-zinc-200 bg-white/95 px-4 text-xs font-semibold text-zinc-800 outline-none transition-all duration-200 focus:border-zinc-950 focus:bg-white"
                    required
                  />
                </div>

                {/* Categories & Brands dropdown selectors */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-[10px] font-black text-zinc-400 uppercase tracking-widest mb-2">
                      Danh mục phân cấp (Category)
                    </label>
                    <select
                      value={formData.categoryId}
                      onChange={(e) => setFormData({ ...formData, categoryId: e.target.value })}
                      className="h-11 w-full rounded-xl border border-zinc-200 bg-white/95 px-4 text-xs font-bold text-zinc-700 outline-none focus:border-zinc-950"
                    >
                      <option value="">Chọn danh mục...</option>
                      {flatCategories.map((cat) => (
                        <option key={cat.id} value={cat.id}>
                          {cat.displayName}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-[10px] font-black text-zinc-400 uppercase tracking-widest mb-2">
                      Thương hiệu (Brand)
                    </label>
                    <select
                      value={formData.brandId}
                      onChange={(e) => setFormData({ ...formData, brandId: e.target.value })}
                      className="h-11 w-full rounded-xl border border-zinc-200 bg-white/95 px-4 text-xs font-bold text-zinc-700 outline-none focus:border-zinc-950"
                    >
                      <option value="">Chọn thương hiệu...</option>
                      {brands.map(b => (
                        <option key={b.id} value={b.id}>{b.name}</option>
                      ))}
                    </select>
                  </div>
                </div>

                {/* Status & ID */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-[10px] font-black text-zinc-400 uppercase tracking-widest mb-2">
                      Trạng thái hiển thị
                    </label>
                    <select
                      value={formData.status}
                      onChange={(e) => setFormData({ ...formData, status: e.target.value as "ACTIVE" | "INACTIVE" })}
                      className="h-11 w-full rounded-xl border border-zinc-200 bg-white/95 px-4 text-xs font-bold text-zinc-700 outline-none focus:border-zinc-950"
                    >
                      <option value="ACTIVE">ACTIVE (Mở bán công khai)</option>
                      <option value="INACTIVE">INACTIVE (Tạm ẩn/Lưu kho)</option>
                    </select>
                  </div>

                  {isEditMode && (
                    <div>
                      <label className="block text-[10px] font-black text-zinc-400 uppercase tracking-widest mb-2">
                        Mã sản phẩm (Gốc)
                      </label>
                      <input
                        type="text"
                        value={formData.id}
                        disabled
                        className="h-11 w-full rounded-xl border border-zinc-200 bg-zinc-100 text-zinc-400 px-4 text-xs font-semibold outline-none cursor-not-allowed"
                      />
                    </div>
                  )}
                </div>

                {/* Description (Cấm dangerouslySetInnerHTML khi render) */}
                <div>
                  <label className="block text-[10px] font-black text-zinc-400 uppercase tracking-widest mb-2">
                    Mô tả chi tiết sản phẩm
                  </label>
                  <textarea
                    rows={4}
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    placeholder="Nhập thông số kỹ thuật, cấu hình chi tiết..."
                    className="w-full rounded-xl border border-zinc-200 bg-white/95 p-4 text-xs font-semibold text-zinc-800 outline-none transition-all duration-200 focus:border-zinc-950"
                  />
                </div>
              </div>

              {/* Cloudinary Image Upload & Safe check details */}
              <div className="flex flex-col">
                <span className="block text-[10px] font-black text-zinc-400 uppercase tracking-widest mb-2">
                  Ảnh đại diện sản phẩm (Max 5MB)
                </span>
                
                <div className="flex-1 flex flex-col items-center justify-center border border-dashed border-zinc-300 bg-white rounded-2xl p-4 min-h-[220px] transition duration-200 hover:border-zinc-500 relative group overflow-hidden">
                  {imagePreviewUrl ? (
                    <div className="w-full h-full flex flex-col justify-between items-center gap-3">
                      <div className="relative w-full aspect-square max-h-[160px] rounded-xl overflow-hidden bg-zinc-50 border border-zinc-150 flex items-center justify-center">
                        <img src={imagePreviewUrl} alt="Preview" className="w-full h-full object-contain" />
                        <button
                          type="button"
                          onClick={() => {
                            setSelectedImageFile(null);
                            setImagePreviewUrl(null);
                            if (fileInputRef.current) fileInputRef.current.value = "";
                          }}
                          className="absolute top-2 right-2 bg-rose-600/95 text-white rounded-full p-1.5 shadow-md hover:bg-rose-700 transition active:scale-90"
                          title="Xóa ảnh"
                        >
                          <FaTimes className="w-2.5 h-2.5" />
                        </button>
                      </div>
                      <span className="text-[10px] font-semibold text-zinc-400 truncate max-w-full text-center">
                        {selectedImageFile ? selectedImageFile.name : "Hình ảnh hiện tại của sản phẩm"}
                      </span>
                    </div>
                  ) : (
                    <div className="text-center flex flex-col items-center justify-center gap-3 py-6">
                      <div className="w-12 h-12 rounded-xl bg-zinc-50 flex items-center justify-center text-zinc-400 border border-zinc-200 group-hover:text-zinc-600 transition">
                        <FaCloudUploadAlt className="w-6 h-6" />
                      </div>
                      <div className="text-xs">
                        <span className="font-extrabold text-zinc-950 block">Chọn tệp ảnh tải lên</span>
                        <span className="text-[10px] font-semibold text-zinc-400 block mt-0.5">PNG, JPG, WEBP tối đa 5MB</span>
                      </div>
                    </div>
                  )}

                  <input
                    type="file"
                    ref={fileInputRef}
                    onChange={handleImageChange}
                    accept="image/png, image/jpeg, image/jpg, image/webp"
                    className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                  />
                </div>
              </div>
            </div>

            {/* Dynamic Variant Form Array Section (Quy tắc 3) */}
            <div className="border-t border-zinc-200/50 pt-5 space-y-4">
              <div className="flex justify-between items-center">
                <span className="block text-[10px] font-black text-zinc-400 uppercase tracking-widest flex items-center gap-1.5">
                  <FaTags className="text-zinc-500" />
                  Danh sách Biến thể SKU sản phẩm (Variants)
                </span>
                <button
                  type="button"
                  onClick={handleAddVariant}
                  className="h-8 px-3.5 bg-zinc-900 text-white rounded-lg hover:bg-zinc-800 text-[10px] font-extrabold flex items-center gap-1 transition active:scale-95 shadow-sm"
                >
                  <FaPlus className="w-2 h-2 text-amber-200" />
                  <span>Thêm biến thể mới</span>
                </button>
              </div>

              {/* Form Array lists */}
              <div className="space-y-4">
                {variantsForm.map((variant, vIdx) => (
                  <div key={vIdx} className="bg-white border border-zinc-200/80 rounded-2xl p-4 space-y-3.5 shadow-sm relative group">
                    <button
                      type="button"
                      onClick={() => handleRemoveVariant(vIdx)}
                      className="absolute top-4 right-4 text-zinc-400 hover:text-rose-600 transition w-7 h-7 bg-zinc-50 rounded-lg flex items-center justify-center border border-zinc-200/50"
                      title="Xóa biến thể này"
                    >
                      <FaMinus className="w-2.5 h-2.5" />
                    </button>

                    <span className="text-[10px] font-black text-indigo-600 uppercase tracking-wider block">
                      Biến thể #{vIdx + 1}
                    </span>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                      {/* SKU Input */}
                      <div>
                        <label className="block text-[9px] font-bold text-zinc-400 uppercase tracking-wider mb-1.5">
                          Mã định danh SKU (Duy nhất)
                        </label>
                        <input
                          type="text"
                          value={variant.sku}
                          onChange={(e) => handleVariantChange(vIdx, 'sku', e.target.value)}
                          placeholder="VD: IP15-PRO-BLK-128"
                          className="h-10 w-full rounded-lg border border-zinc-200 bg-white px-3 text-xs font-semibold text-zinc-800 outline-none focus:border-zinc-950"
                          required
                        />
                      </div>

                      {/* Variant Price */}
                      <div>
                        <label className="block text-[9px] font-bold text-zinc-400 uppercase tracking-wider mb-1.5">
                          Đơn giá SKU này (VND)
                        </label>
                        <input
                          type="number"
                          min="0"
                          value={variant.price}
                          onChange={(e) => handleVariantChange(vIdx, 'price', e.target.value)}
                          placeholder="VD: 25000000"
                          className="h-10 w-full rounded-lg border border-zinc-200 bg-white px-3 text-xs font-semibold text-zinc-800 outline-none focus:border-zinc-950"
                          required
                        />
                      </div>

                      {/* Variant Stock Quantity */}
                      <div>
                        <label className="block text-[9px] font-bold text-zinc-400 uppercase tracking-wider mb-1.5">
                          Số lượng tồn kho SKU
                        </label>
                        <input
                          type="number"
                          min="0"
                          value={variant.quantity}
                          onChange={(e) => handleVariantChange(vIdx, 'quantity', e.target.value)}
                          placeholder="VD: 25"
                          className="h-10 w-full rounded-lg border border-zinc-200 bg-white px-3 text-xs font-semibold text-zinc-800 outline-none focus:border-zinc-950"
                          required
                        />
                      </div>
                    </div>

                    {/* Nested Attributes Array (Chips configuration) */}
                    <div className="border-t border-dashed border-zinc-100 pt-3 space-y-2">
                      <div className="flex justify-between items-center">
                        <span className="block text-[9px] font-extrabold text-zinc-400 uppercase tracking-wider">
                          Thuộc tính cụ thể của SKU (ví dụ: Màu sắc: Đen, Dung lượng: 128GB)
                        </span>
                        <button
                          type="button"
                          onClick={() => handleAddAttribute(vIdx)}
                          className="text-[9px] font-black text-indigo-600 hover:text-indigo-800 flex items-center gap-1 transition"
                        >
                          <FaPlus className="w-1.5 h-1.5" />
                          <span>Thêm thuộc tính</span>
                        </button>
                      </div>

                      {variant.attributes && variant.attributes.length > 0 ? (
                        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                          {variant.attributes.map((attr, attrIdx) => (
                            <div key={attrIdx} className="flex gap-2 items-center bg-zinc-50 border border-zinc-200/50 p-2 rounded-xl">
                              <input
                                type="text"
                                placeholder="Tên: Màu sắc, Size"
                                value={attr.name}
                                onChange={(e) => handleAttributeChange(vIdx, attrIdx, 'name', e.target.value)}
                                className="h-8 flex-1 bg-white border border-zinc-200 rounded-lg px-2 text-[11px] font-bold text-zinc-700 outline-none"
                              />
                              <input
                                type="text"
                                placeholder="Giá trị: Đen, XL"
                                value={attr.value}
                                onChange={(e) => handleAttributeChange(vIdx, attrIdx, 'value', e.target.value)}
                                className="h-8 flex-1 bg-white border border-zinc-200 rounded-lg px-2 text-[11px] font-bold text-zinc-700 outline-none"
                              />
                              <button
                                type="button"
                                onClick={() => handleRemoveAttribute(vIdx, attrIdx)}
                                className="text-rose-500 hover:text-rose-700 text-xs px-1 hover:bg-zinc-100 rounded"
                                title="Xóa thuộc tính này"
                              >
                                <FaTimes className="w-2.5 h-2.5" />
                              </button>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <p className="text-[10px] text-zinc-400 italic">Chưa cấu hình thuộc tính Chips cho SKU này.</p>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Submission buttons */}
            <div className="flex gap-2.5 justify-end border-t border-zinc-200/40 pt-4">
              <button
                type="button"
                onClick={handleCloseForm}
                disabled={isSubmitting}
                className="h-11 px-5 bg-zinc-100 hover:bg-zinc-200/80 border border-zinc-200/40 text-zinc-600 rounded-xl font-extrabold text-xs transition active:scale-95 disabled:opacity-50"
              >
                Hủy bỏ
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                className="h-11 px-6 bg-zinc-950 hover:bg-zinc-800 text-white rounded-xl font-extrabold text-xs transition shadow-sm active:scale-95 disabled:bg-zinc-800 disabled:opacity-80 flex items-center justify-center gap-2"
              >
                {isSubmitting ? (
                  <>
                    <div className="w-3.5 h-3.5 border-2 border-white/20 border-t-white rounded-full animate-spin"></div>
                    <span>Đang xử lý...</span>
                  </>
                ) : (
                  <>
                    <FaCheck className="w-3 h-3 text-amber-200" />
                    <span>{isEditMode ? "Cập nhật sản phẩm" : "Lưu sản phẩm mới"}</span>
                  </>
                )}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Control filters bar */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="relative md:col-span-2">
          <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-zinc-400">
            <FaSearch className="w-3.5 h-3.5" />
          </div>
          <input
            type="text"
            placeholder="Tìm kiếm theo tên sản phẩm, mã SKU biến thể hoặc mô tả..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 h-11 bg-zinc-50 border border-zinc-200/60 focus:bg-white focus:border-zinc-950 focus:ring-4 focus:ring-zinc-900/5 rounded-xl text-xs font-semibold outline-none transition duration-200 text-zinc-700 placeholder:text-zinc-400"
          />
        </div>

        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as "ALL" | "ACTIVE" | "INACTIVE")}
          className="h-11 px-4 bg-zinc-50 border border-zinc-200/60 focus:bg-white focus:border-zinc-950 focus:ring-4 focus:ring-zinc-900/5 rounded-xl text-xs outline-none transition duration-200 text-zinc-500 font-extrabold uppercase tracking-wider"
        >
          <option value="ALL">Tất cả trạng thái</option>
          <option value="ACTIVE">Đang hoạt động</option>
          <option value="INACTIVE">Ngừng hoạt động</option>
        </select>

        <button
          onClick={fetchMetadataAndProducts}
          disabled={isLoading}
          className="h-11 flex items-center justify-center gap-2 px-5 bg-zinc-950 text-white rounded-xl hover:bg-zinc-800 disabled:bg-zinc-100 disabled:text-zinc-400 font-extrabold text-xs uppercase tracking-wider transition-all duration-300"
        >
          <FaSync className={`w-3 h-3 ${isLoading ? "animate-spin" : ""}`} />
          <span>Làm mới</span>
        </button>
      </div>

      {/* Main product display section */}
      {isLoading ? (
        <div className="text-center py-16 flex flex-col justify-center items-center">
          <div className="w-10 h-10 border-2 border-zinc-950/20 border-t-zinc-950 rounded-full animate-spin mb-4"></div>
          <p className="text-zinc-400 text-xs font-bold uppercase tracking-wider animate-pulse">
            Đang tải dữ liệu từ máy chủ...
          </p>
        </div>
      ) : filteredProducts.length === 0 ? (
        <div className="text-center py-16 bg-zinc-50/50 rounded-2xl border border-dashed border-zinc-200 p-8 flex flex-col items-center justify-center">
          <FaBox className="w-12 h-12 text-zinc-300 mb-4" />
          <p className="text-zinc-500 text-xs font-bold uppercase tracking-wider">Không tìm thấy sản phẩm nào</p>
          <p className="text-zinc-400 text-[10px] font-semibold mt-1">Hãy tạo thêm mới hoặc thay đổi từ khóa lọc tìm kiếm của bạn.</p>
        </div>
      ) : (
        <div className="table-shell">
          <table className="w-full text-xs text-left border-collapse">
            <thead>
              <tr className="table-head">
                <th className="py-4 px-4 text-center w-20">Ảnh minh họa</th>
                <th className="py-4 px-4 w-28">Mã sản phẩm</th>
                <th className="py-4 px-4">Tên sản phẩm</th>
                <th className="py-4 px-4">Số lượng biến thể SKU</th>
                <th className="py-4 px-4 text-right w-32">Khoảng giá niêm yết</th>
                <th className="py-4 px-4 text-center w-28">Trạng thái</th>
                <th className="py-4 px-4 text-center w-28">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100">
              {filteredProducts.map((product) => {
                // Calculate display price range
                const prices = product.variants ? product.variants.map(v => v.price) : [];
                const minPrice = prices.length > 0 ? Math.min(...prices) : 0;
                const maxPrice = prices.length > 0 ? Math.max(...prices) : 0;
                const priceText = minPrice === maxPrice 
                  ? formatPrice(minPrice) 
                  : `${formatPrice(minPrice)} - ${formatPrice(maxPrice)}`;

                // Retrieve thumbnail image
                const primaryImg = product.images?.find(img => img.isPrimary) || product.images?.[0];
                const displayImg = primaryImg?.imageUrl || product.imageUrl;

                return (
                  <tr key={product.id} className="hover:bg-zinc-50/40 transition-colors">
                    <td className="py-3 px-4">
                      <div className="w-12 h-12 bg-zinc-50 rounded-xl overflow-hidden flex items-center justify-center border border-zinc-200/50 relative shadow-sm">
                        {displayImg ? (
                          <img
                            src={displayImg}
                            alt={product.name}
                            className="w-full h-full object-cover"
                            onError={(e) => {
                              (e.target as HTMLImageElement).style.display = 'none';
                              const nextEl = (e.target as HTMLElement).nextElementSibling as HTMLElement | null;
                              if (nextEl) nextEl.style.display = 'flex';
                            }}
                          />
                        ) : null}
                        <div className={`absolute inset-0 bg-zinc-50 flex items-center justify-center ${displayImg ? 'hidden' : 'flex'}`}>
                          <FaRegFileImage className="text-zinc-300 w-5 h-5" />
                        </div>
                      </div>
                    </td>
                    <td className="py-3 px-4 font-mono font-bold text-zinc-950 text-xs">
                      {product.id}
                    </td>
                    <td className="py-3 px-4">
                      <div className="font-bold text-zinc-900 truncate max-w-[200px]" title={product.name}>
                        {product.name}
                      </div>
                      <div className="flex gap-1.5 mt-1">
                        {product.brandName && (
                          <span className="text-[8px] font-black uppercase text-zinc-400 bg-zinc-100 px-1.5 py-0.5 rounded">
                            {product.brandName}
                          </span>
                        )}
                        {product.categoryName && (
                          <span className="text-[8px] font-black uppercase text-zinc-400 bg-zinc-100 px-1.5 py-0.5 rounded">
                            {product.categoryName}
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="py-3 px-4 font-bold text-zinc-500 pl-8">
                      {product.variants ? product.variants.length : 0} SKUs
                    </td>
                    <td className="py-3 px-4 text-right font-extrabold text-zinc-950 text-sm">
                      {priceText}
                    </td>
                    <td className="py-3 px-4 text-center">
                      <span className={`inline-block px-2.5 py-1 rounded-lg text-[9px] font-black uppercase tracking-widest ${
                        product.status === "ACTIVE"
                          ? "bg-emerald-50 text-emerald-700 border border-emerald-200/40"
                          : "bg-rose-50 text-rose-700 border-rose-200/40"
                      }`}>
                        {product.status === "ACTIVE" ? "Mở bán" : "Lưu kho"}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-center">
                      <div className="flex justify-center gap-2">
                        <button
                          onClick={() => handleOpenEditForm(product)}
                          className="bg-white hover:bg-zinc-50 border border-zinc-200/60 text-zinc-600 p-2 rounded-lg transition flex items-center justify-center active:scale-95"
                          title="Chỉnh sửa sản phẩm & biến thể"
                        >
                          <FaEdit className="w-3.5 h-3.5" />
                        </button>
                        <button
                          onClick={() => handleDeleteProduct(product)}
                          className="bg-white hover:bg-rose-50 border border-zinc-200/60 hover:border-rose-100 hover:text-rose-600 text-zinc-500 p-2 rounded-lg transition flex items-center justify-center active:scale-95"
                          title="Xóa sản phẩm"
                        >
                          <FaTrash className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default ProductManagement;
