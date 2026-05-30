package com.nexoracommerce.statistic.dto.response;

public record AdminDashboardSummaryResponse(
    Long totalRevenue,
    Long totalOrders,
    Long totalCustomers,
    Long totalProducts
) {}
