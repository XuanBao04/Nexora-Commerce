package com.nexoracommerce.checkout.dto;

public record PaymentResponse(
    String orderId,
    String status,
    String message
) {}
