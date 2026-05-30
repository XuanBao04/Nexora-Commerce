import { useEffect, useState } from "react";
import {
  DollarSign,
  ShoppingCart,
  Users,
  Package,
  TrendingUp,
  Crown,
  CalendarDays,
  Clock,
} from "lucide-react";
import { 
  adminStatisticService, 
  AdminDashboardSummary, 
  RevenueChartItem, 
  BestSellerItem, 
  OrderStatusStat 
} from "../services/adminStatisticService";
import { formatPrice } from "@/features/cart/utils/priceCalculation";

const AdminStatistics = () => {
  const [summary, setSummary] = useState<AdminDashboardSummary | null>(null);
  const [revenueDays, setRevenueDays] = useState<number>(7);
  const [chartData, setChartData] = useState<RevenueChartItem[]>([]);
  const [bestSellers, setBestSellers] = useState<BestSellerItem[]>([]);
  const [orderStatusStats, setOrderStatusStats] = useState<OrderStatusStat[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [hoveredPoint, setHoveredPoint] = useState<{ x: number; y: number; date: string; revenue: number } | null>(null);

  const fetchStats = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const [sumRes, bestRes, statusRes] = await Promise.all([
        adminStatisticService.getDashboardSummary(),
        adminStatisticService.getBestSellers(5),
        adminStatisticService.getOrderStatusStats(),
      ]);

      setSummary(sumRes);
      setBestSellers(bestRes);
      setOrderStatusStats(statusRes);
    } catch (err: any) {
      console.error("Failed to load statistics summary", err);
      setError("Không thể tải thông tin thống kê. Vui lòng thử lại sau.");
    } finally {
      setLoading(false);
    }
  };

  const fetchChart = async () => {
    try {
      const chartRes = await adminStatisticService.getRevenueChart(revenueDays);
      setChartData(chartRes);
    } catch (err) {
      console.error("Failed to load revenue chart", err);
    }
  };

  useEffect(() => {
    fetchStats();
  }, []);

  useEffect(() => {
    fetchChart();
  }, [revenueDays]);

  if (loading && !summary) {
    return (
      <div className="flex h-96 w-full items-center justify-center">
        <div className="flex flex-col items-center gap-3">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-zinc-300 border-t-zinc-950"></div>
          <p className="text-sm font-bold text-zinc-500 uppercase tracking-widest">Đang tải dữ liệu phân tích...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="surface p-8 text-center text-rose-600 bg-rose-50/50 border border-rose-200/50">
        <p className="font-bold">{error}</p>
        <button 
          onClick={fetchStats}
          className="mt-4 px-4 py-2 bg-zinc-950 text-white rounded-xl text-xs font-bold hover:bg-zinc-800 transition"
        >
          Tải lại dữ liệu
        </button>
      </div>
    );
  }

  // Map order status to beautiful Vietnamese text & custom modern colors
  const getStatusConfig = (status: string) => {
    switch (status.toUpperCase()) {
      case "PENDING":
        return { label: "Chờ xử lý", color: "bg-amber-500", text: "text-amber-500", bgLight: "bg-amber-50/80" };
      case "CONFIRMED":
        return { label: "Đã xác nhận", color: "bg-blue-500", text: "text-blue-500", bgLight: "bg-blue-50/80" };
      case "SHIPPED":
        return { label: "Đang giao hàng", color: "bg-purple-500", text: "text-purple-500", bgLight: "bg-purple-50/80" };
      case "DELIVERED":
        return { label: "Đã giao hàng", color: "bg-emerald-500", text: "text-emerald-500", bgLight: "bg-emerald-50/80" };
      case "CANCELLED":
        return { label: "Đã hủy", color: "bg-rose-500", text: "text-rose-500", bgLight: "bg-rose-50/80" };
      default:
        return { label: status, color: "bg-zinc-500", text: "text-zinc-500", bgLight: "bg-zinc-50/80" };
    }
  };

  // Safe SVG Chart Math
  const chartWidth = 720;
  const chartHeight = 240;
  const paddingLeft = 70;
  const paddingRight = 20;
  const paddingTop = 20;
  const paddingBottom = 40;

  const usableWidth = chartWidth - paddingLeft - paddingRight;
  const usableHeight = chartHeight - paddingTop - paddingBottom;

  const maxRevenue = Math.max(...chartData.map((d) => d.revenue), 100000);
  const points = chartData.map((d, index) => {
    const x = paddingLeft + (index / (chartData.length - 1 || 1)) * usableWidth;
    const y = paddingTop + usableHeight - (d.revenue / maxRevenue) * usableHeight;
    return { x, y, data: d };
  });

  const pathD = points.reduce((acc, p, i) => {
    return i === 0 ? `M ${p.x} ${p.y}` : `${acc} L ${p.x} ${p.y}`;
  }, "");

  const areaD = points.length > 0
    ? `${pathD} L ${points[points.length - 1].x} ${paddingTop + usableHeight} L ${points[0].x} ${paddingTop + usableHeight} Z`
    : "";

  // Helper for concise visual Y-Axis grids
  const gridLinesCount = 4;
  const gridLines = Array.from({ length: gridLinesCount + 1 }).map((_, i) => {
    const val = (maxRevenue / gridLinesCount) * i;
    const y = paddingTop + usableHeight - (val / maxRevenue) * usableHeight;
    return { val, y };
  });

  return (
    <div className="space-y-8">
      {/* 4 Premium KPI Cards */}
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
        {/* KPI 1: Net Revenue */}
        <div className="group relative overflow-hidden rounded-2xl border border-zinc-200/50 bg-white/70 p-6 shadow-sm backdrop-blur-md transition-all duration-300 hover:-translate-y-1 hover:shadow-md">
          <div className="absolute top-0 right-0 -mr-6 -mt-6 h-24 w-24 rounded-full bg-emerald-50 opacity-50 transition-all duration-300 group-hover:scale-110"></div>
          <div className="flex items-center justify-between">
            <span className="text-xs font-black uppercase tracking-wider text-zinc-400">Doanh thu thuần</span>
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-500 text-white shadow-md shadow-emerald-500/10">
              <DollarSign size={18} />
            </div>
          </div>
          <div className="mt-4">
            <h3 className="text-2xl font-black tracking-tight text-zinc-950">
              {formatPrice(summary?.totalRevenue ?? 0)}
            </h3>
            <p className="mt-1 text-[11px] font-bold text-emerald-600">
              Chỉ tính các đơn hàng đã thanh toán
            </p>
          </div>
        </div>

        {/* KPI 2: Total Orders */}
        <div className="group relative overflow-hidden rounded-2xl border border-zinc-200/50 bg-white/70 p-6 shadow-sm backdrop-blur-md transition-all duration-300 hover:-translate-y-1 hover:shadow-md">
          <div className="absolute top-0 right-0 -mr-6 -mt-6 h-24 w-24 rounded-full bg-indigo-50 opacity-50 transition-all duration-300 group-hover:scale-110"></div>
          <div className="flex items-center justify-between">
            <span className="text-xs font-black uppercase tracking-wider text-zinc-400">Tổng đơn hàng</span>
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-600 text-white shadow-md shadow-indigo-600/10">
              <ShoppingCart size={18} />
            </div>
          </div>
          <div className="mt-4">
            <h3 className="text-2xl font-black tracking-tight text-zinc-950">
              {summary?.totalOrders.toLocaleString("vi-VN") ?? 0}
            </h3>
            <p className="mt-1 text-[11px] font-bold text-zinc-400">
              Tất cả trạng thái vận đơn
            </p>
          </div>
        </div>

        {/* KPI 3: Total Customers */}
        <div className="group relative overflow-hidden rounded-2xl border border-zinc-200/50 bg-white/70 p-6 shadow-sm backdrop-blur-md transition-all duration-300 hover:-translate-y-1 hover:shadow-md">
          <div className="absolute top-0 right-0 -mr-6 -mt-6 h-24 w-24 rounded-full bg-amber-50 opacity-50 transition-all duration-300 group-hover:scale-110"></div>
          <div className="flex items-center justify-between">
            <span className="text-xs font-black uppercase tracking-wider text-zinc-400">Khách hàng</span>
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-500 text-white shadow-md shadow-amber-500/10">
              <Users size={18} />
            </div>
          </div>
          <div className="mt-4">
            <h3 className="text-2xl font-black tracking-tight text-zinc-950">
              {summary?.totalCustomers.toLocaleString("vi-VN") ?? 0}
            </h3>
            <p className="mt-1 text-[11px] font-bold text-amber-600">
              Tài khoản khách mua sắm
            </p>
          </div>
        </div>

        {/* KPI 4: Total Products */}
        <div className="group relative overflow-hidden rounded-2xl border border-zinc-200/50 bg-white/70 p-6 shadow-sm backdrop-blur-md transition-all duration-300 hover:-translate-y-1 hover:shadow-md">
          <div className="absolute top-0 right-0 -mr-6 -mt-6 h-24 w-24 rounded-full bg-rose-50 opacity-50 transition-all duration-300 group-hover:scale-110"></div>
          <div className="flex items-center justify-between">
            <span className="text-xs font-black uppercase tracking-wider text-zinc-400">Sản phẩm</span>
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-rose-500 text-white shadow-md shadow-rose-500/10">
              <Package size={18} />
            </div>
          </div>
          <div className="mt-4">
            <h3 className="text-2xl font-black tracking-tight text-zinc-950">
              {summary?.totalProducts.toLocaleString("vi-VN") ?? 0}
            </h3>
            <p className="mt-1 text-[11px] font-bold text-zinc-400">
              Số lượng mặt hàng trong kho
            </p>
          </div>
        </div>
      </div>

      {/* Main Charts & Analytics Details */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Left 2 cols: SVG Revenue Timeline */}
        <div className="lg:col-span-2 relative overflow-hidden rounded-2xl border border-zinc-200/50 bg-white/70 p-6 shadow-sm backdrop-blur-md flex flex-col justify-between">
          <div className="flex flex-wrap items-center justify-between gap-4 border-b border-zinc-100 pb-4">
            <div className="flex items-center gap-2">
              <TrendingUp className="h-4 w-4 text-indigo-600" />
              <h3 className="text-sm font-black uppercase tracking-wider text-zinc-900">Doanh thu theo thời gian</h3>
            </div>
            
            {/* Timeline Filter */}
            <div className="inline-flex rounded-xl bg-zinc-100 p-1 border border-zinc-200/30">
              {[7, 14, 30].map((days) => (
                <button
                  key={days}
                  onClick={() => setRevenueDays(days)}
                  className={`inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-bold transition-all duration-200 ${
                    revenueDays === days
                      ? "bg-white text-zinc-950 shadow-sm"
                      : "text-zinc-500 hover:text-zinc-950"
                  }`}
                >
                  <CalendarDays className="h-3 w-3 opacity-60" />
                  {days} ngày
                </button>
              ))}
            </div>
          </div>

          {/* Interactive SVG Chart Container */}
          <div className="relative mt-6 flex-1 min-h-[250px]">
            {chartData.length === 0 ? (
              <div className="flex h-56 w-full items-center justify-center text-xs font-bold text-zinc-400">
                Không có dữ liệu doanh thu trong khoảng thời gian này
              </div>
            ) : (
              <>
                <svg viewBox={`0 0 ${chartWidth} ${chartHeight}`} className="w-full h-auto" style={{ overflow: "visible" }}>
                  <defs>
                    {/* Soft gradient fill under chart */}
                    <linearGradient id="chart-gradient" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="#4f46e5" stopOpacity="0.25" />
                      <stop offset="100%" stopColor="#4f46e5" stopOpacity="0.0" />
                    </linearGradient>
                    {/* Line stroke gradient */}
                    <linearGradient id="line-gradient" x1="0" y1="0" x2="1" y2="0">
                      <stop offset="0%" stopColor="#6366f1" />
                      <stop offset="100%" stopColor="#4f46e5" />
                    </linearGradient>
                  </defs>

                  {/* Horizontal dotted grid lines */}
                  {gridLines.map((gl, i) => (
                    <g key={i}>
                      <line
                        x1={paddingLeft}
                        y1={gl.y}
                        x2={chartWidth - paddingRight}
                        y2={gl.y}
                        stroke="#e4e4e7"
                        strokeDasharray="4 4"
                        strokeWidth={1}
                      />
                      {/* Grid Labels */}
                      <text
                        x={paddingLeft - 10}
                        y={gl.y + 4}
                        fill="#a1a1aa"
                        fontSize={10}
                        fontWeight="bold"
                        textAnchor="end"
                      >
                        {gl.val >= 1000000 
                          ? `${(gl.val / 1000000).toFixed(1)}M` 
                          : gl.val >= 1000 
                            ? `${(gl.val / 1000).toFixed(0)}K` 
                            : gl.val}
                      </text>
                    </g>
                  ))}

                  {/* Under path filled area gradient */}
                  <path d={areaD} fill="url(#chart-gradient)" className="transition-all duration-300" />

                  {/* Sharp path stroke line */}
                  <path
                    d={pathD}
                    fill="none"
                    stroke="url(#line-gradient)"
                    strokeWidth={3}
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    className="transition-all duration-300"
                  />

                  {/* X Axis Labels */}
                  {points.map((p, i) => {
                    // Show dates sparsely to keep it highly clean
                    const shouldShowLabel = 
                      revenueDays === 7 
                        ? true 
                        : revenueDays === 14 
                          ? i % 2 === 0 
                          : i % 5 === 0;

                    if (!shouldShowLabel) return null;

                    // Formats YYYY-MM-DD to DD/MM
                    const parts = p.data.date.split("-");
                    const dateFormatted = parts.length === 3 ? `${parts[2]}/${parts[1]}` : p.data.date;

                    return (
                      <text
                        key={i}
                        x={p.x}
                        y={chartHeight - 15}
                        fill="#71717a"
                        fontSize={9}
                        fontWeight="bold"
                        textAnchor="middle"
                      >
                        {dateFormatted}
                      </text>
                    );
                  })}

                  {/* Interactive Dot Triggers */}
                  {points.map((p, i) => (
                    <g key={i}>
                      <circle
                        cx={p.x}
                        cy={p.y}
                        r={hoveredPoint?.date === p.data.date ? 6 : 3.5}
                        className={`transition-all duration-200 ${
                          hoveredPoint?.date === p.data.date
                            ? "fill-indigo-600 stroke-white stroke-[2px]"
                            : "fill-white stroke-indigo-500 stroke-[2px]"
                        }`}
                      />
                      <circle
                        cx={p.x}
                        cy={p.y}
                        r={16}
                        fill="transparent"
                        className="cursor-pointer"
                        onMouseEnter={() => setHoveredPoint({ x: p.x, y: p.y, date: p.data.date, revenue: p.data.revenue })}
                        onMouseLeave={() => setHoveredPoint(null)}
                      />
                    </g>
                  ))}
                </svg>

                {/* Floating Glass Tooltip */}
                {hoveredPoint && (
                  <div
                    className="absolute z-10 p-3 bg-zinc-950/90 text-white rounded-xl border border-white/10 shadow-xl pointer-events-none backdrop-blur-sm text-[11px] space-y-0.5 transition-all duration-150"
                    style={{
                      left: `${((hoveredPoint.x - paddingLeft) / usableWidth) * 90 + 5}%`,
                      top: `${Math.max(10, hoveredPoint.y - 70)}px`,
                    }}
                  >
                    <div className="font-bold text-zinc-400">{hoveredPoint.date}</div>
                    <div className="font-black text-amber-300 text-sm">
                      {formatPrice(hoveredPoint.revenue)}
                    </div>
                  </div>
                )}
              </>
            )}
          </div>
        </div>

        {/* Right 1 col: Order Status Distributions */}
        <div className="overflow-hidden rounded-2xl border border-zinc-200/50 bg-white/70 p-6 shadow-sm backdrop-blur-md">
          <div className="flex items-center gap-2 border-b border-zinc-100 pb-4">
            <Clock className="h-4 w-4 text-amber-500" />
            <h3 className="text-sm font-black uppercase tracking-wider text-zinc-900">Phân bố đơn hàng</h3>
          </div>

          <div className="mt-6 space-y-5">
            {orderStatusStats.length === 0 ? (
              <div className="text-center py-12 text-xs font-bold text-zinc-400">
                Không có dữ liệu trạng thái đơn hàng
              </div>
            ) : (
              orderStatusStats.map((stat, i) => {
                const config = getStatusConfig(stat.status);
                const totalOrders = orderStatusStats.reduce((acc, curr) => acc + curr.count, 0) || 1;
                const percentage = ((stat.count / totalOrders) * 100).toFixed(1);

                return (
                  <div key={i} className="space-y-1.5">
                    <div className="flex items-center justify-between text-xs font-bold text-zinc-700">
                      <span className="inline-flex items-center gap-1.5">
                        <span className={`h-2.5 w-2.5 rounded-full ${config.color}`}></span>
                        {config.label}
                      </span>
                      <span className="font-black text-zinc-950">
                        {stat.count} ({percentage}%)
                      </span>
                    </div>
                    {/* Premium Progress Bar */}
                    <div className="h-2 w-full rounded-full bg-zinc-100 overflow-hidden">
                      <div
                        className={`h-full rounded-full transition-all duration-500 ${config.color}`}
                        style={{ width: `${percentage}%` }}
                      ></div>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      </div>

      {/* Best Sellers Grid */}
      <div className="overflow-hidden rounded-2xl border border-zinc-200/50 bg-white/70 p-6 shadow-sm backdrop-blur-md">
        <div className="flex items-center gap-2 border-b border-zinc-100 pb-4">
          <Crown className="h-4 w-4 text-amber-500" />
          <h3 className="text-sm font-black uppercase tracking-wider text-zinc-900">Mặt hàng bán chạy</h3>
        </div>

        <div className="mt-6 overflow-x-auto">
          {bestSellers.length === 0 ? (
            <div className="text-center py-12 text-xs font-bold text-zinc-400">
              Chưa ghi nhận dữ liệu bán chạy
            </div>
          ) : (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-zinc-200/40 text-[10px] font-black uppercase tracking-widest text-zinc-400">
                  <th className="pb-3 text-center w-16">Xếp hạng</th>
                  <th className="pb-3 pl-4">Tên sản phẩm</th>
                  <th className="pb-3 text-center w-24">Mã SKU</th>
                  <th className="pb-3 text-center w-28">Số lượng bán</th>
                  <th className="pb-3 text-right pr-6 w-36">Tổng doanh thu</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100/60">
                {bestSellers.map((item, index) => {
                  const isTop3 = index < 3;
                  const rankColors = [
                    "bg-amber-100 text-amber-700 border-amber-200", // 1st Gold
                    "bg-slate-100 text-slate-700 border-slate-200", // 2nd Silver
                    "bg-orange-100 text-orange-700 border-orange-200", // 3rd Bronze
                  ];

                  return (
                    <tr key={index} className="group hover:bg-zinc-50/50 transition">
                      <td className="py-4 text-center">
                        {isTop3 ? (
                          <div className={`inline-flex items-center justify-center gap-1 px-2.5 py-1 rounded-full text-[10px] font-black border uppercase tracking-wider ${rankColors[index]}`}>
                            <Crown className="h-2.5 w-2.5" />
                            Top {index + 1}
                          </div>
                        ) : (
                          <span className="text-xs font-extrabold text-zinc-400">
                            #{index + 1}
                          </span>
                        )}
                      </td>
                      <td className="py-4 pl-4 text-xs font-bold text-zinc-900 group-hover:text-indigo-600 transition">
                        {item.productName}
                      </td>
                      <td className="py-4 text-center">
                        <span className="inline-block rounded bg-zinc-100 px-2 py-1 text-[10px] font-bold text-zinc-600 font-mono">
                          {item.sku}
                        </span>
                      </td>
                      <td className="py-4 text-center text-xs font-black text-zinc-950">
                        {item.soldQuantity.toLocaleString("vi-VN")}
                      </td>
                      <td className="py-4 text-right pr-6 text-xs font-black text-zinc-950">
                        {formatPrice(item.revenue)}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
};

export default AdminStatistics;
