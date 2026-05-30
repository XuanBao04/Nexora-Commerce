import apiClient from "@/services/api/apiClient";
import { ApiResponse } from "@/types/apiResponse";

export interface AdminDashboardSummary {
  totalRevenue: number;
  totalOrders: number;
  totalCustomers: number;
  totalProducts: number;
}

export interface RevenueChartItem {
  date: string; // ISO date format "YYYY-MM-DD"
  revenue: number;
}

export interface BestSellerItem {
  productName: string;
  sku: string;
  soldQuantity: number;
  revenue: number;
}

export interface OrderStatusStat {
  status: string;
  count: number;
}

const STATISTIC_API = "/v1/admin/statistics";

export const adminStatisticService = {
  /**
   * Fetch KPI overview summary
   */
  async getDashboardSummary(): Promise<AdminDashboardSummary> {
    const response = await apiClient.get<ApiResponse<AdminDashboardSummary>>(
      `${STATISTIC_API}/summary`
    );
    return response.data.data;
  },

  /**
   * Fetch daily revenue history for N days
   */
  async getRevenueChart(days: number = 7): Promise<RevenueChartItem[]> {
    const response = await apiClient.get<ApiResponse<RevenueChartItem[]>>(
      `${STATISTIC_API}/revenue-chart`,
      { params: { days } }
    );
    return response.data.data;
  },

  /**
   * Fetch top best selling products
   */
  async getBestSellers(limit: number = 5): Promise<BestSellerItem[]> {
    const response = await apiClient.get<ApiResponse<BestSellerItem[]>>(
      `${STATISTIC_API}/best-sellers`,
      { params: { limit } }
    );
    return response.data.data;
  },

  /**
   * Fetch orders count grouped by status
   */
  async getOrderStatusStats(): Promise<OrderStatusStat[]> {
    const response = await apiClient.get<ApiResponse<OrderStatusStat[]>>(
      `${STATISTIC_API}/order-status`
    );
    return response.data.data;
  },
};
