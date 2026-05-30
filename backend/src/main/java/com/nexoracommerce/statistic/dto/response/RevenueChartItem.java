package com.nexoracommerce.statistic.dto.response;

import java.time.LocalDate;

public record RevenueChartItem(
    LocalDate date,
    Long revenue
) {}
