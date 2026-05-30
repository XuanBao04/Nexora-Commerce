package com.nexoracommerce.payment.service;

import com.nexoracommerce.order.dto.response.OrderResponse;

public interface IPaymentProviderService {
    /**
     * Generate payment URL for the given order
     */
    String generatePaymentUrl(OrderResponse order, String ipAddress);
}
