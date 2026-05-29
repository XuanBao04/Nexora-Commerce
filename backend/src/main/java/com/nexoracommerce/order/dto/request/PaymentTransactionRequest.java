package com.nexoracommerce.order.dto.request;

import com.nexoracommerce.order.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * DTO for payment transaction request
 * Used when initiating a payment for an order
 */
public record PaymentTransactionRequest(
    @NotBlank(message = "Order ID is required")
    String orderId,
    
    @NotNull(message = "Payment method is required")
    PaymentMethod paymentMethod,        // COD, VNPAY, MOMO
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    Long amount,                        // Amount in VND
    
    @Size(max = 255, message = "Provider transaction ID must not exceed 255 characters")
    String providerTransactionId        // Reference ID from payment provider (optional for COD)
) {}
