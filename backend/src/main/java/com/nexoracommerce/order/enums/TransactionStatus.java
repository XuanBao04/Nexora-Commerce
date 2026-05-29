package com.nexoracommerce.order.enums;

/**
 * Payment transaction status
 * - PENDING: Transaction is being processed
 * - SUCCESS: Transaction completed successfully
 * - FAILED: Transaction failed
 */
public enum TransactionStatus {
    PENDING,    // Processing
    SUCCESS,    // Completed
    FAILED      // Failed
}
