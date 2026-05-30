package com.nexoracommerce.statistic.service.impl;

import com.nexoracommerce.order.repository.OrderItemRepository;
import com.nexoracommerce.order.repository.OrderRepository;
import com.nexoracommerce.product.repository.ProductRepository;
import com.nexoracommerce.statistic.dto.response.AdminDashboardSummaryResponse;
import com.nexoracommerce.statistic.dto.response.BestSellerItem;
import com.nexoracommerce.statistic.dto.response.OrderStatusStat;
import com.nexoracommerce.statistic.dto.response.RevenueChartItem;
import com.nexoracommerce.statistic.service.IAdminStatisticService;
import com.nexoracommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatisticServiceImpl implements IAdminStatisticService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    public AdminDashboardSummaryResponse getDashboardSummary() {
        long totalRevenue = orderRepository.getTotalRevenueByPaymentStatusPaid();
        long totalOrders = orderRepository.count();
        long totalCustomers = userRepository.countCustomers();
        long totalProducts = productRepository.count();

        return new AdminDashboardSummaryResponse(totalRevenue, totalOrders, totalCustomers, totalProducts);
    }

    @Override
    public List<RevenueChartItem> getRevenueChart(int days) {
        LocalDateTime startDate = LocalDate.now().minusDays(days - 1).atStartOfDay();
        List<Object[]> results = orderRepository.findRevenueByDateRange(startDate);

        Map<LocalDate, Long> revenueMap = new HashMap<>();
        for (Object[] row : results) {
            Object dateObj = row[0];
            LocalDate date;
            if (dateObj instanceof java.time.LocalDate) {
                date = (java.time.LocalDate) dateObj;
            } else if (dateObj instanceof java.sql.Date) {
                date = ((java.sql.Date) dateObj).toLocalDate();
            } else if (dateObj instanceof java.sql.Timestamp) {
                date = ((java.sql.Timestamp) dateObj).toLocalDateTime().toLocalDate();
            } else {
                date = LocalDate.parse(dateObj.toString());
            }
            Long revenue = ((Number) row[1]).longValue();
            revenueMap.put(date, revenue);
        }

        List<RevenueChartItem> chartItems = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate current = today.minusDays(days - 1);
        while (!current.isAfter(today)) {
            Long revenue = revenueMap.getOrDefault(current, 0L);
            chartItems.add(new RevenueChartItem(current, revenue));
            current = current.plusDays(1);
        }

        return chartItems;
    }

    @Override
    public List<BestSellerItem> getBestSellers(int limit) {
        return orderItemRepository.findBestSellers(PageRequest.of(0, limit));
    }

    @Override
    public List<OrderStatusStat> getOrderStatusStats() {
        List<Object[]> results = orderRepository.getOrderStatusStats();
        List<OrderStatusStat> stats = new ArrayList<>();
        for (Object[] row : results) {
            if (row[0] != null) {
                String status = row[0].toString();
                Long count = ((Number) row[1]).longValue();
                stats.add(new OrderStatusStat(status, count));
            }
        }
        return stats;
    }
}
