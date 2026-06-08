package com.nexoracommerce.statistic.service;

import com.nexoracommerce.statistic.dto.response.AdminDashboardSummaryResponse;
import com.nexoracommerce.statistic.dto.response.BestSellerItem;
import com.nexoracommerce.statistic.dto.response.OrderStatusStat;
import com.nexoracommerce.statistic.dto.response.RevenueChartItem;
import java.util.List;

public interface AdminStatisticService {
    AdminDashboardSummaryResponse getDashboardSummary();
    List<RevenueChartItem> getRevenueChart(int days);
    List<BestSellerItem> getBestSellers(int limit);
    List<OrderStatusStat> getOrderStatusStats();
}
