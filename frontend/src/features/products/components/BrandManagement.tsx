import React, { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { brandService } from "../services/brandService";
import { BrandResponse, BrandRequest } from "../types/product";
import { FaSync, FaEdit, FaTrash, FaPlus, FaTimes, FaSearch, FaExclamationTriangle, FaStar} from "react-icons/fa";
import { adminApiService } from "@/features/admin/services/adminApiService";
import { useAdminPagination } from "@/features/admin/hooks/useAdminPagination";
import { AdminPagination } from '@/features/admin/components/AdminPagination';
import { toast } from "react-toastify";

const BrandManagement = () => {
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState("");
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  const { page, size, pagination, setPage, updatePaginationData, isLoading: paginatingLoading, setIsLoading, error: paginatingError, setError } = useAdminPagination(10);

  const [formData, setFormData] = useState<BrandRequest>({
    name: "",
    slug: "",
  });

  // Fetch tree for dropdowns if needed elsewhere
  const { isLoading: isTreeLoading, error: treeError } = useQuery({
    queryKey: ["allBrands"],
    queryFn: brandService.getAllBrands,
  });

  const [paginatedBrands, setPaginatedBrands] = useState<BrandResponse[]>([]);
  const fetchPaginatedBrands = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await adminApiService.getAllBrands(page, size);
      setPaginatedBrands(response.items);
      updatePaginationData(response.pagination as any);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setIsLoading(false);
    }
  };

  React.useEffect(() => {
    fetchPaginatedBrands();
  }, [page, size]);

  const isLoading = isTreeLoading || paginatingLoading;
  const fetchError = treeError || paginatingError;


  const createMutation = useMutation({
    mutationFn: (data: BrandRequest) => brandService.createBrand(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["allBrands"] });
      fetchPaginatedBrands();
      toast.success("Tạo thương hiệu thành công!");
      handleCloseForm();
    },
    onError: (error: Error) => {
      toast.error("Lỗi tạo thương hiệu: " + error.message);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: BrandRequest }) => brandService.updateBrand(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["allBrands"] });
      fetchPaginatedBrands();
      toast.success("Cập nhật thương hiệu thành công!");
      handleCloseForm();
    },
    onError: (error: Error) => {
      toast.error("Lỗi cập nhật thương hiệu: " + error.message);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => brandService.deleteBrand(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["allBrands"] });
      fetchPaginatedBrands();
      toast.success("Xóa thương hiệu thành công!");
    },
    onError: (error: any) => {
      const errorMessage = error?.response?.data?.message || error.message;
      if (errorMessage.toLowerCase().includes("constraint") || errorMessage.toLowerCase().includes("foreign key")) {
        toast.error("Không thể xóa thương hiệu này vì đang có sản phẩm thuộc về nó. Vui lòng chuyển các sản phẩm sang thương hiệu khác trước.", {
          autoClose: 5000,
        });
      } else {
        toast.error("Lỗi xóa thương hiệu: " + errorMessage);
      }
    },
  });

  // Local search only for the current page
  let displayBrands = paginatedBrands;
  if (searchTerm) {
    displayBrands = displayBrands.filter(
      (b) => b.name.toLowerCase().includes(searchTerm.toLowerCase())
    );
  }

  const handleOpenCreateForm = () => {
    setFormData({ name: "", slug: "" });
    setEditingId(null);
    setShowCreateForm(true);
  };

  const handleEditStart = (brand: BrandResponse) => {
    setFormData({
      name: brand.name,
      slug: brand.slug,
    });
    setEditingId(brand.id);
    setShowCreateForm(true);
  };

  const handleCloseForm = () => {
    setShowCreateForm(false);
    setEditingId(null);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim() || !formData.slug?.trim()) {
      toast.error("Tên thương hiệu và Slug không được để trống");
      return;
    }

    if (editingId) {
      updateMutation.mutate({ id: editingId, data: formData });
    } else {
      createMutation.mutate(formData);
    }
  };

  const handleDelete = (id: number, name: string) => {
    if (window.confirm(`Bạn có chắc muốn xóa thương hiệu "${name}"?`)) {
      deleteMutation.mutate(id);
    }
  };

  return (
    <div className="surface p-6 sm:p-8 bg-white/70 backdrop-blur-md border border-zinc-200/50 rounded-2xl space-y-6 sm:space-y-8">
      <div className="border-b border-zinc-100 pb-5">
        <h2 className="text-lg font-extrabold text-zinc-950">Quản lý Thương hiệu</h2>
        <p className="text-xs font-semibold text-zinc-400 mt-1">
          Tạo, chỉnh sửa và quản lý các thương hiệu sản phẩm đối tác (Brands).
        </p>
      </div>

      {fetchError && (
        <div className="bg-rose-50/50 border border-rose-200/40 text-rose-700 px-4 py-3 rounded-xl text-xs font-semibold flex items-center gap-2">
          <FaExclamationTriangle className="flex-shrink-0" />
          <span>{(fetchError as Error).message}</span>
        </div>
      )}

      <div className="flex flex-col md:flex-row gap-4">
        <div className="relative flex-1">
          <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-zinc-400">
            <FaSearch className="w-3.5 h-3.5" />
          </div>
          <input
            type="text"
            placeholder="Tìm kiếm thương hiệu..."
            value={searchTerm}
            onChange={(e) => {
              setSearchTerm(e.target.value);
              setPage(0);
            }}
            className="w-full pl-10 pr-4 h-11 bg-zinc-50 border border-zinc-200/60 focus:bg-white focus:border-zinc-950 focus:ring-4 focus:ring-zinc-900/5 rounded-xl text-xs font-semibold outline-none transition duration-200 text-zinc-700 placeholder:text-zinc-400"
          />
        </div>

        <div className="flex gap-2.5">
          <button
            onClick={() => { queryClient.invalidateQueries({ queryKey: ["allBrands"] }); fetchPaginatedBrands(); }}
            disabled={isLoading}
            className="h-11 flex items-center justify-center gap-2 px-5 bg-zinc-950 text-white rounded-xl hover:bg-zinc-800 disabled:bg-zinc-100 disabled:text-zinc-400 font-extrabold text-xs uppercase tracking-wider transition-all duration-300 active:scale-95"
          >
            <FaSync className={`w-3 h-3 ${isLoading ? "animate-spin" : ""}`} />
            <span className="hidden sm:inline">Làm mới</span>
          </button>

          <button
            onClick={handleOpenCreateForm}
            className="h-11 flex items-center justify-center gap-2 px-5 bg-zinc-950 text-white rounded-xl hover:bg-zinc-800 font-extrabold text-xs uppercase tracking-wider transition-all duration-300 active:scale-95 shadow-md shadow-zinc-950/10"
          >
            <FaPlus className="w-3 h-3 text-amber-200" />
            <span className="hidden sm:inline">Tạo mới</span>
          </button>
        </div>
      </div>

      {showCreateForm && (
        <div className="bg-zinc-50/50 border border-zinc-200/40 p-6 rounded-2xl animate-slide-down">
          <div className="flex justify-between items-center mb-5 border-b border-zinc-200/40 pb-3">
            <h3 className="text-xs font-black text-zinc-950 uppercase tracking-widest flex items-center gap-2">
              <FaStar className="text-zinc-500" />
              {editingId ? "Cập nhật thương hiệu" : "Tạo thương hiệu mới"}
            </h3>
            <button onClick={handleCloseForm} className="text-zinc-400 hover:text-zinc-600 transition" title="Đóng form">
              <FaTimes className="w-4 h-4" />
            </button>
          </div>

          <form onSubmit={handleSubmit} className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div className="md:col-span-2">
              <label className="block text-[10px] font-black text-zinc-400 uppercase tracking-widest mb-2">Tên thương hiệu</label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                className="h-11 w-full rounded-xl border border-zinc-200 bg-white/95 px-4 text-xs font-semibold text-zinc-800 outline-none focus:border-zinc-950"
                required
              />
            </div>
            
            <div className="md:col-span-2">
              <label className="block text-[10px] font-black text-zinc-400 uppercase tracking-widest mb-2">Đường dẫn tĩnh (Slug)</label>
              <input
                type="text"
                value={formData.slug}
                onChange={(e) => setFormData({ ...formData, slug: e.target.value })}
                className="h-11 w-full rounded-xl border border-zinc-200 bg-white/95 px-4 text-xs font-semibold text-zinc-800 outline-none focus:border-zinc-950"
                required
              />
            </div>

            <div className="flex gap-2.5 items-end justify-end md:col-span-2">
              <button
                type="button"
                onClick={handleCloseForm}
                className="h-10 px-4 bg-zinc-100 hover:bg-zinc-200/80 text-zinc-600 rounded-xl font-extrabold text-xs transition"
              >
                Hủy bỏ
              </button>
              <button
                type="submit"
                disabled={createMutation.isPending || updateMutation.isPending}
                className="h-10 px-5 bg-zinc-950 hover:bg-zinc-800 text-white rounded-xl font-extrabold text-xs transition shadow-sm disabled:opacity-70 flex items-center gap-2"
              >
                {(createMutation.isPending || updateMutation.isPending) && <div className="w-3.5 h-3.5 border-2 border-white/20 border-t-white rounded-full animate-spin"></div>}
                <span>Lưu thông tin</span>
              </button>
            </div>
          </form>
        </div>
      )}

      {isLoading ? (
        <div className="text-center py-12 flex flex-col items-center">
          <div className="w-8 h-8 border-2 border-zinc-950/20 border-t-zinc-950 rounded-full animate-spin mb-3"></div>
          <p className="text-zinc-400 text-xs font-bold uppercase tracking-wider animate-pulse">Đang tải...</p>
        </div>
      ) : displayBrands.length === 0 ? (
        <div className="text-center py-12 bg-zinc-50/50 rounded-2xl border border-dashed border-zinc-200 p-8">
          <FaStar className="w-10 h-10 text-zinc-300 mx-auto mb-3" />
          <p className="text-zinc-500 text-xs font-bold uppercase tracking-wider">Không tìm thấy thương hiệu</p>
        </div>
      ) : (
        <div className="space-y-4">
          <div className="table-shell overflow-x-auto">
            <table className="w-full text-xs text-left border-collapse">
              <thead>
                <tr className="table-head">
                  <th className="py-4 px-4 w-20 text-center">ID</th>
                  <th className="py-4 px-4">Tên thương hiệu</th>
                  <th className="py-4 px-4">Slug</th>
                  <th className="py-4 px-4 text-center">Hành động</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100">
                {displayBrands.map((brand) => (
                  <tr key={brand.id} className="hover:bg-zinc-50/40 transition-colors">
                    <td className="py-3 px-4 text-center font-bold text-zinc-400">#{brand.id}</td>
                    <td className="py-3 px-4 font-bold text-zinc-800">{brand.name}</td>
                    <td className="py-3 px-4 font-mono text-[10px] text-zinc-500">{brand.slug}</td>
                    <td className="py-3 px-4">
                      <div className="flex justify-center gap-2">
                        <button
                          onClick={() => handleEditStart(brand)}
                          className="bg-white hover:bg-zinc-50 border border-zinc-200/60 text-zinc-600 p-2 rounded-lg transition"
                          title="Sửa"
                        >
                          <FaEdit className="w-3.5 h-3.5" />
                        </button>
                        <button
                          onClick={() => handleDelete(brand.id, brand.name)}
                          disabled={deleteMutation.isPending && deleteMutation.variables === brand.id}
                          className="bg-white hover:bg-rose-50 border border-zinc-200/60 text-zinc-500 hover:text-rose-600 p-2 rounded-lg transition disabled:opacity-50"
                          title="Xóa"
                        >
                          {(deleteMutation.isPending && deleteMutation.variables === brand.id) 
                            ? <div className="w-3.5 h-3.5 border-2 border-rose-600/20 border-t-rose-600 rounded-full animate-spin"></div> 
                            : <FaTrash className="w-3.5 h-3.5" />}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination Controls */}
          <AdminPagination pagination={pagination} onPageChange={setPage} />
        </div>
      )}
    </div>
  );
};

export default BrandManagement;
