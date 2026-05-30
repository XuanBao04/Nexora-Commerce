package com.nexoracommerce.statistic.controller;

import com.nexoracommerce.common.response.ApiResponse;
import com.nexoracommerce.statistic.dto.response.AdminDashboardSummaryResponse;
import com.nexoracommerce.statistic.dto.response.BestSellerItem;
import com.nexoracommerce.statistic.dto.response.OrderStatusStat;
import com.nexoracommerce.statistic.dto.response.RevenueChartItem;
import com.nexoracommerce.statistic.service.IAdminStatisticService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/statistics")
@RequiredArgsConstructor
@Tag(name = "Admin Statistics Module", description = "Endpoints for admin to view business dashboard analytics")
public class AdminStatisticController {

    private final IAdminStatisticService statisticService;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get dashboard overview KPIs", description = "Fetch total revenue, orders, customers, and active products.")
    public ResponseEntity<ApiResponse<AdminDashboardSummaryResponse>> getDashboardSummary() {
        return ResponseEntity.ok(ApiResponse.ok(statisticService.getDashboardSummary()));
    }

    @GetMapping("/revenue-chart")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get daily revenue timeline", description = "Fetch net daily revenue for the past N days.")
    public ResponseEntity<ApiResponse<List<RevenueChartItem>>> getRevenueChart(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(ApiResponse.ok(statisticService.getRevenueChart(days)));
    }

    @GetMapping("/best-sellers")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get top best selling products", description = "Fetch the top best selling products by quantity ordered.")
    public ResponseEntity<ApiResponse<List<BestSellerItem>>> getBestSellers(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(statisticService.getBestSellers(limit)));
    }

    @GetMapping("/order-status")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get order counts grouped by status", description = "Fetch numbers of orders for each distinct order status.")
    public ResponseEntity<ApiResponse<List<OrderStatusStat>>> getOrderStatusStats() {
        return ResponseEntity.ok(ApiResponse.ok(statisticService.getOrderStatusStats()));
    }
}
