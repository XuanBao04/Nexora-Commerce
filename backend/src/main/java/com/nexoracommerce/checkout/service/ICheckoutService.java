package com.nexoracommerce.checkout.service;

import com.nexoracommerce.checkout.dto.PaymentResponse;
import com.nexoracommerce.order.dto.request.OrderRequest;
import com.nexoracommerce.order.dto.response.OrderResponse;
import com.nexoracommerce.order.enums.PaymentMethod;

public interface ICheckoutService {
    OrderResponse checkoutWithRedisProtection(OrderRequest request, String userId, String ipAddress);

    PaymentResponse processPayment(String orderId, boolean paymentSuccessful, String providerTransactionId, Long amount, PaymentMethod paymentMethod);
}
