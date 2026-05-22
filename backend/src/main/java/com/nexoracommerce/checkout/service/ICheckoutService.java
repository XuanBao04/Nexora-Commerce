package com.nexoracommerce.checkout.service;

import com.nexoracommerce.checkout.dto.PaymentResponse;
import com.nexoracommerce.order.dto.request.OrderRequest;
import com.nexoracommerce.order.dto.response.OrderResponse;

public interface ICheckoutService {
    OrderResponse checkoutWithRedisProtection(OrderRequest request, String userId);

    PaymentResponse processPayment(String orderId, boolean paymentSuccessful);
}
