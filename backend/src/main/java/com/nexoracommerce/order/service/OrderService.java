package com.nexoracommerce.order.service;

import com.nexoracommerce.common.enums.OrderStatus;
import com.nexoracommerce.order.dto.request.OrderRequest;
import com.nexoracommerce.order.dto.request.OrderStatusChangeRequest;
import com.nexoracommerce.order.dto.response.OrderResponse;
import com.nexoracommerce.order.dto.response.OrderPreviewResponse;
import com.nexoracommerce.order.dto.response.OrderStatusHistoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for Order operations
 */
public interface IOrderService {

    /**
     * Create a new order
     * @param request OrderRequest containing order details and items
     * @param userId the user creating the order
     * @return created OrderResponse
     */
    OrderResponse createOrder(OrderRequest request, String userId);

    /**
     * Get order by ID
     * @param orderId the order ID
     * @return OrderResponse with full details
     */
    OrderResponse getOrderById(String orderId);

    /**
     * Get all orders for a user
     * @param userId the user ID
     * @return list of OrderResponse
     */
    List<OrderResponse> getUserOrders(String userId);

    /**
     * Get all orders for a user with pagination
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return Page of OrderResponse
     */
    Page<OrderResponse> getUserOrdersPageable(String userId, Pageable pageable);

    /**
     * Cancel an order
     * @param orderId the order ID
     * @return updated OrderResponse
     */
    OrderResponse cancelOrder(String orderId);

    /**
     * Update order status with audit trail
     * @param request containing orderId, newStatus, and optional reason
     * @return updated OrderResponse
     */
    OrderResponse updateOrderStatus(OrderStatusChangeRequest request);

    /**
     * Get order status history
     * @param orderId the order ID
     * @return list of OrderStatusHistoryResponse
     */
    List<OrderStatusHistoryResponse> getOrderStatusHistory(String orderId);

    /**
     * Preview order price before checkout
     * @param request OrderRequest containing order details
     * @return OrderPreviewResponse with price breakdown
     */
    OrderPreviewResponse previewOrder(OrderRequest request);

    /**
     * Get all orders (admin only)
     * @return list of all OrderResponse
     */
    List<OrderResponse> getAllOrders();

    /**
     * Get all orders with pagination (admin only)
     * @param pageable pagination parameters
     * @return Page of OrderResponse
     */
    Page<OrderResponse> getAllOrdersPageable(Pageable pageable);

    /**
     * Get orders by status with pagination
     * @param status the order status
     * @param pageable pagination parameters
     * @return Page of OrderResponse
     */
    Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable);

    /**
     * Get unpaid orders for a user
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return Page of OrderResponse
     */
    Page<OrderResponse> getUnpaidOrdersByUser(String userId, Pageable pageable);
}
