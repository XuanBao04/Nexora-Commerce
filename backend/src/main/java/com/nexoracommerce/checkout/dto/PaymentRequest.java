package com.nexoracommerce.checkout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payment request for processing order payment
 */
public record PaymentRequest(
    /**
     * Payment method: CREDIT_CARD, BANK_TRANSFER, E_WALLET
     */
    @NotBlank(message = "Payment method is required")
    String paymentMethod,

    /**
     * Whether payment was successful
     */
    @NotNull(message = "Payment result is required")
    Boolean successful,
    
    /**
     * Optional transaction ID for tracking
     */
    String transactionId,
    
    /**
     * Optional error message if payment failed
     */
    String errorMessage
) {}
