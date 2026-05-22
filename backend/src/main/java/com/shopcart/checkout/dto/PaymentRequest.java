package com.shopcart.checkout.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payment request for processing order payment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    
    /**
     * Payment method: CREDIT_CARD, BANK_TRANSFER, E_WALLET
     */
    private String paymentMethod;
    
    /**
     * Whether payment was successful
     */
    private boolean successful;
    
    /**
     * Optional transaction ID for tracking
     */
    private String transactionId;
    
    /**
     * Optional error message if payment failed
     */
    private String errorMessage;
}
