import React, { useEffect, useState, useRef } from "react";
import { productService } from "../../services/api/productService";
import { Product } from "../../types/product";
import { formatPrice } from "../../utils/priceCalculation";
import { 
  FaSync, FaEdit, FaTrash, FaPlus, FaSearch, FaBox, 
  FaCloudUploadAlt, FaExclamationTriangle, FaCheck, FaTimes, 
  FaRegFileImage, FaImage
} from "react-icons/fa";
import { toast } from "react-toastify";

interface ProductFormData {
  id: string;
  name: string;
  description: string;
  price: string;
  status: "ACTIVE" | "INACTIVE";
}

const ProductManagement = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  // Search & Filter state
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<"ALL" | "ACTIVE" | "INACTIVE">("ALL");

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
    price: "",
    status: "ACTIVE",
  });

  const fetchProducts = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await productService.getAllProducts();
      setProducts(data);
    } catch (err) {
      setError((err as Error).message || "Không thể tải danh sách sản phẩm");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts();
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
      // Validate file type
      if (!file.type.startsWith("image/")) {
        toast.error("Vui lòng chọn tệp hình ảnh hợp lệ (png, jpg, jpeg, webp)");
        return;
      }
      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        toast.error("Kích thước hình ảnh không được vượt quá 5MB");
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
      price: "",
      status: "ACTIVE",
    });
    setSelectedImageFile(null);
    setImagePreviewUrl(null);
    if (fileInputRef.current) fileInputRef.current.value = "";
    setIsEditMode(false);
    setShowForm(true);
  };

  const handleOpenEditForm = (product: Product) => {
    setFormData({
      id: product.id,
      name: product.name,
      description: product.description || "",
      price: product.price.toString(),
      status: product.status,
    });
    setSelectedImageFile(null);
    setImagePreviewUrl(product.imageUrl || null);
    if (fileInputRef.current) fileInputRef.current.value = "";
    setIsEditMode(true);
    setShowForm(true);

    // Scroll to form smoothly
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleCloseForm = () => {
    setShowForm(false);
    setIsEditMode(false);
    setSelectedImageFile(null);
    setImagePreviewUrl(null);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Validations
    if (isEditMode && !formData.id.trim()) {
      toast.error("Mã sản phẩm không được để trống");
      return;
    }
    if (!formData.name.trim()) {
      toast.error("Tên sản phẩm không được để trống");
      return;
    }
    const priceNum = parseFloat(formData.price);
    if (isNaN(priceNum) || priceNum <= 0) {
      toast.error("Đơn giá sản phẩm phải là số lớn hơn 0");
      return;
    }

    setIsSubmitting(true);
    try {
      const uploadFormData = new FormData();
      if (formData.id.trim()) {
        uploadFormData.append("id", formData.id.trim());
      }
      uploadFormData.append("name", formData.name.trim());
      uploadFormData.append("description", formData.description.trim());
      uploadFormData.append("price", priceNum.toString());
      uploadFormData.append("status", formData.status);

      if (selectedImageFile) {
        uploadFormData.append("image", selectedImageFile);
      }

      if (isEditMode) {
        await productService.updateProduct(formData.id, uploadFormData);
        toast.success("Cập nhật sản phẩm thành công!");
      } else {
        await productService.createProduct(uploadFormData);
        toast.success("Thêm sản phẩm mới thành công!");
      }

      handleCloseForm();
      fetchProducts();
    } catch (err) {
      toast.error(`Lỗi ${isEditMode ? "cập nhật" : "thêm"} sản phẩm: ` + (err as Error).message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteProduct = async (product: Product) => {
    if (confirm(`Bạn có chắc muốn xóa sản phẩm "${product.name}" (${product.id})? Hành động này không thể hoàn tác!`)) {
      setIsLoading(true);
      try {
        await productService.deleteProduct(product.id);
        toast.success("Xóa sản phẩm thành công!");
        fetchProducts();
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
        (p.description && p.description.toLowerCase().includes(keyword))
    );
  }

  return (
    <div className="surface p-6 sm:p-8 bg-white/70 backdrop-blur-md border border-zinc-200/50 rounded-2xl space-y-6 sm:space-y-8">
      {/* Header Section */}
      <div className="border-b border-zinc-100 pb-5 flex flex-col sm:flex-row justify-between sm:items-center gap-4">
        <div>
          <h2 className="text-lg font-extrabold text-zinc-950">Quản lý danh mục sản phẩm</h2>
          <p className="text-xs font-semibold text-zinc-400 mt-1">
            Xem danh sách sản phẩm, tạo mới, chỉnh sửa thông tin hoặc xóa sản phẩm cùng tính năng tích hợp Cloudinary.
          </p>
        </div>
        {!showForm && (
          <button
            onClick={handleOpenCreateForm}
            className="self-start sm:self-center h-11 flex items-center justify-center gap-2 px-5 bg-zinc-950 text-white rounded-xl hover:bg-zinc-800 font-extrabold text-xs uppercase tracking-wider transition-all duration-300 active:scale-95 shadow-md shadow-zinc-900/10"
          >
            <FaPlus className="w-3 h-3" />
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

      {/* Slide-down Form Container */}
      {showForm && (
        <div className="bg-zinc-50/50 border border-zinc-200/40 p-6 rounded-2xl mb-8 animate-slide-down shadow-inner">
          <div className="flex justify-between items-center mb-5 border-b border-zinc-200/40 pb-3">
            <h3 className="text-xs font-black text-zinc-950 uppercase tracking-widest flex items-center gap-2">
              <FaBox className="text-zinc-500" />
              {isEditMode ? `Cập nhật sản phẩm: ${formData.id}` : "Thêm sản phẩm mới"}
            </h3>
            <button
              onClick={handleCloseForm}
              className="text-zinc-400 hover:text-zinc-600 transition"
              title="Đóng form"
            >
              <FaTimes className="w-4 h-4" />
            </button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Product Info inputs */}
              <div className="lg:col-span-2 space-y-4">
                <div>
                  {isEditMode ? (
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                      <div>
                        <label className="block text-[10px] font-black text-zinc-400 uppercase tracking-widest mb-2">
                          Mã sản phẩm (UUID v7)
                        </label>
                        <input
                          type="text"
                          value={formData.id}
                          disabled
                          className="h-11 w-full rounded-xl border border-zinc-200 bg-zinc-100 text-zinc-400 cursor-not-allowed px-4 text-xs font-semibold outline-none"
                        />
                      </div>
                      <div>
                        <label className="block text-[10px] font-black text-zinc-400 uppercase tracking-widest mb-2">
                          Tên sản phẩm
                        </label>
                        <input
                          type="text"
                          value={formData.name}
                          onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                          placeholder="VD: Áo Thun Nam Cotton Premium"
                          className="h-11 w-full rounded-xl border border-zinc-200 bg-white/95 px-4 text-xs font-semibold text-zinc-800 outline-none transition-all duration-200 placeholder:text-zinc-400 focus:border-zinc-950 focus:bg-white focus:ring-4 focus:ring-zinc-900/5"
                          required
                        />
                      </div>
                    </div>
                  ) : (
                    <div>
                      <label className="block text-[10px] font-black text-zinc-400 uppercase tracking-widest mb-2">
                        Tên sản phẩm
                      </label>
                      <input
                        type="text"
                        value={formData.name}
                        onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                        placeholder="VD: Áo Thun Nam Cotton Premium"
                        className="h-11 w-full rounded-xl border border-zinc-200 bg-white/95 px-4 text-xs font-semibold text-zinc-800 outline-none transition-all duration-200 placeholder:text-zinc-400 focus:border-zinc-950 focus:bg-white focus:ring-4 focus:ring-zinc-900/5"
                        required
                      />
                    </div>
                  )}
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-[10px] font-black text-zinc-400 uppercase tracking-widest mb-2">
                      Đơn giá sản phẩm (VND)
                    </label>
                    <input
                      type="number"
                      min="1"
                      value={formData.price}
                      onChange={(e) => setFormData({ ...formData, price: e.target.value })}
                      placeholder="VD: 150000"
                      className="h-11 w-full rounded-xl border border-zinc-200 bg-white/95 px-4 text-xs font-semibold text-zinc-800 outline-none transition-all duration-200 placeholder:text-zinc-400 focus:border-zinc-950 focus:bg-white focus:ring-4 focus:ring-zinc-900/5"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-[10px] font-black text-zinc-400 uppercase tracking-widest mb-2">
                      Trạng thái hoạt động
                    </label>
                    <select
                      value={formData.status}
                      onChange={(e) => setFormData({ ...formData, status: e.target.value as "ACTIVE" | "INACTIVE" })}
                      className="h-11 w-full rounded-xl border border-zinc-200 bg-white/95 px-4 text-xs font-bold text-zinc-700 outline-none transition-all duration-200 focus:border-zinc-950 focus:bg-white focus:ring-4 focus:ring-zinc-900/5"
                    >
                      <option value="ACTIVE">ACTIVE (Mở bán)</option>
                      <option value="INACTIVE">INACTIVE (Đóng kho/Ngừng bán)</option>
                    </select>
                  </div>
                </div>

                <div>
                  <label className="block text-[10px] font-black text-zinc-400 uppercase tracking-widest mb-2">
                    Mô tả sản phẩm
                  </label>
                  <textarea
                    rows={4}
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    placeholder="Mô tả thông tin chi tiết về sản phẩm..."
                    className="w-full rounded-xl border border-zinc-200 bg-white/95 p-4 text-xs font-semibold text-zinc-800 outline-none transition-all duration-200 placeholder:text-zinc-400 focus:border-zinc-950 focus:bg-white focus:ring-4 focus:ring-zinc-900/5"
                  />
                </div>
              </div>

              {/* Cloudinary Image Selector & Preview */}
              <div className="flex flex-col">
                <span className="block text-[10px] font-black text-zinc-400 uppercase tracking-widest mb-2">
                  Hình ảnh sản phẩm (Cloudinary)
                </span>
                
                <div className="flex-1 flex flex-col items-center justify-center border border-dashed border-zinc-300 bg-white rounded-2xl p-4 min-h-[220px] transition duration-200 hover:border-zinc-500 relative group overflow-hidden">
                  {imagePreviewUrl ? (
                    <div className="w-full h-full flex flex-col justify-between items-center gap-3">
                      <div className="relative w-full aspect-square max-h-[160px] rounded-xl overflow-hidden bg-zinc-50 border border-zinc-150 flex items-center justify-center">
                        <img
                          src={imagePreviewUrl}
                          alt="Preview"
                          className="w-full h-full object-contain"
                        />
                        <button
                          type="button"
                          onClick={() => {
                            setSelectedImageFile(null);
                            setImagePreviewUrl(null);
                            if (fileInputRef.current) fileInputRef.current.value = "";
                          }}
                          className="absolute top-2 right-2 bg-rose-600/90 text-white rounded-full p-1.5 shadow-md hover:bg-rose-700 transition active:scale-90"
                          title="Xóa ảnh"
                        >
                          <FaTimes className="w-2.5 h-2.5" />
                        </button>
                      </div>
                      <span className="text-[10px] font-semibold text-zinc-400 truncate max-w-full text-center">
                        {selectedImageFile ? selectedImageFile.name : "Ảnh hiện tại trên hệ thống"}
                      </span>
                    </div>
                  ) : (
                    <div className="text-center flex flex-col items-center justify-center gap-3 py-6">
                      <div className="w-12 h-12 rounded-xl bg-zinc-50 flex items-center justify-center text-zinc-400 border border-zinc-200 group-hover:text-zinc-600 transition">
                        <FaCloudUploadAlt className="w-6 h-6" />
                      </div>
                      <div className="text-xs">
                        <span className="font-extrabold text-zinc-950 block">Chọn tệp ảnh tải lên</span>
                        <span className="text-[10px] font-semibold text-zinc-400 block mt-0.5">PNG, JPG tối đa 5MB</span>
                      </div>
                    </div>
                  )}

                  <input
                    type="file"
                    ref={fileInputRef}
                    onChange={handleImageChange}
                    accept="image/*"
                    className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                  />
                </div>
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
                className="h-11 px-6 bg-zinc-950 hover:bg-zinc-800 text-white rounded-xl font-extrabold text-xs transition shadow-sm shadow-zinc-950/10 active:scale-95 disabled:bg-zinc-800 disabled:opacity-80 flex items-center justify-center gap-2"
              >
                {isSubmitting ? (
                  <>
                    <div className="w-3.5 h-3.5 border-2 border-white/20 border-t-white rounded-full animate-spin"></div>
                    <span>Đang xử lý...</span>
                  </>
                ) : (
                  <>
                    <FaCheck className="w-3 h-3 text-amber-200" />
                    <span>{isEditMode ? "Cập nhật sản phẩm" : "Thêm sản phẩm"}</span>
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
            placeholder="Tìm kiếm sản phẩm theo tên, mã SKU hoặc mô tả..."
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
          onClick={fetchProducts}
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
                <th className="py-4 px-4 w-28">Mã SKU</th>
                <th className="py-4 px-4">Tên sản phẩm</th>
                <th className="py-4 px-4">Mô tả chi tiết</th>
                <th className="py-4 px-4 text-right w-32">Đơn giá niêm yết</th>
                <th className="py-4 px-4 text-center w-28">Trạng thái</th>
                <th className="py-4 px-4 text-center w-28">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100">
              {filteredProducts.map((product) => (
                <tr key={product.id} className="hover:bg-zinc-50/40 transition-colors">
                  <td className="py-3 px-4">
                    <div className="w-12 h-12 bg-zinc-50 rounded-xl overflow-hidden flex items-center justify-center border border-zinc-200/50 relative shadow-sm">
                      {product.imageUrl ? (
                        <img
                          src={product.imageUrl}
                          alt={product.name}
                          className="w-full h-full object-cover"
                          onError={(e) => {
                            (e.target as HTMLImageElement).style.display = 'none';
                            const nextEl = (e.target as any).nextElementSibling;
                            if (nextEl) nextEl.style.display = 'flex';
                          }}
                        />
                      ) : null}
                      <div className={`absolute inset-0 bg-zinc-50 flex items-center justify-center ${product.imageUrl ? 'hidden' : 'flex'}`}>
                        <FaRegFileImage className="text-zinc-300 w-5 h-5" />
                      </div>
                    </div>
                  </td>
                  <td className="py-3 px-4 font-mono font-bold text-zinc-950 text-xs">
                    {product.id}
                  </td>
                  <td className="py-3 px-4 font-bold text-zinc-900 max-w-[200px] truncate">
                    {product.name}
                  </td>
                  <td className="py-3 px-4 text-zinc-400 font-semibold max-w-[280px] truncate">
                    {product.description || <em className="text-zinc-300 text-[10px]">Chưa bổ sung mô tả</em>}
                  </td>
                  <td className="py-3 px-4 text-right font-extrabold text-zinc-950 text-sm">
                    {formatPrice(product.price)}
                  </td>
                  <td className="py-3 px-4 text-center">
                    <span className={`inline-block px-2.5 py-1 rounded-lg text-[9px] font-black uppercase tracking-widest ${
                      product.status === "ACTIVE"
                        ? "bg-emerald-50 text-emerald-700 border border-emerald-200/40"
                        : "bg-rose-50 text-rose-700 border-rose-200/40"
                    }`}>
                      {product.status === "ACTIVE" ? "Mở bán" : "Ngừng bán"}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-center">
                    <div className="flex justify-center gap-2">
                      <button
                        onClick={() => handleOpenEditForm(product)}
                        className="bg-white hover:bg-zinc-50 border border-zinc-200/60 text-zinc-600 p-2 rounded-lg transition flex items-center justify-center active:scale-95"
                        title="Chỉnh sửa sản phẩm"
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
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default ProductManagement;
