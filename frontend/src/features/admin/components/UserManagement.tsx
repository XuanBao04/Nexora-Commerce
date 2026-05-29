import { useEffect, useState } from "react";
import { adminApiService } from "../services/adminApiService";
import { useAdminPagination } from "../hooks/useAdminPagination";
import { AdminPagination } from "@/features/admin/components/AdminPagination";
import { FaSync, FaSearch, FaUser, FaUserShield, FaUserTimes, FaExclamationTriangle} from "react-icons/fa";

const UserManagement = () => {
  const [users, setUsers] = useState<any[]>([]);
  const [searchTerm, setSearchTerm] = useState("");
  
  const { 
    page, 
    size, 
    pagination, 
    setPage, 
    updatePaginationData, 
    isLoading, 
    setIsLoading, 
    error, 
    setError 
  } = useAdminPagination(10);

  const fetchUsers = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await adminApiService.getAllUsers(page, size, searchTerm);
      setUsers(response.items);
      updatePaginationData(response.pagination as any);
    } catch (err) {
      setError((err as Error).message || "Không thể tải danh sách người dùng");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, [page, size]);

  const formatDate = (dateString: string) => {
    if (!dateString) return "N/A";
    return new Date(dateString).toLocaleDateString("vi-VN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  return (
    <div className="surface p-6 sm:p-8 bg-white/70 backdrop-blur-md border border-zinc-200/50 rounded-2xl space-y-6 sm:space-y-8">
      <div className="border-b border-zinc-100 pb-5">
        <h2 className="text-lg font-extrabold text-zinc-950">Quản lý Người dùng</h2>
        <p className="text-xs font-semibold text-zinc-400 mt-1">
          Xem danh sách người dùng, vai trò và trạng thái hoạt động trên hệ thống.
        </p>
      </div>

      {error && (
        <div className="bg-rose-50/50 border border-rose-200/40 text-rose-700 px-4 py-3 rounded-xl text-xs font-semibold flex items-center gap-2">
          <FaExclamationTriangle className="flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Controls */}
      <div className="flex flex-col sm:flex-row gap-4 justify-between">
        <div className="relative w-full sm:w-96 flex-1">
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-zinc-400">
            <FaSearch className="w-3.5 h-3.5" />
          </div>
          <input
            type="text"
            placeholder="Tìm kiếm theo username hoặc email..."
            className="h-11 w-full pl-10 pr-4 bg-zinc-50 border border-zinc-200/60 focus:bg-white focus:border-zinc-950 focus:ring-4 focus:ring-zinc-900/5 rounded-xl text-xs outline-none transition duration-200 text-zinc-700 font-medium placeholder:text-zinc-400"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                setPage(0);
                fetchUsers();
              }
            }}
          />
        </div>

        <button
          onClick={() => {
            setPage(0);
            fetchUsers();
          }}
          disabled={isLoading}
          className="h-11 flex items-center justify-center gap-2 px-5 bg-zinc-950 text-white rounded-xl hover:bg-zinc-800 disabled:bg-zinc-100 disabled:text-zinc-400 font-extrabold text-xs uppercase tracking-wider transition-all duration-300 active:scale-95 whitespace-nowrap"
        >
          <FaSync className={`w-3 h-3 ${isLoading ? "animate-spin" : ""}`} />
          <span>Làm mới</span>
        </button>
      </div>

      {/* Users Table */}
      {isLoading ? (
        <div className="text-center py-12 flex flex-col justify-center items-center">
          <div className="w-8 h-8 border-2 border-zinc-950/20 border-t-zinc-950 rounded-full animate-spin mb-3"></div>
          <p className="text-zinc-400 text-xs font-bold uppercase tracking-wider animate-pulse">Đang tải dữ liệu...</p>
        </div>
      ) : users.length === 0 ? (
        <div className="text-center py-12 bg-zinc-50/50 rounded-2xl border border-dashed border-zinc-200 p-8 flex flex-col items-center justify-center">
          <FaUserTimes className="w-10 h-10 text-zinc-300 mb-3" />
          <p className="text-zinc-500 text-xs font-bold uppercase tracking-wider">Không tìm thấy người dùng</p>
        </div>
      ) : (
        <div className="table-shell overflow-x-auto">
          <table className="w-full text-xs text-left border-collapse">
            <thead>
              <tr className="table-head">
                <th className="py-4 px-4">Tài khoản</th>
                <th className="py-4 px-4">Thông tin cá nhân</th>
                <th className="py-4 px-4 text-center">Quyền hạn</th>
                <th className="py-4 px-4 text-center">Trạng thái</th>
                <th className="py-4 px-4 text-right">Ngày đăng ký</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100">
              {users.map((user) => (
                <tr key={user.id} className="hover:bg-zinc-50/40 transition-colors">
                  <td className="py-3 px-4">
                    <div className="font-bold text-zinc-900">{user.username}</div>
                    <div className="text-zinc-500 text-[10px] mt-0.5">{user.email}</div>
                  </td>
                  <td className="py-3 px-4">
                    <div className="text-zinc-800 font-semibold">{user.fullName || "Chưa cập nhật"}</div>
                    {user.phoneNumber && <div className="text-zinc-500 text-[10px] mt-0.5">{user.phoneNumber}</div>}
                  </td>
                  <td className="py-3 px-4 text-center">
                    <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[9px] font-black uppercase tracking-widest border ${
                      user.role === "ROLE_ADMIN" || user.role === "ADMIN"
                        ? "bg-indigo-50 text-indigo-700 border-indigo-200/40"
                        : "bg-zinc-100 text-zinc-600 border-zinc-200/40"
                    }`}>
                      {user.role === "ROLE_ADMIN" || user.role === "ADMIN" ? <FaUserShield className="w-2.5 h-2.5" /> : <FaUser className="w-2.5 h-2.5" />}
                      {user.role === "ROLE_ADMIN" || user.role === "ADMIN" ? "Quản trị" : "Khách"}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-center">
                    <span className={`inline-block px-2.5 py-1 rounded-lg text-[9px] font-black uppercase tracking-widest border ${
                      user.active
                        ? "bg-emerald-50 text-emerald-700 border-emerald-200/40"
                        : "bg-rose-50 text-rose-700 border-rose-200/40"
                    }`}>
                      {user.active ? "Đang hoạt động" : "Bị khóa"}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-right text-zinc-500 font-medium">
                    {formatDate(user.createdAt)}
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

export default UserManagement;
