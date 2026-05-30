package com.nexoracommerce.statistic.dto.response;

public record OrderStatusStat(
    String status,
    Long count
) {}
