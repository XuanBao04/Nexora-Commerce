package com.nexoracommerce.order.dto.response;

import com.nexoracommerce.order.enums.PaymentMethod;
import com.nexoracommerce.order.enums.TransactionStatus;
import java.time.LocalDateTime;

/**
 * DTO for payment transaction response
 * Contains payment transaction details for frontend display
 */
public record PaymentTransactionResponse(
    Long id,
    Long amount,
    PaymentMethod paymentMethod,          // Payment method: COD, VNPAY, MOMO
    String providerTransactionId,         // Reference ID from payment provider
    TransactionStatus status,             // Transaction status: PENDING, SUCCESS, FAILED
    LocalDateTime createdAt               // Transaction creation timestamp
) {}
