package com.nexoracommerce.order.service.impl;

import com.nexoracommerce.common.exception.ResourceNotFoundException;
import com.nexoracommerce.constant.MessageConstant;
import com.nexoracommerce.order.dto.request.PaymentTransactionRequest;
import com.nexoracommerce.order.dto.response.PaymentTransactionResponse;
import com.nexoracommerce.order.entity.Order;
import com.nexoracommerce.order.entity.PaymentTransaction;
import com.nexoracommerce.order.enums.PaymentStatus;
import com.nexoracommerce.order.enums.TransactionStatus;
import com.nexoracommerce.order.mapper.OrderMapper;
import com.nexoracommerce.order.repository.OrderRepository;
import com.nexoracommerce.order.repository.PaymentTransactionRepository;
import com.nexoracommerce.order.service.IPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service implementation for Payment operations
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements IPaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public PaymentTransactionResponse createPaymentTransaction(PaymentTransactionRequest request) {
        // Validate order exists
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    MessageConstant.Order.NOT_FOUND + request.orderId()));

        // Create payment transaction
        PaymentTransaction transaction = PaymentTransaction.builder()
                .order(order)
                .paymentMethod(request.paymentMethod())
                .amount(request.amount())
                .providerTransactionId(request.providerTransactionId())
                .status(TransactionStatus.PENDING)
                .build();

        PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);
        return orderMapper.toPaymentTransactionResponse(savedTransaction);
    }

    @Override
    public PaymentTransactionResponse getPaymentTransaction(Long transactionId) {
        PaymentTransaction transaction = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found: " + transactionId));
        return orderMapper.toPaymentTransactionResponse(transaction);
    }

    @Override
    public List<PaymentTransactionResponse> getOrderPaymentTransactions(String orderId) {
        // Validate order exists
        orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    MessageConstant.Order.NOT_FOUND + orderId));

        List<PaymentTransaction> transactions = paymentTransactionRepository.findByOrder_IdOrderByCreatedAtDesc(orderId);
        return orderMapper.toPaymentTransactionResponseList(transactions);
    }

    @Override
    public PaymentTransactionResponse getLatestPaymentTransaction(String orderId) {
        // Validate order exists
        orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    MessageConstant.Order.NOT_FOUND + orderId));

        return paymentTransactionRepository.findLatestPaymentTransaction(orderId)
                .map(orderMapper::toPaymentTransactionResponse)
                .orElse(null);
    }

    @Override
    public Page<PaymentTransactionResponse> getPaymentTransactionsByStatus(PaymentStatus paymentStatus, Pageable pageable) {
        // Map from PaymentStatus (order level) to TransactionStatus (transaction level)
        // Note: Typically we query by TransactionStatus, not PaymentStatus
        // This is a helper method - adjust as per business logic
        throw new UnsupportedOperationException("Use TransactionStatus instead of PaymentStatus for payment transactions");
    }

    @Override
    @Transactional
    public void updateOrderPaymentStatus(String orderId, PaymentStatus paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    MessageConstant.Order.NOT_FOUND + orderId));

        order.setPaymentStatus(paymentStatus);
        order.setLastModifiedDate(LocalDateTime.now());
        orderRepository.save(order);
    }

    @Override
    public boolean isOrderPaid(String orderId) {
        // Check if order has a successful payment transaction
        return paymentTransactionRepository.existsByOrder_IdAndStatus(orderId, TransactionStatus.SUCCESS);
    }

    @Override
    public Long getTotalPaymentAmount(String orderId) {
        // Validate order exists
        orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    MessageConstant.Order.NOT_FOUND + orderId));

        // Sum successful payment amounts
        long total = paymentTransactionRepository.getTotalAmountByStatus(TransactionStatus.SUCCESS);
        return total;
    }
}
