package com.nexoracommerce.order.service;

import com.nexoracommerce.order.dto.request.PaymentTransactionRequest;
import com.nexoracommerce.order.dto.response.PaymentTransactionResponse;
import com.nexoracommerce.order.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for Payment operations
 */
public interface IPaymentService {

    /**
     * Create a new payment transaction
     * @param request PaymentTransactionRequest with payment details
     * @return created PaymentTransactionResponse
     */
    PaymentTransactionResponse createPaymentTransaction(PaymentTransactionRequest request);

    /**
     * Get payment transaction by ID
     * @param transactionId the transaction ID
     * @return PaymentTransactionResponse
     */
    PaymentTransactionResponse getPaymentTransaction(Long transactionId);

    /**
     * Get all payment transactions for an order
     * @param orderId the order ID
     * @return list of PaymentTransactionResponse
     */
    List<PaymentTransactionResponse> getOrderPaymentTransactions(String orderId);

    /**
     * Get the latest payment transaction for an order
     * @param orderId the order ID
     * @return PaymentTransactionResponse or null if not found
     */
    PaymentTransactionResponse getLatestPaymentTransaction(String orderId);

    /**
     * Get payment transactions by status with pagination
     * @param paymentStatus the payment status
     * @param pageable pagination parameters
     * @return Page of PaymentTransactionResponse
     */
    Page<PaymentTransactionResponse> getPaymentTransactionsByStatus(PaymentStatus paymentStatus, Pageable pageable);

    /**
     * Update order payment status after successful transaction
     * @param orderId the order ID
     * @param paymentStatus the new payment status
     */
    void updateOrderPaymentStatus(String orderId, PaymentStatus paymentStatus);

    /**
     * Check if order has been paid
     * @param orderId the order ID
     * @return true if paid, false otherwise
     */
    boolean isOrderPaid(String orderId);

    /**
     * Get total payment amount for an order
     * @param orderId the order ID
     * @return total amount paid in VND
     */
    Long getTotalPaymentAmount(String orderId);
}
