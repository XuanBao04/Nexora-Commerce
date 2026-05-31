package com.nexoracommerce.payment.enums;

/**
 * Payment status for orders
 * - UNPAID: Order created but payment not received yet
 * - PAID: Payment received successfully
 * - REFUNDED: Payment refunded to customer
 */
public enum PaymentStatus {
    UNPAID,     // Payment pending
    PAID,       // Payment received
    REFUNDED    // Payment refunded
}
