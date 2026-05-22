import { useEffect, useState } from "react";
import { orderService } from "../../services/api/orderService";
import { productService } from "../../services/api/productService";
import { OrderResponse } from "../../types/order";
import PriceBreakdown from "../Cart/PriceBreakdown";
import { formatPrice } from "../../utils/priceCalculation";
import { FaChevronLeft, FaChevronRight, FaChevronDown, FaClipboardList, FaRegCalendarAlt, FaShippingFast, FaCheckCircle, FaExclamationTriangle } from "react-icons/fa";

const getStatusBadgeClass = (status: OrderResponse["status"]) => {
  switch (status) {
    case "PENDING":
      return "bg-amber-50 text-amber-700 border-amber-200/40";
    case "PROCESSING":
      return "bg-indigo-50 text-indigo-700 border-indigo-200/40";
    case "SHIPPED":
      return "bg-blue-50 text-blue-700 border-blue-200/40";
    case "DELIVERED":
      return "bg-emerald-50 text-emerald-700 border-emerald-200/40";
    case "CANCELLED":
      return "bg-zinc-50 text-zinc-500 border-zinc-200/50";
    default:
      return "bg-zinc-50 text-zinc-500 border-zinc-200/50";
  }
};

const getStatusLabel = (status: OrderResponse["status"]) => {
  switch (status) {
    case "PENDING":
      return "Chờ xác nhận";
    case "PROCESSING":
      return "Đang xử lý";
    case "SHIPPED":
      return "Đang giao hàng";
    case "DELIVERED":
      return "Đã giao thành công";
    case "CANCELLED":
      return "Đã hủy đơn";
    default:
      return status;
  }
};

export default function Order() {
  const userId = localStorage.getItem("userId") || "";
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [cancelingOrderId, setCancelingOrderId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [expandedOrderId, setExpandedOrderId] = useState<string | null>(null);
  const [productMap, setProductMap] = useState<Record<string, { name: string; imageUrl?: string }>>({});

  // Pagination State
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const pageSize = 5;

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

  useEffect(() => {
    if (!userId) return;

    const fetchOrders = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const response = await orderService.getUserOrdersPaginated(userId, currentPage, pageSize, 'createdAt');
        setOrders(response.items);
        setTotalPages(response.pagination.totalPages);
        setTotalElements(response.pagination.totalElements);
      } catch (err: Error | unknown) {
        const errorMessage = err instanceof Error ? err.message : "Không thể tải đơn hàng.";
        setError(errorMessage);
      } finally {
        setIsLoading(false);
      }
    };

    fetchOrders();
  }, [userId, currentPage]);

  const handleCancelOrder = async (orderId: string) => {
    setCancelingOrderId(orderId);
    setError(null);

    try {
      const updatedOrder = await orderService.cancelOrder(orderId);
      setOrders((prev) =>
        prev.map((order) => (order.id === orderId ? updatedOrder : order)),
      );
    } catch (err: Error | unknown) {
      const errorMessage = err instanceof Error ? err.message : "Hủy đơn hàng thất bại.";
      setError(errorMessage);
    } finally {
      setCancelingOrderId(null);
    }
  };

  const handlePageChange = (page: number) => {
    if (page >= 0 && page < totalPages) {
      setCurrentPage(page);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-4 animate-fade-in">
        <div className="sr-only">Đang tải đơn hàng...</div>
        <div className="skeleton h-20 w-full" />
        <div className="skeleton h-28 w-full" />
        <div className="skeleton h-28 w-full" />
        <div className="skeleton h-28 w-full" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="empty-state border-rose-200/60 bg-rose-50/50 p-6 text-rose-700">
        <FaExclamationTriangle className="mb-3 h-8 w-8 text-rose-500" />
        <p className="font-extrabold text-sm uppercase tracking-wider">Đã xảy ra lỗi: {error}</p>
      </div>
    );
  }

  if (orders.length === 0) {
    return (
      <div className="empty-state bg-white/40 border border-dashed border-zinc-200 p-12 text-center">
        <span className="sr-only">Bạn chưa có đơn hàng nào</span>
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-3xl bg-zinc-100 text-zinc-400 mb-6">
          <FaClipboardList className="h-6 w-6" />
        </div>
        <h1 className="text-xl font-extrabold text-zinc-950">Lịch sử đơn hàng trống</h1>
        <p className="mt-2 max-w-sm text-sm font-semibold leading-relaxed text-zinc-400">
          Bạn chưa thực hiện bất kỳ giao dịch mua sắm nào cùng Nexora Commerce.
        </p>
        <a
          href="/authenticated/products"
          className="btn-primary mt-8 inline-flex px-6 h-12 rounded-xl text-xs uppercase tracking-wider font-bold"
        >
          Mua sắm ngay
        </a>
      </div>
    );
  }

  const pageNumbers = [];
  for (let i = 0; i < totalPages; i++) {
    pageNumbers.push(i);
  }

  const startElement = currentPage * pageSize + 1;
  const endElement = Math.min((currentPage + 1) * pageSize, totalElements);

  return (
    <div className="animate-fade-in space-y-8 lg:space-y-10">
      <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
        <div className="space-y-2">
          <div className="inline-flex items-center gap-1.5 rounded-full border border-indigo-200/50 bg-indigo-50/60 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-indigo-700">
            Aetheris Orders
          </div>
          <h1 className="page-heading">Lịch sử giao dịch</h1>
          <p className="page-subtitle">
            Hiển thị {startElement} - {endElement} trên tổng số {totalElements} hóa đơn đặt mua sản phẩm của bạn.
          </p>
        </div>
      </div>

      {/* Orders List Layout */}
      <div className="space-y-5" data-testid="orders-list">
        {orders.map((order) => {
          const isPending = order.status === "PENDING";
          const isCancelling = cancelingOrderId === order.id;
          const isExpanded = expandedOrderId === order.id;

          return (
            <div
              key={order.id}
              className="surface overflow-hidden transition-all duration-300 hover:border-zinc-300"
              data-testid="order-item"
            >
              {/* Order Header Summary Row */}
              <div
                className="flex cursor-pointer items-center justify-between gap-4 bg-white/40 p-6 transition hover:bg-zinc-50/60"
                onClick={() =>
                  setExpandedOrderId(isExpanded ? null : order.id)
                }
              >
                <div className="flex-1 space-y-1">
                  <h2 className="text-sm font-extrabold text-zinc-950 sm:text-base">
                    Đơn hàng <span className="font-mono text-xs text-zinc-400 font-bold uppercase tracking-widest ml-1">#{order.id.substring(0, 8).toUpperCase()}</span>
                  </h2>
                  <p className="text-[10px] font-bold text-zinc-400 uppercase tracking-widest flex items-center gap-1.5">
                    <FaRegCalendarAlt className="h-3 w-3" />
                    <span>
                      {order.createdAt
                        ? new Date(order.createdAt).toLocaleDateString(
                            "vi-VN",
                            {
                              year: "numeric",
                              month: "2-digit",
                              day: "2-digit",
                              hour: "2-digit",
                              minute: "2-digit",
                            }
                          )
                        : "Không xác định"}
                    </span>
                  </p>
                </div>

                <div className="flex items-center gap-4 sm:gap-8">
                  <div className="text-right space-y-1">
                    <p className="text-base font-black text-zinc-950 sm:text-lg">
                      {formatPrice(order.totalPrice)}
                    </p>
                    <span
                      className={`badge text-[9px] uppercase tracking-widest font-black ${getStatusBadgeClass(
                        order.status
                      )}`}
                    >
                      {getStatusLabel(order.status)}
                      <span className="sr-only">{order.status}</span>
                    </span>
                  </div>

                  <div className={`text-zinc-400 transition-transform duration-300 ${isExpanded ? 'rotate-180 text-zinc-950' : ''}`}>
                    <FaChevronDown className="w-3.5 h-3.5" />
                  </div>
                </div>
              </div>

              {/* Order Details Drawer Container */}
              {isExpanded && (
                <div className="space-y-6 border-t border-zinc-100 bg-zinc-50/40 p-6 animate-slide-down">
                  
                  {/* Coupon Indicator Banner */}
                  {order.couponCode && (
                    <div className="inline-flex items-center gap-1.5 rounded-full border border-emerald-200/50 bg-emerald-50/60 px-3.5 py-1 text-[10px] font-black uppercase tracking-widest text-emerald-700">
                      Khuyến mãi đã dùng: {order.couponCode}
                      <span className="sr-only">Mã giảm giá</span>
                    </div>
                  )}

                  {/* Delivery Detail Grid */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="bg-white/80 p-5 rounded-2xl border border-zinc-200/40 shadow-sm space-y-2">
                      <h4 className="text-[10px] font-bold text-zinc-400 uppercase tracking-widest flex items-center gap-1.5">
                        <FaShippingFast className="h-3.5 w-3.5" />
                        <span>Thông tin giao nhận</span>
                      </h4>
                      <p className="text-sm font-semibold text-zinc-800">{order.shippingAddress}</p>
                      <p className="text-xs text-zinc-400 font-semibold">
                        Phường {order.ward}, Quận {order.district}, {order.city}
                      </p>
                    </div>

                    <div className="bg-white/80 p-5 rounded-2xl border border-zinc-200/40 shadow-sm space-y-2">
                      <h4 className="text-[10px] font-bold text-zinc-400 uppercase tracking-widest flex items-center gap-1.5">
                        <FaCheckCircle className="h-3.5 w-3.5" />
                        <span>Liên hệ đặt mua</span>
                      </h4>
                      <p className="text-sm font-extrabold text-zinc-800">{order.phoneNumber || "Không cung cấp số điện thoại"}</p>
                      <p className="text-xs text-zinc-400 font-semibold uppercase tracking-widest">
                        Khách hàng ID: {order.userId.substring(0, 8)}
                      </p>
                    </div>
                  </div>

                  {/* Items List detail */}
                  <div className="surface overflow-hidden bg-white/60">
                    <div className="border-b border-zinc-100 p-4 bg-zinc-50/50">
                      <h3 className="text-xs font-bold text-zinc-400 uppercase tracking-widest">Danh mục sản phẩm đã mua</h3>
                    </div>
                    <ul className="divide-y divide-zinc-100 p-4 space-y-4">
                      {order.items.map((item) => {
                        const product = productMap[item.productId];
                        return (
                          <li
                            key={item.id}
                            className="flex items-center gap-4 py-2 text-xs font-semibold text-zinc-500"
                          >
                            {/* Product Image */}
                            <div className="flex h-12 w-12 flex-shrink-0 items-center justify-center overflow-hidden rounded-lg border border-zinc-200/50 bg-zinc-50 transition hover:border-zinc-300">
                              {product?.imageUrl ? (
                                <img
                                  src={product.imageUrl}
                                  alt={product.name || item.name}
                                  className="h-full w-full object-cover"
                                  onError={(e) => {
                                    e.currentTarget.style.display = "none";
                                    const fallback = e.currentTarget.nextElementSibling as HTMLElement | null;
                                    if (fallback) fallback.style.display = "block";
                                  }}
                                />
                              ) : null}
                              <span
                                className={`px-1 text-center text-[8px] font-bold uppercase tracking-wider text-zinc-400 ${
                                  product?.imageUrl ? "hidden" : "block"
                                }`}
                              >
                                No Image
                              </span>
                            </div>

                            {/* Details Area */}
                            <div className="flex-1 min-w-0">
                              <h4 className="text-zinc-800 font-extrabold text-xs truncate">
                                {product?.name || item.name || "Sản phẩm Aetheris"}
                              </h4>
                              <p className="text-[10px] text-zinc-400 font-bold uppercase tracking-widest mt-1 flex items-center gap-1">
                                <span>Đơn giá: {formatPrice(item.price)}</span>
                                <span className="text-zinc-200 font-normal">|</span>
                                <span>Số lượng: {item.quantity}</span>
                                <span className="sr-only">{item.productId} - {item.quantity} x</span>
                              </p>
                            </div>

                            {/* Subtotal */}
                            <div className="text-right flex-shrink-0">
                              <span className="font-extrabold text-zinc-950 block">
                                {formatPrice(item.price * item.quantity)}
                              </span>
                            </div>
                          </li>
                        );
                      })}
                    </ul>
                  </div>

                  {/* Financial calculation receipts */}
                  <div className="bg-white/60 p-5 rounded-2xl border border-zinc-200/40 shadow-sm max-w-sm ml-auto">
                    <PriceBreakdown
                      subtotal={order.subtotal}
                      discountAmount={order.discountAmount}
                      couponCode={order.couponCode}
                      shippingFee={order.shippingFee}
                      totalPrice={order.totalPrice}
                      compact={true}
                    />
                  </div>

                  {/* Actions Bar */}
                  <div className="flex justify-end gap-2 border-t border-zinc-100 pt-5">
                    <button
                      onClick={() => handleCancelOrder(order.id)}
                      disabled={!isPending || isCancelling}
                      className="btn-danger h-10 px-5 text-xs font-bold uppercase tracking-wider disabled:bg-zinc-100 disabled:border-zinc-200/50 disabled:text-zinc-400 disabled:shadow-none"
                    >
                      {isCancelling
                        ? "Đang xử lý..."
                        : !isPending
                          ? "Không thể hủy đơn"
                          : "Hủy đơn đặt hàng"}
                    </button>
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Pagination Controls */}
      {totalPages > 1 && (
        <div className="flex flex-col items-center justify-center gap-4 border-t border-zinc-200/50 pt-10 sm:flex-row">
          <div className="flex items-center gap-1.5 rounded-2xl bg-white/80 p-1.5 shadow-sm border border-zinc-200/50 backdrop-blur-sm">
            <button
              onClick={() => handlePageChange(currentPage - 1)}
              disabled={currentPage === 0}
              className="icon-btn border-0 shadow-none hover:bg-zinc-100 disabled:opacity-30"
              title="Trang trước"
            >
              <FaChevronLeft className="h-3.5 w-3.5" />
            </button>

            {pageNumbers.map((page) => (
              <button
                key={page}
                onClick={() => handlePageChange(page)}
                className={`h-10 w-10 rounded-xl text-xs font-bold transition-all duration-200 ${
                  currentPage === page
                    ? 'bg-zinc-950 text-white shadow-md'
                    : 'text-zinc-600 hover:bg-zinc-100 hover:text-zinc-950'
                }`}
              >
                {page + 1}
              </button>
            ))}

            <button
              onClick={() => handlePageChange(currentPage + 1)}
              disabled={currentPage === totalPages - 1}
              className="icon-btn border-0 shadow-none hover:bg-zinc-100 disabled:opacity-30"
              title="Trang sau"
            >
              <FaChevronRight className="h-3.5 w-3.5" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
