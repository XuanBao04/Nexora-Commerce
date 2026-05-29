package com.nexoracommerce.order.dto.response;

import com.nexoracommerce.order.enums.PaymentMethod;
import com.nexoracommerce.order.enums.TransactionStatus;
import java.time.LocalDateTime;

/**
 * DTO for payment transaction response
 * Contains payment details and transaction status
 */
public record PaymentTransactionResponse(
    Long id,
    String orderId,
    PaymentMethod paymentMethod,        // COD, VNPAY, MOMO
    Long amount,                        // Amount in VND
    String providerTransactionId,       // Reference ID from payment provider
    TransactionStatus status,           // PENDING, SUCCESS, FAILED
    LocalDateTime createdAt
) {}
