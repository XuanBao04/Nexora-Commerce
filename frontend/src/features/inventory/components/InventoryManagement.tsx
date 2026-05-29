import { useEffect, useState } from "react";
import { inventoryService } from "../services/inventoryService";
import { Product } from "@/features/products/types/product";
import { InventoryItem } from "../types/inventory";
import { formatPrice } from "@/features/cart/utils/priceCalculation";
import { FaSync, FaEdit, FaCheck, FaTimes, FaSearch, FaBox, FaChartBar, FaCheckCircle, FaExclamationTriangle } from "react-icons/fa";
import { toast } from "react-toastify";
import { adminApiService } from '@/features/admin/services/adminApiService';
import { useAdminPagination } from '@/features/admin/hooks/useAdminPagination';
import { AdminPagination } from "@/features/admin/components/AdminPagination";

interface InventoryWithProduct extends Product {
  inventory?: InventoryItem;
  availableQuantity: number;
  reservedQuantity: number;
  soldQuantity: number;
  physicalQuantity: number;
}

const InventoryManagement = () => {
  const [products, setProducts] = useState<InventoryWithProduct[]>([]);
  const { page, size, pagination, setPage, updatePaginationData, isLoading, setIsLoading, error, setError } = useAdminPagination(10);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editQuantity, setEditQuantity] = useState<number>(0);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<"ALL" | "ACTIVE" | "INACTIVE">("ALL");

  const fetchInventory = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const productsData = await adminApiService.getAllProducts(page, size, searchTerm);
      const allProducts = productsData.items;

      const items: InventoryWithProduct[] = [];
      for (const product of allProducts) {
        if (product.variants && product.variants.length > 0) {
          for (const v of product.variants) {
            const reserved = v.reservedQuantity ?? 0;
            const sold = v.soldQuantity ?? 0;
            items.push({
              ...product,
              id: v.sku, // Use SKU as the unique ID for table row & editing
              price: v.price, // Use SKU variant price
              availableQuantity: v.quantity - reserved,
              reservedQuantity: reserved,
              soldQuantity: sold,
              physicalQuantity: v.quantity,
              variantAttributes: v.attributes || [],
            } as any);
          }
        } else {
          // Fallback to product.id as SKU if variants is empty
          items.push({
            ...product,
            availableQuantity: product.quantity ?? 0,
            reservedQuantity: 0,
            soldQuantity: 0,
            physicalQuantity: product.quantity ?? 0,
            variantAttributes: [],
          } as any);
        }
      }

      setProducts(items);
      updatePaginationData(productsData.pagination as any);
    } catch (err) {
      setError((err as Error).message || "Không thể tải dữ liệu tồn kho");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchInventory();
  }, [page, size]);

  const handleEditStart = (productId: string, currentQuantity: number) => {
    setEditingId(productId);
    setEditQuantity(currentQuantity);
  };

  const handleSaveInventory = async (productId: string) => {
    try {
      await inventoryService.updateInventoryItem(productId, editQuantity);
      
      setProducts(products.map(p => 
        p.id === productId ? { 
          ...p, 
          physicalQuantity: editQuantity,
          availableQuantity: editQuantity - (p.reservedQuantity || 0)
        } : p
      ));
      
      setEditingId(null);
      toast.success("Cập nhật tồn kho thành công!");
    } catch (err) {
      toast.error("Lỗi cập nhật tồn kho: " + (err as Error).message);
    }
  };

  const handleCancel = () => {
    setEditingId(null);
    setEditQuantity(0);
  };

  let filteredProducts = products;

  if (statusFilter !== "ALL") {
    filteredProducts = filteredProducts.filter(p => p.status === statusFilter);
  }

  if (searchTerm) {
    filteredProducts = filteredProducts.filter(p =>
      p.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.id.toLowerCase().includes(searchTerm.toLowerCase())
    );
  }

  return (
    <div className="surface p-6 sm:p-8 bg-white/70 backdrop-blur-md border border-zinc-200/50 rounded-2xl space-y-6 sm:space-y-8">
      <div className="border-b border-zinc-100 pb-5">
        <h2 className="text-lg font-extrabold text-zinc-950">Quản lý tồn kho hàng hóa</h2>
        <p className="text-xs font-semibold text-zinc-400 mt-1">Kiểm tra thông số tồn kho khả dụng, điều chỉnh sản phẩm thực tế và theo dõi số lượng đơn hàng đặt giữ.</p>
      </div>

      {error && (
        <div className="bg-rose-50/50 border border-rose-200/40 text-rose-700 px-4 py-3 rounded-xl text-xs font-semibold flex items-center gap-2">
          <FaExclamationTriangle className="flex-shrink-0" />
          <span>{error}</span>
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
            placeholder="Tìm kiếm sản phẩm theo tên hoặc mã..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && fetchInventory()}
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
          onClick={fetchInventory}
          disabled={isLoading}
          className="h-11 flex items-center justify-center gap-2 px-5 bg-zinc-950 text-white rounded-xl hover:bg-zinc-800 disabled:bg-zinc-100 disabled:text-zinc-400 font-extrabold text-xs uppercase tracking-wider transition-all duration-300"
        >
          <FaSync className={`w-3 h-3 ${isLoading ? "animate-spin" : ""}`} />
          <span>Tìm kiếm</span>
        </button>
      </div>

      {/* Modern High-End Stats Metrics Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
        <div className="bg-white/80 border border-zinc-200/40 p-6 rounded-2xl shadow-sm flex items-center gap-4 transition hover:shadow-md">
          <div className="w-11 h-11 rounded-xl bg-zinc-950 text-white flex items-center justify-center shadow-md shadow-zinc-950/15">
            <FaBox className="w-4 h-4 text-amber-200" />
          </div>
          <div>
            <p className="text-zinc-400 text-[9px] font-black uppercase tracking-widest">Tổng mặt hàng</p>
            <p className="text-2xl font-black text-zinc-950 mt-0.5">{products.length}</p>
          </div>
        </div>

        <div className="bg-white/80 border border-zinc-200/40 p-6 rounded-2xl shadow-sm flex items-center gap-4 transition hover:shadow-md">
          <div className="w-11 h-11 rounded-xl bg-emerald-50 border border-emerald-100 text-emerald-600 flex items-center justify-center shadow-sm">
            <FaCheckCircle className="w-4 h-4" />
          </div>
          <div>
            <p className="text-zinc-400 text-[9px] font-black uppercase tracking-widest">Đang hoạt động</p>
            <p className="text-2xl font-black text-emerald-600 mt-0.5">
              {products.filter(p => p.status === "ACTIVE").length}
            </p>
          </div>
        </div>

        <div className="bg-white/80 border border-zinc-200/40 p-6 rounded-2xl shadow-sm flex items-center gap-4 transition hover:shadow-md">
          <div className="w-11 h-11 rounded-xl bg-amber-50 border border-amber-100 text-amber-600 flex items-center justify-center shadow-sm">
            <FaChartBar className="w-4 h-4" />
          </div>
          <div>
            <p className="text-zinc-400 text-[9px] font-black uppercase tracking-widest">Tổng kho thực tế</p>
            <p className="text-2xl font-black text-amber-600 mt-0.5">
              {products.reduce((sum, p) => sum + (p.physicalQuantity || 0), 0)}
            </p>
          </div>
        </div>
      </div>

      {/* Details matrix table */}
      {isLoading ? (
        <div className="text-center py-12 flex flex-col justify-center items-center">
          <div className="w-8 h-8 border-2 border-zinc-950/20 border-t-zinc-950 rounded-full animate-spin mb-3"></div>
          <p className="text-zinc-400 text-xs font-bold uppercase tracking-wider animate-pulse">Đang đồng bộ tồn kho...</p>
        </div>
      ) : filteredProducts.length === 0 ? (
        <div className="text-center py-12 bg-zinc-50/50 rounded-2xl border border-dashed border-zinc-200 p-8">
          <FaBox className="w-10 h-10 text-zinc-300 mx-auto mb-3" />
          <p className="text-zinc-500 text-xs font-bold uppercase tracking-wider">Không tìm thấy sản phẩm nào</p>
        </div>
      ) : (
        <div className="table-shell">
          <table className="w-full text-xs text-left border-collapse">
            <thead>
              <tr className="table-head">
                <th className="py-4 px-4 text-center">Ảnh</th>
                <th className="py-4 px-4">Mã sản phẩm</th>
                <th className="py-4 px-4">Tên sản phẩm</th>
                <th className="py-4 px-4 text-right">Đơn giá</th>
                <th className="py-4 px-4 text-center">Khả dụng</th>
                <th className="py-4 px-4 text-center">Đang giữ</th>
                <th className="py-4 px-4 text-center w-36">Tồn thực tế</th>
                <th className="py-4 px-4 text-center">Đã bán</th>
                <th className="py-4 px-4 text-center">Trạng thái</th>
                <th className="py-4 px-4 text-center">Hành động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100">
              {filteredProducts.map((product) => (
                <tr key={product.id} className="hover:bg-zinc-50/40 transition-colors">
                  <td className="py-3 px-4 flex justify-center">
                    <div className="w-11 h-11 bg-zinc-50 rounded-xl overflow-hidden flex items-center justify-center border border-zinc-200/50">
                      {product.imageUrl ? (
                        <img
                          src={product.imageUrl}
                          alt={product.name}
                          className="w-full h-full object-cover"
                          onError={(e) => {
                            (e.target as HTMLImageElement).style.display = 'none';
                            const nextEl = (e.target as any).nextElementSibling;
                            if (nextEl) {
                              nextEl.style.display = 'block';
                            }
                          }}
                        />
                      ) : null}
                      <span className={`text-zinc-400 text-[9px] font-bold uppercase ${product.imageUrl ? 'hidden' : 'block'}`}>
                        No Img
                      </span>
                    </div>
                  </td>
                  <td className="py-3 px-4 font-mono font-bold text-zinc-950 text-xs">{product.id}</td>
                  <td className="py-3 px-4">
                    <div className="font-bold text-zinc-800 max-w-xs truncate" title={product.name}>
                      {product.name}
                    </div>
                    {(product as any).variantAttributes && (product as any).variantAttributes.length > 0 && (
                      <div className="flex flex-wrap gap-1 mt-1">
                        {(product as any).variantAttributes.map((attr: any, idx: number) => (
                          <span key={idx} className="text-[9px] font-bold uppercase tracking-wider text-zinc-500 bg-zinc-100 px-1.5 py-0.5 rounded animate-fade-in">
                            {attr.name}: {attr.value}
                          </span>
                        ))}
                      </div>
                    )}
                  </td>
                  <td className="py-3 px-4 text-right font-extrabold text-zinc-950">{formatPrice(product.price ?? 0)}</td>
                  <td className="py-3 px-4 text-center">
                    <span className="inline-block px-2.5 py-1 rounded-lg font-black bg-emerald-50 text-emerald-700 border border-emerald-100/50">
                      {product.availableQuantity}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-center">
                    <span className={`inline-block px-2.5 py-1 rounded-lg font-black ${
                      product.reservedQuantity > 0 ? "bg-amber-50 text-amber-700 border border-amber-100/50 animate-pulse" : "bg-zinc-50 text-zinc-400 border border-zinc-200/50"
                    }`}>
                      {product.reservedQuantity}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-center">
                    {editingId === product.id ? (
                      <input
                        type="number"
                        min="0"
                        value={editQuantity}
                        onChange={(e) => setEditQuantity(Math.max(0, parseInt(e.target.value) || 0))}
                        className="w-20 h-8 text-center bg-white border border-zinc-950 focus:ring-4 focus:ring-zinc-900/5 rounded-lg font-black text-zinc-950 outline-none"
                        autoFocus
                      />
                    ) : (
                      <span className={`font-black text-sm ${
                        (product.physicalQuantity || 0) <= 5 ? "text-rose-600 bg-rose-50/50 px-2 py-0.5 rounded-lg border border-rose-100" : "text-zinc-800"
                      }`}>
                        {product.physicalQuantity}
                      </span>
                    )}
                  </td>
                  <td className="py-3 px-4 text-center text-zinc-950 font-black">{product.soldQuantity}</td>
                  <td className="py-3 px-4 text-center">
                    <span className={`inline-block px-2.5 py-1 rounded-lg text-[9px] font-black uppercase tracking-widest ${
                      product.status === "ACTIVE"
                        ? "bg-emerald-50 text-emerald-700 border border-emerald-200/40"
                        : "bg-rose-50 text-rose-700 border-rose-200/40"
                    }`}>
                      {product.status === "ACTIVE" ? "Hoạt động" : "Ngừng bán"}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-center">
                    {editingId === product.id ? (
                      <div className="flex justify-center gap-1.5">
                        <button
                          onClick={() => handleSaveInventory(product.id)}
                          className="bg-zinc-950 hover:bg-zinc-800 text-white p-2 rounded-lg transition shadow-sm"
                          title="Lưu"
                        >
                          <FaCheck className="w-3 h-3" />
                        </button>
                        <button
                          onClick={handleCancel}
                          className="bg-zinc-100 hover:bg-zinc-200 text-zinc-600 p-2 rounded-lg transition shadow-sm border border-zinc-200/50"
                          title="Hủy"
                        >
                          <FaTimes className="w-3 h-3" />
                        </button>
                      </div>
                    ) : (
                      <button
                        onClick={() => handleEditStart(product.id, product.physicalQuantity || 0)}
                        className="bg-white hover:bg-zinc-50 border border-zinc-200/60 text-zinc-600 p-2 rounded-lg transition flex items-center justify-center mx-auto active:scale-95"
                        title="Chỉnh sửa tồn kho"
                      >
                        <FaEdit className="w-3.5 h-3.5" />
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Pagination Controls */}
      <AdminPagination pagination={pagination} onPageChange={setPage} />
    </div>
  );
};

export default InventoryManagement;
