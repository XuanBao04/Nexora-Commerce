package com.nexoracommerce.order.service.impl;

import com.nexoracommerce.common.enums.OrderStatus;
import com.nexoracommerce.common.exception.ResourceNotFoundException;
import com.nexoracommerce.constant.MessageConstant;
import com.nexoracommerce.order.dto.response.OrderStatusHistoryResponse;
import com.nexoracommerce.order.entity.Order;
import com.nexoracommerce.order.entity.OrderStatusHistory;
import com.nexoracommerce.order.mapper.OrderMapper;
import com.nexoracommerce.order.repository.OrderRepository;
import com.nexoracommerce.order.repository.OrderStatusHistoryRepository;
import com.nexoracommerce.order.service.IOrderStatusHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service implementation for Order Status History tracking
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderStatusHistoryServiceImpl implements IOrderStatusHistoryService {

    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderStatusHistoryResponse recordStatusChange(String orderId, String newStatusStr, String changedBy, String reason) {
        // Validate order exists
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    MessageConstant.Order.NOT_FOUND + orderId));

        // Parse and validate status
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(newStatusStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid order status: " + newStatusStr);
        }

        // Create status history record
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(newStatus)
                .changedBy(changedBy != null ? changedBy : "SYSTEM")
                .reason(reason)
                .build();

        OrderStatusHistory savedHistory = orderStatusHistoryRepository.save(history);
        return orderMapper.toOrderStatusHistoryResponse(savedHistory);
    }

    @Override
    public List<OrderStatusHistoryResponse> getOrderStatusHistory(String orderId) {
        // Validate order exists
        orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    MessageConstant.Order.NOT_FOUND + orderId));

        List<OrderStatusHistory> histories = orderStatusHistoryRepository.findByOrder_IdOrderByCreatedAtDesc(orderId);
        return orderMapper.toStatusHistoryResponseList(histories);
    }

    @Override
    public OrderStatusHistoryResponse getLatestStatusChange(String orderId) {
        // Validate order exists
        orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    MessageConstant.Order.NOT_FOUND + orderId));

        return orderStatusHistoryRepository.findLatestStatusChangeByOrder(orderId)
                .map(orderMapper::toOrderStatusHistoryResponse)
                .orElse(null);
    }

    @Override
    public Page<OrderStatusHistoryResponse> getOrderStatusHistoryPageable(String orderId, Pageable pageable) {
        // Validate order exists
        orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    MessageConstant.Order.NOT_FOUND + orderId));

        // Note: OrderStatusHistoryRepository doesn't have pageable method
        // This is a limitation - would need to add custom query
        throw new UnsupportedOperationException("Pagination for order status history not yet implemented");
    }

    @Override
    public boolean hasReachedStatus(String orderId, String statusStr) {
        // Validate order exists
        orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    MessageConstant.Order.NOT_FOUND + orderId));

        // Parse status
        OrderStatus status;
        try {
            status = OrderStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid order status: " + statusStr);
        }

        return orderStatusHistoryRepository.existsByOrder_IdAndStatus(orderId, status);
    }

    @Override
    public long getStatusChangeCount(String orderId) {
        // Validate order exists
        orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    MessageConstant.Order.NOT_FOUND + orderId));

        return orderStatusHistoryRepository.countByOrder_Id(orderId);
    }
}
