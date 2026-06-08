package com.nexoracommerce.checkout.service;

import com.nexoracommerce.checkout.dto.PaymentResponse;
import com.nexoracommerce.order.dto.request.OrderRequest;
import com.nexoracommerce.order.dto.response.OrderResponse;
import com.nexoracommerce.payment.enums.PaymentMethod;

public interface CheckoutService {
    OrderResponse checkoutWithRedisProtection(OrderRequest request, String userId, String ipAddress);

    PaymentResponse processPayment(String orderId, boolean paymentSuccessful, String providerTransactionId, Long amount, PaymentMethod paymentMethod);

    OrderResponse repayPayment(String orderId, String ipAddress);
}
