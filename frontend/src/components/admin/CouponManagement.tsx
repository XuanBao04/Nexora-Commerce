import React, { useEffect, useState } from "react";
import { couponService } from "../../services/api/couponService";
import { CouponResponse, CouponRequest, UpdateCouponRequest } from "../../types/coupon";
import { formatPrice } from "../../utils/priceCalculation";
import { FaSync, FaEdit, FaTrash, FaCheck, FaTimes, FaPlus, FaTag, FaSearch, FaCheckCircle, FaHourglassEnd, FaExclamationTriangle } from "react-icons/fa";
import { toast } from "react-toastify";

interface CreateFormData {
  code: string;
  discountPercent: string;
  active: boolean;
  minimumOrderAmount: string;
  expiryDate: string;
}

interface EditFormData {
  discountPercent: string;
  active: boolean;
  minimumOrderAmount: string;
  expiryDate: string;
}

const CouponManagement = () => {
  const [coupons, setCoupons] = useState<CouponResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [editingCode, setEditingCode] = useState<string | null>(null);

  // Create form state
  const [createForm, setCreateForm] = useState<CreateFormData>({
    code: "",
    discountPercent: "10",
    active: true,
    minimumOrderAmount: "0",
    expiryDate: "",
  });

  // Edit form state
  const [editForm, setEditForm] = useState<EditFormData>({
    discountPercent: "10",
    active: true,
    minimumOrderAmount: "0",
    expiryDate: "",
  });

  const fetchCoupons = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await couponService.getAllCoupons();
      setCoupons(data);
    } catch (err) {
      setError((err as Error).message || "Không thể tải dữ liệu mã giảm giá");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchCoupons();
  }, []);

  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!createForm.code.trim()) {
      toast.error("Mã giảm giá không được để trống");
      return;
    }
    if (parseInt(createForm.discountPercent) <= 0 || parseInt(createForm.discountPercent) > 100) {
      toast.error("Phần trăm giảm giá phải từ 1-100");
      return;
    }

    try {
      const request: CouponRequest = {
        code: createForm.code.toUpperCase(),
        discountPercent: parseInt(createForm.discountPercent),
        active: createForm.active,
        minimumOrderAmount: parseInt(createForm.minimumOrderAmount) || 0,
        expiryDate: createForm.expiryDate || undefined,
      };

      await couponService.createCoupon(request);
      await fetchCoupons();
      setShowCreateForm(false);
      setCreateForm({
        code: "",
        discountPercent: "10",
        active: true,
        minimumOrderAmount: "0",
        expiryDate: "",
      });
      toast.success("Tạo mã giảm giá thành công!");
    } catch (err) {
      toast.error("Lỗi tạo mã giảm giá: " + (err as Error).message);
    }
  };

  const handleEditStart = (coupon: CouponResponse) => {
    setEditingCode(coupon.code);
    setEditForm({
      discountPercent: coupon.discountPercent.toString(),
      active: coupon.active,
      minimumOrderAmount: coupon.minimumOrderAmount.toString(),
      expiryDate: coupon.expiryDate ? new Date(coupon.expiryDate).toISOString().split("T")[0] : "",
    });
  };

  const handleEditSubmit = async (code: string) => {
    if (parseInt(editForm.discountPercent) <= 0 || parseInt(editForm.discountPercent) > 100) {
      toast.error("Phần trăm giảm giá phải từ 1-100");
      return;
    }

    try {
      const request: UpdateCouponRequest = {
        discountPercent: parseInt(editForm.discountPercent),
        active: editForm.active,
        minimumOrderAmount: parseInt(editForm.minimumOrderAmount) || 0,
        expiryDate: editForm.expiryDate || undefined,
      };

      await couponService.updateCoupon(code, request);
      await fetchCoupons();
      setEditingCode(null);
      toast.success("Cập nhật mã giảm giá thành công!");
    } catch (err) {
      toast.error("Lỗi cập nhật mã giảm giá: " + (err as Error).message);
    }
  };

  const handleDelete = async (code: string) => {
    if (confirm(`Bạn có chắc muốn xóa mã giảm giá ${code}?`)) {
      try {
        await couponService.deleteCoupon(code);
        await fetchCoupons();
        toast.success("Xóa mã giảm giá thành công!");
      } catch (err) {
        toast.error("Lỗi xóa mã giảm giá: " + (err as Error).message);
      }
    }
  };

  const handleCancel = () => {
    setEditingCode(null);
    setShowCreateForm(false);
  };

  let filteredCoupons = coupons;
  if (searchTerm) {
    filteredCoupons = filteredCoupons.filter((c) =>
      c.code.toLowerCase().includes(searchTerm.toLowerCase())
    );
  }

  const isExpired = (expiryDate: string | null) => {
    if (!expiryDate) return false;
    return new Date(expiryDate) < new Date();
  };

  const formatDate = (dateString: string | null) => {
    if (!dateString) return "Không có hạn";
    return new Date(dateString).toLocaleDateString("vi-VN");
  };

  return (
    <div className="surface p-6 sm:p-8 bg-white/70 backdrop-blur-md border border-zinc-200/50 rounded-2xl space-y-6 sm:space-y-8">
      {/* Header Section */}
      <div className="border-b border-zinc-100 pb-5">
        <h2 className="text-lg font-extrabold text-zinc-950">Quản lý Mã giảm giá</h2>
        <p className="text-xs font-semibold text-zinc-400 mt-1">Thiết lập các chiến dịch chiết khấu, hạn định thời gian và điều kiện tối thiểu áp dụng cho khách hàng.</p>
      </div>

      {error && (
        <div className="bg-rose-50/50 border border-rose-200/40 text-rose-700 px-4 py-3 rounded-xl text-xs font-semibold flex items-center gap-2">
          <FaExclamationTriangle className="flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Controls Bar */}
      <div className="flex flex-col md:flex-row gap-4">
        {/* Search bar */}
        <div className="relative flex-1">
          <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-zinc-400">
            <FaSearch className="w-3.5 h-3.5" />
          </div>
          <input
            type="text"
            placeholder="Tìm kiếm mã giảm giá..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 h-11 bg-zinc-50 border border-zinc-200/60 focus:bg-white focus:border-zinc-950 focus:ring-4 focus:ring-zinc-900/5 rounded-xl text-xs font-semibold outline-none transition duration-200 text-zinc-700 placeholder:text-zinc-400"
          />
        </div>

        <div className="flex gap-2.5">
          <button
            onClick={fetchCoupons}
            disabled={isLoading}
            className="h-11 flex items-center justify-center gap-2 px-5 bg-zinc-950 text-white rounded-xl hover:bg-zinc-800 disabled:bg-zinc-100 disabled:text-zinc-400 font-extrabold text-xs uppercase tracking-wider transition-all duration-300 active:scale-95"
          >
            <FaSync className={`w-3 h-3 ${isLoading ? "animate-spin" : ""}`} />
            <span>Làm mới</span>
          </button>

          <button
            onClick={() => setShowCreateForm(!showCreateForm)}
            className="h-11 flex items-center justify-center gap-2 px-5 bg-zinc-950 text-white rounded-xl hover:bg-zinc-800 font-extrabold text-xs uppercase tracking-wider transition-all duration-300 active:scale-95"
          >
            <FaPlus className="w-3 h-3" />
            <span>Tạo mới</span>
          </button>
        </div>
      </div>

      {/* Create Form Drawer */}
      {showCreateForm && (
        <div className="bg-zinc-50/50 border border-zinc-200/40 p-6 rounded-2xl mb-8 animate-slide-down">
          <h3 className="text-xs font-black text-zinc-950 uppercase tracking-widest mb-4 flex items-center gap-2 border-b border-zinc-200/30 pb-2.5">
            <FaTag className="text-zinc-500" />
            Tạo mã giảm giá mới
          </h3>
          <form onSubmit={handleCreateSubmit} className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div>
              <label className="block text-[10px] font-black text-zinc-400 uppercase tracking-widest mb-2">Mã giảm giá</label>
              <input
                type="text"
                value={createForm.code}
                onChange={(e) => setCreateForm({ ...createForm, code: e.target.value.toUpperCase() })}
                placeholder="VD: WELCOME10"
                className="h-11 w-full rounded-xl border border-zinc-200 bg-white/95 px-4 text-xs font-semibold text-zinc-800 outline-none transition-all duration-200 placeholder:text-zinc-400 focus:border-zinc-950 focus:bg-white focus:ring-4 focus:ring-zinc-900/5"
                required
              />
            </div>

            <div>
              <label className="block text-[10px] font-black text-zinc-400 uppercase tracking-widest mb-2">Phần trăm giảm (%)</label>
              <input
                type="number"
                min="1"
                max="100"
                value={createForm.discountPercent}
                onChange={(e) => setCreateForm({ ...createForm, discountPercent: e.target.value })}
                className="h-11 w-full rounded-xl border border-zinc-200 bg-white/95 px-4 text-xs font-semibold text-zinc-800 outline-none transition-all duration-200 placeholder:text-zinc-400 focus:border-zinc-950 focus:bg-white focus:ring-4 focus:ring-zinc-900/5"
                required
              />
            </div>

            <div>
              <label className="block text-[10px] font-black text-zinc-400 uppercase tracking-widest mb-2">Số tiền tối thiểu (VND)</label>
              <input
                type="number"
                min="0"
                value={createForm.minimumOrderAmount}
                onChange={(e) => setCreateForm({ ...createForm, minimumOrderAmount: e.target.value })}
                className="h-11 w-full rounded-xl border border-zinc-200 bg-white/95 px-4 text-xs font-semibold text-zinc-800 outline-none transition-all duration-200 placeholder:text-zinc-400 focus:border-zinc-950 focus:bg-white focus:ring-4 focus:ring-zinc-900/5"
              />
            </div>

            <div>
              <label className="block text-[10px] font-black text-zinc-400 uppercase tracking-widest mb-2">Ngày hết hạn</label>
              <input
                type="datetime-local"
                value={createForm.expiryDate}
                onChange={(e) => setCreateForm({ ...createForm, expiryDate: e.target.value })}
                className="h-11 w-full rounded-xl border border-zinc-200 bg-white/95 px-4 text-xs font-semibold text-zinc-500 outline-none transition-all duration-200 focus:border-zinc-950 focus:bg-white focus:ring-4 focus:ring-zinc-900/5"
              />
            </div>

            <div className="flex items-center gap-2 py-2 md:col-span-2">
              <input
                type="checkbox"
                id="create-active"
                checked={createForm.active}
                onChange={(e) => setCreateForm({ ...createForm, active: e.target.checked })}
                className="h-4 w-4 text-zinc-950 border-zinc-300 rounded focus:ring-zinc-950"
              />
              <label htmlFor="create-active" className="text-xs font-bold text-zinc-700 cursor-pointer select-none">
                Cho phép kích hoạt sử dụng ngay
              </label>
            </div>

            <div className="flex gap-2.5 items-end justify-end md:col-span-2">
              <button
                type="button"
                onClick={() => setShowCreateForm(false)}
                className="h-10 px-4 bg-zinc-100 hover:bg-zinc-200/80 border border-zinc-200/40 text-zinc-600 rounded-xl font-extrabold text-xs transition active:scale-95"
              >
                Hủy bỏ
              </button>
              <button
                type="submit"
                className="h-10 px-5 bg-zinc-950 hover:bg-zinc-800 text-white rounded-xl font-extrabold text-xs transition shadow-sm shadow-zinc-950/10 active:scale-95"
              >
                Tạo mã
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Stats Summary Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <div className="bg-white/80 border border-zinc-200/40 p-5 rounded-2xl shadow-sm flex items-center gap-4 transition hover:shadow-md">
          <div className="w-11 h-11 rounded-xl bg-zinc-950 text-white flex items-center justify-center shadow-md shadow-zinc-950/15">
            <FaTag className="w-4 h-4 text-amber-200" />
          </div>
          <div>
            <p className="text-zinc-400 text-[9px] font-black uppercase tracking-widest">Tổng số mã</p>
            <p className="text-2xl font-black text-zinc-950 mt-0.5">{coupons.length}</p>
          </div>
        </div>

        <div className="bg-white/80 border border-zinc-200/40 p-5 rounded-2xl shadow-sm flex items-center gap-4 transition hover:shadow-md">
          <div className="w-11 h-11 rounded-xl bg-emerald-50 border border-emerald-100 text-emerald-600 flex items-center justify-center shadow-sm">
            <FaCheckCircle className="w-4 h-4" />
          </div>
          <div>
            <p className="text-zinc-400 text-[9px] font-black uppercase tracking-widest">Đang hoạt động</p>
            <p className="text-2xl font-black text-emerald-600 mt-0.5">
              {coupons.filter((c) => c.active && !isExpired(c.expiryDate)).length}
            </p>
          </div>
        </div>

        <div className="bg-white/80 border border-zinc-200/40 p-5 rounded-2xl shadow-sm flex items-center gap-4 transition hover:shadow-md">
          <div className="w-11 h-11 rounded-xl bg-rose-50 border border-rose-100 text-rose-600 flex items-center justify-center shadow-sm">
            <FaHourglassEnd className="w-4 h-4" />
          </div>
          <div>
            <p className="text-zinc-400 text-[9px] font-black uppercase tracking-widest">Đã hết hạn</p>
            <p className="text-2xl font-black text-rose-600 mt-0.5">
              {coupons.filter((c) => isExpired(c.expiryDate)).length}
            </p>
          </div>
        </div>
      </div>

      {/* Coupon Table */}
      {isLoading ? (
        <div className="text-center py-12 flex flex-col justify-center items-center">
          <div className="w-8 h-8 border-2 border-zinc-950/20 border-t-zinc-950 rounded-full animate-spin mb-3"></div>
          <p className="text-zinc-400 text-xs font-bold uppercase tracking-wider animate-pulse">Đang tải dữ liệu mã giảm giá...</p>
        </div>
      ) : filteredCoupons.length === 0 ? (
        <div className="empty-state">
          <FaTag className="w-10 h-10 text-zinc-300 mb-3" />
          <p className="text-zinc-500 text-xs font-bold uppercase tracking-wider">Chưa thiết lập mã giảm giá nào</p>
        </div>
      ) : (
        <div className="table-shell">
          <table className="w-full text-xs text-left border-collapse">
            <thead>
              <tr className="table-head">
                <th className="py-4 px-4">Mã coupon</th>
                <th className="py-4 px-4 text-center">Phần trăm giảm</th>
                <th className="py-4 px-4 text-right">Giá trị tối thiểu</th>
                <th className="py-4 px-4 text-center">Hạn dùng</th>
                <th className="py-4 px-4 text-center">Trạng thái hoạt động</th>
                <th className="py-4 px-4 text-center">Hành động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100">
              {filteredCoupons.map((coupon) => {
                const isEditing = editingCode === coupon.code;
                
                return (
                  <tr key={coupon.code} className="hover:bg-zinc-50/40 transition-colors">
                    <td className="py-3 px-4">
                      <span className="inline-block bg-zinc-50 border border-zinc-200/60 text-zinc-800 px-3 py-1 rounded-lg font-mono font-extrabold text-sm uppercase tracking-wide">
                        {coupon.code}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-center">
                      {isEditing ? (
                        <input
                          type="number"
                          min="1"
                          max="100"
                          value={editForm.discountPercent}
                          onChange={(e) => setEditForm({ ...editForm, discountPercent: e.target.value })}
                          className="w-20 h-8 text-center bg-white border border-zinc-950 focus:ring-4 focus:ring-zinc-900/5 rounded-lg font-black text-zinc-950 outline-none"
                          placeholder="Giảm %"
                        />
                      ) : (
                        <span className="font-extrabold text-zinc-950 text-sm">{coupon.discountPercent}%</span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-right">
                      {isEditing ? (
                        <input
                          type="number"
                          min="0"
                          value={editForm.minimumOrderAmount}
                          onChange={(e) => setEditForm({ ...editForm, minimumOrderAmount: e.target.value })}
                          className="w-28 h-8 text-center bg-white border border-zinc-950 focus:ring-4 focus:ring-zinc-900/5 rounded-lg font-black text-zinc-950 outline-none"
                          placeholder="Tối thiểu"
                        />
                      ) : (
                        <span className="font-extrabold text-zinc-700">{formatPrice(coupon.minimumOrderAmount)}</span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-center">
                      {isEditing ? (
                        <input
                          type="datetime-local"
                          value={editForm.expiryDate}
                          onChange={(e) => setEditForm({ ...editForm, expiryDate: e.target.value })}
                          className="h-8 px-2 bg-white border border-zinc-200 focus:ring-4 focus:ring-zinc-900/5 rounded-lg text-xs font-semibold outline-none"
                        />
                      ) : (
                        <span className={`text-xs font-semibold ${isExpired(coupon.expiryDate) ? "text-rose-600 font-extrabold" : "text-zinc-500"}`}>
                          {formatDate(coupon.expiryDate)}
                        </span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-center">
                      {isEditing ? (
                        <label className="inline-flex items-center gap-1.5 cursor-pointer">
                          <input
                            type="checkbox"
                            checked={editForm.active}
                            onChange={(e) => setEditForm({ ...editForm, active: e.target.checked })}
                            className="w-4 h-4 text-zinc-950 border-zinc-300 rounded focus:ring-zinc-950"
                          />
                          <span className="text-xs font-bold text-zinc-700">Kích hoạt</span>
                        </label>
                      ) : (
                        <span className={`inline-block px-2.5 py-1 rounded-lg text-[9px] font-black uppercase tracking-widest ${
                          coupon.active && !isExpired(coupon.expiryDate)
                            ? "bg-emerald-50 text-emerald-700 border border-emerald-200/40"
                            : "bg-rose-50 text-rose-700 border-rose-200/40"
                        }`}>
                          {coupon.active && !isExpired(coupon.expiryDate) ? "Đang chạy" : "Hết hạn/Tắt"}
                        </span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-center">
                      {isEditing ? (
                        <div className="flex justify-center gap-1.5">
                          <button
                            onClick={() => handleEditSubmit(coupon.code)}
                            className="bg-zinc-950 hover:bg-zinc-800 text-white p-2 rounded-lg transition shadow-sm"
                            title="Lưu"
                          >
                            <FaCheck className="w-3 h-3" />
                          </button>
                          <button
                            onClick={handleCancel}
                            className="bg-zinc-100 hover:bg-zinc-200 text-zinc-600 p-2 rounded-lg transition border border-zinc-200/50"
                            title="Hủy"
                          >
                            <FaTimes className="w-3 h-3" />
                          </button>
                        </div>
                      ) : (
                        <div className="flex justify-center gap-2">
                          <button
                            onClick={() => handleEditStart(coupon)}
                            className="bg-white hover:bg-zinc-50 border border-zinc-200/60 text-zinc-600 p-2 rounded-lg transition flex items-center justify-center active:scale-95"
                            title="Chỉnh sửa mã"
                          >
                            <FaEdit className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => handleDelete(coupon.code)}
                            className="bg-white hover:bg-rose-50 border border-zinc-200/60 hover:border-rose-100 hover:text-rose-600 text-zinc-500 p-2 rounded-lg transition flex items-center justify-center active:scale-95"
                            title="Xóa mã"
                          >
                            <FaTrash className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      )}
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

export default CouponManagement;
