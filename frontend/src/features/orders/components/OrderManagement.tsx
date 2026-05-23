import { useEffect, useState } from "react";
import { orderService } from "../services/orderService";
import { inventoryService } from "@/features/inventory/services/inventoryService";
import { productService } from "@/features/products/services/productService";
import { OrderResponse } from "../types/order";
import { formatPrice } from "@/features/cart/utils/priceCalculation";
import { FaSync, FaChevronDown, FaCheck, FaTimes, FaCalendarAlt, FaClipboardList, FaUser, FaShieldAlt, FaExclamationTriangle } from "react-icons/fa";
import { toast } from "react-toastify";

const ORDER_STATUSES = [
  { value: "PENDING", label: "Chờ xác nhận", color: "bg-amber-50 text-amber-700 border-amber-200/40" },
  { value: "PROCESSING", label: "Đang xử lý", color: "bg-indigo-50 text-indigo-700 border-indigo-200/40" },
  { value: "SHIPPED", label: "Đã gửi", color: "bg-purple-50 text-purple-700 border-purple-200/40" },
  { value: "DELIVERED", label: "Đã giao", color: "bg-emerald-50 text-emerald-700 border-emerald-200/40" },
  { value: "CANCELLED", label: "Đã hủy", color: "bg-rose-50 text-rose-700 border-rose-200/40" },
];

const OrderManagement = () => {
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<string>("PENDING");
  const [expandedOrderId, setExpandedOrderId] = useState<string | null>(null);
  const [updatingOrderId, setUpdatingOrderId] = useState<string | null>(null);
  const [productMap, setProductMap] = useState<Record<string, { name: string; imageUrl?: string }>>({});

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        const products = await productService.getAllProducts();
        const map: Record<string, { name: string; imageUrl?: string }> = {};
        products.forEach((p) => {
          map[p.id] = { name: p.name, imageUrl: p.imageUrl };
        });
        setProductMap(map);
      } catch (err) {
        console.error("Failed to fetch products:", err);
      }
    };
    fetchProducts();
  }, []);

  const fetchOrders = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const allOrders = await orderService.getAllOrders();
      setOrders(allOrders);
    } catch (err) {
      setError((err as Error).message || "Không thể tải danh sách đơn hàng");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, []);

  const handleUpdateStatus = async (orderId: string, newStatus: string) => {
    setUpdatingOrderId(orderId);
    try {
      const updatedOrder = await orderService.updateOrderStatus(orderId, newStatus);
      
      setOrders(orders.map(o => o.id === orderId ? updatedOrder : o));
      
      if (newStatus === "SHIPPED" && updatedOrder.items) {
        try {
          await Promise.all(
            updatedOrder.items.map(item =>
              inventoryService.getInventoryDetails(item.productId)
            )
          );
        } catch (err) {
          console.warn("Failed to refresh inventory data:", err);
        }
      }
      
      toast.success("Cập nhật trạng thái đơn hàng thành công!");
    } catch (err) {
      toast.error("Lỗi cập nhật trạng thái: " + (err as Error).message);
    } finally {
      setUpdatingOrderId(null);
    }
  };

  let filteredOrders = orders;
  if (statusFilter !== "ALL") {
    filteredOrders = filteredOrders.filter(o => o.status === statusFilter);
  }

  const getStatusInfo = (status: string) => {
    return ORDER_STATUSES.find(s => s.value === status) || ORDER_STATUSES[0];
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("vi-VN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const getNextStatuses = (currentStatus: string): string[] => {
    const statusFlow: { [key: string]: string[] } = {
      PENDING: ["PROCESSING", "CANCELLED"],
      PROCESSING: ["SHIPPED", "CANCELLED"],
      SHIPPED: ["DELIVERED"],
      DELIVERED: [],
      CANCELLED: [],
    };
    return statusFlow[currentStatus] || [];
  };

  return (
    <div className="surface p-6 sm:p-8 bg-white/70 backdrop-blur-md border border-zinc-200/50 rounded-2xl space-y-6 sm:space-y-8">
      {/* Header section */}
      <div className="border-b border-zinc-100 pb-5">
        <h2 className="text-lg font-extrabold text-zinc-950">Quản lý Đơn hàng</h2>
        <p className="text-xs font-semibold text-zinc-400 mt-1">Xác nhận đơn hàng, cập nhật lộ trình vận chuyển và quản lý các trạng thái đơn hàng.</p>
      </div>

      {error && (
        <div className="bg-rose-50/50 border border-rose-200/40 text-rose-700 px-4 py-3 rounded-xl text-xs font-semibold flex items-center gap-2">
          <FaExclamationTriangle className="flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Controls */}
      <div className="flex flex-col sm:flex-row gap-4">
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="h-11 px-4 bg-zinc-50 border border-zinc-200/60 focus:bg-white focus:border-zinc-950 focus:ring-4 focus:ring-zinc-900/5 rounded-xl text-xs outline-none transition duration-200 text-zinc-500 font-extrabold uppercase tracking-wider w-full sm:w-64"
        >
          <option value="ALL">Tất cả trạng thái</option>
          <option value="PENDING">Chờ xác nhận</option>
          <option value="PROCESSING">Đang xử lý</option>
          <option value="SHIPPED">Đã gửi</option>
          <option value="DELIVERED">Đã giao</option>
          <option value="CANCELLED">Đã hủy</option>
        </select>

        <button
          onClick={fetchOrders}
          disabled={isLoading}
          className="h-11 flex items-center justify-center gap-2 px-5 bg-zinc-950 text-white rounded-xl hover:bg-zinc-800 disabled:bg-zinc-100 disabled:text-zinc-400 font-extrabold text-xs uppercase tracking-wider transition-all duration-300 w-full sm:w-auto active:scale-95"
        >
          <FaSync className={`w-3 h-3 ${isLoading ? "animate-spin" : ""}`} />
          <span>Làm mới</span>
        </button>
      </div>

      {/* Stats Summary Metrics Grid */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
        <div className="bg-white/80 border border-zinc-200/40 p-4 rounded-2xl shadow-sm transition hover:shadow-md">
          <p className="text-zinc-400 text-[9px] font-black uppercase tracking-widest">Tất cả</p>
          <p className="text-xl font-black text-zinc-950 mt-1">{orders.length}</p>
        </div>
        <div className="bg-white/80 border border-zinc-200/40 p-4 rounded-2xl shadow-sm transition hover:shadow-md">
          <p className="text-amber-500 text-[9px] font-black uppercase tracking-widest">Chờ xác nhận</p>
          <p className="text-xl font-black text-amber-600 mt-1">
            {orders.filter(o => o.status === "PENDING").length}
          </p>
        </div>
        <div className="bg-white/80 border border-zinc-200/40 p-4 rounded-2xl shadow-sm transition hover:shadow-md">
          <p className="text-indigo-500 text-[9px] font-black uppercase tracking-widest">Đang xử lý</p>
          <p className="text-xl font-black text-indigo-600 mt-1">
            {orders.filter(o => o.status === "PROCESSING").length}
          </p>
        </div>
        <div className="bg-white/80 border border-zinc-200/40 p-4 rounded-2xl shadow-sm transition hover:shadow-md">
          <p className="text-emerald-500 text-[9px] font-black uppercase tracking-widest">Đã giao</p>
          <p className="text-xl font-black text-emerald-600 mt-1">
            {orders.filter(o => o.status === "DELIVERED").length}
          </p>
        </div>
        <div className="bg-white/80 border border-zinc-200/40 p-4 rounded-2xl shadow-sm transition hover:shadow-md">
          <p className="text-rose-500 text-[9px] font-black uppercase tracking-widest">Đã hủy</p>
          <p className="text-xl font-black text-rose-600 mt-1">
            {orders.filter(o => o.status === "CANCELLED").length}
          </p>
        </div>
      </div>

      {/* Orders List */}
      {isLoading ? (
        <div className="text-center py-12 flex flex-col justify-center items-center">
          <div className="w-8 h-8 border-2 border-zinc-950/20 border-t-zinc-950 rounded-full animate-spin mb-3"></div>
          <p className="text-zinc-400 text-xs font-bold uppercase tracking-wider animate-pulse">Đang tải dữ liệu đơn hàng...</p>
        </div>
      ) : filteredOrders.length === 0 ? (
        <div className="empty-state">
          <FaClipboardList className="w-10 h-10 text-zinc-300 mb-3" />
          <p className="text-zinc-500 text-xs font-bold uppercase tracking-wider">Không có đơn hàng nào ở trạng thái này</p>
        </div>
      ) : (
        <div className="space-y-4">
          {filteredOrders.map((order) => {
            const isExpanded = expandedOrderId === order.id;
            const nextStatuses = getNextStatuses(order.status);
            
            return (
              <div
                key={order.id}
                className="border border-zinc-200/50 rounded-2xl overflow-hidden shadow-sm hover:shadow-md transition-all duration-300 bg-white/60 backdrop-blur-md"
              >
                {/* Order Card Summary */}
                <div
                  className="p-5 cursor-pointer hover:bg-zinc-50/50 transition flex flex-col md:flex-row md:items-center justify-between gap-4"
                  onClick={() => setExpandedOrderId(isExpanded ? null : order.id)}
                >
                  <div className="flex-1 flex flex-col sm:flex-row sm:items-center gap-4 sm:gap-8">
                    <div>
                      <p className="font-mono font-bold text-zinc-950 text-base">
                        Đơn #{order.id.substring(0, 8).toUpperCase()}
                      </p>
                      <p className="text-xs text-zinc-400 mt-1 flex items-center gap-1.5 font-semibold">
                        <FaUser className="text-zinc-300" />
                        <span>Khách: {order.userId}</span>
                      </p>
                    </div>

                    <div className="sm:border-l sm:border-zinc-200/60 sm:pl-8 py-1">
                      <p className="text-zinc-400 text-[9px] font-black uppercase tracking-widest">Tổng tiền</p>
                      <p className="font-black text-zinc-950 text-lg mt-0.5">
                        {formatPrice(order.totalPrice)}
                      </p>
                    </div>

                    <div className="sm:border-l sm:border-zinc-200/60 sm:pl-8 py-1">
                      <p className="text-zinc-400 text-[9px] font-black uppercase tracking-widest">Ngày đặt hàng</p>
                      <p className="text-zinc-700 text-sm font-bold mt-0.5 flex items-center gap-1.5">
                        <FaCalendarAlt className="text-zinc-400" />
                        <span>{formatDate(order.createdAt)}</span>
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-4 ml-auto md:ml-0">
                    <span className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-[10px] font-black uppercase tracking-widest border ${
                      getStatusInfo(order.status).color
                    }`}>
                      {getStatusInfo(order.status).label}
                    </span>
                    <FaChevronDown
                      className={`text-zinc-400 transition-transform duration-200 ${
                        isExpanded ? "rotate-180" : ""
                      }`}
                    />
                  </div>
                </div>

                {/* Order Details Drawer */}
                {isExpanded && (
                  <div className="p-6 border-t border-zinc-100 bg-zinc-50/20 space-y-6 animate-slide-down">
                    
                    {/* Shipping Info Card */}
                    <div className="bg-white/80 border border-zinc-200/40 rounded-2xl p-5 shadow-sm grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div>
                        <h4 className="text-[10px] font-black text-zinc-400 uppercase tracking-widest mb-2.5">Thông tin giao nhận</h4>
                        <p className="text-xs font-semibold text-zinc-700">{order.shippingAddress}</p>
                        <p className="text-[11px] text-zinc-400 font-medium mt-1">
                          {order.ward}, {order.district}, {order.city}
                        </p>
                      </div>
                      <div>
                        <h4 className="text-[10px] font-black text-zinc-400 uppercase tracking-widest mb-2.5">Liên hệ khách hàng</h4>
                        <p className="text-xs font-bold text-zinc-800">{order.phoneNumber || "Không có sđt"}</p>
                        {order.couponCode && (
                          <p className="text-[10px] text-emerald-600 font-black mt-2 bg-emerald-50 border border-emerald-100/50 px-2.5 py-1 rounded-lg inline-block">
                            Mã giảm: {order.couponCode}
                          </p>
                        )}
                      </div>
                    </div>

                    {/* Items Details Table */}
                    <div className="table-shell overflow-hidden">
                      <table className="w-full text-xs text-left border-collapse">
                        <thead>
                          <tr className="table-head border-b border-zinc-100">
                            <th className="py-3 px-4 font-bold text-zinc-400 text-xs uppercase tracking-wider">Sản phẩm</th>
                            <th className="py-3 px-4 font-bold text-zinc-400 text-xs uppercase tracking-wider text-center">Số lượng</th>
                            <th className="py-3 px-4 font-bold text-zinc-400 text-xs uppercase tracking-wider text-right">Đơn giá</th>
                            <th className="py-3 px-4 font-bold text-zinc-400 text-xs uppercase tracking-wider text-right">Thành tiền</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-zinc-100">
                           {order.items.map((item) => (
                            <tr key={item.id} className="hover:bg-zinc-50/20 transition-colors">
                              <td className="py-3 px-4 font-bold text-zinc-800 flex items-center gap-3">
                                {/* Small Product Image inside Table cell */}
                                <div className="flex h-9 w-9 flex-shrink-0 items-center justify-center overflow-hidden rounded-lg border border-zinc-200/50 bg-zinc-50">
                                  {productMap[item.productId]?.imageUrl ? (
                                    <img
                                      src={productMap[item.productId].imageUrl}
                                      alt={productMap[item.productId].name || item.name}
                                      className="h-full w-full object-cover"
                                      onError={(e) => {
                                        e.currentTarget.style.display = "none";
                                        const fallback = e.currentTarget.nextElementSibling as HTMLElement | null;
                                        if (fallback) fallback.style.display = "block";
                                      }}
                                    />
                                  ) : null}
                                  <span
                                    className={`px-0.5 text-center text-[7px] font-bold uppercase tracking-wider text-zinc-400 ${
                                      productMap[item.productId]?.imageUrl ? "hidden" : "block"
                                    }`}
                                  >
                                    No Img
                                  </span>
                                </div>
                                <span className="truncate max-w-[180px] sm:max-w-none">
                                  {productMap[item.productId]?.name || item.name || item.productId}
                                </span>
                              </td>
                              <td className="py-3.5 px-4 text-center font-black text-zinc-600">{item.quantity}</td>
                              <td className="py-3.5 px-4 text-right font-bold text-zinc-500">{formatPrice(item.price)}</td>
                              <td className="py-3.5 px-4 text-right font-black text-zinc-950">
                                {formatPrice(item.price * item.quantity)}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>

                    {/* Financial breakdown */}
                    <div className="bg-white/80 border border-zinc-200/40 rounded-2xl p-5 shadow-sm max-w-sm ml-auto space-y-2.5">
                      <div className="flex justify-between text-xs text-zinc-500 font-semibold">
                        <span>Tạm tính:</span>
                        <span>{formatPrice(order.totalPrice - order.shippingFee)}</span>
                      </div>
                      <div className="flex justify-between text-xs text-zinc-500 font-semibold">
                        <span>Phí vận chuyển:</span>
                        <span>{formatPrice(order.shippingFee)}</span>
                      </div>
                      {order.discountAmount > 0 && (
                        <div className="flex justify-between text-xs text-emerald-600 font-bold">
                          <span>Giảm giá:</span>
                          <span>-{formatPrice(order.discountAmount)}</span>
                        </div>
                      )}
                      <div className="flex justify-between text-xs text-zinc-500 font-semibold border-t border-zinc-150 pt-2.5 mt-2.5">
                        <span>Tổng cộng:</span>
                        <span className="text-sm font-black text-zinc-950">{formatPrice(order.totalPrice)}</span>
                      </div>
                    </div>

                    {/* Actions */}
                    <div className="flex flex-wrap gap-2.5 items-center justify-end border-t border-zinc-100 pt-4">
                      {nextStatuses.length > 0 ? (
                        <>
                          {nextStatuses.map((nextStatus) => (
                            <button
                              key={nextStatus}
                              onClick={() => handleUpdateStatus(order.id, nextStatus)}
                              disabled={updatingOrderId === order.id}
                              className={`inline-flex h-9 items-center justify-center gap-1.5 rounded-lg px-4 text-xs font-bold tracking-wide transition-all duration-200 active:scale-[0.98] border border-transparent shadow-sm hover:opacity-90 disabled:opacity-50 ${
                                getStatusInfo(nextStatus).color
                              }`}
                            >
                              <FaCheck size={10} />
                              <span>{getStatusInfo(nextStatus).label}</span>
                            </button>
                          ))}

                          {order.status !== "CANCELLED" && (
                            <button
                              onClick={() => handleUpdateStatus(order.id, "CANCELLED")}
                              disabled={updatingOrderId === order.id}
                              className="inline-flex h-9 items-center justify-center gap-1.5 rounded-lg px-4 text-xs font-bold tracking-wide bg-rose-50 text-rose-700 hover:bg-rose-100 border border-rose-200/40 transition duration-200 disabled:opacity-50"
                            >
                              <FaTimes size={10} />
                              <span>Hủy đơn hàng</span>
                            </button>
                          )}
                        </>
                      ) : (
                        <div className="text-zinc-400 text-xs font-bold flex items-center gap-1.5 bg-zinc-100/80 px-3.5 py-2.5 rounded-xl border border-zinc-200/30">
                          <FaShieldAlt className="text-zinc-400" />
                          <span>
                            {order.status === "DELIVERED"
                              ? "Đơn hàng đã giao thành công và lưu trữ"
                              : "Đơn hàng này đã kết thúc lộ trình"}
                          </span>
                        </div>
                      )}
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default OrderManagement;

