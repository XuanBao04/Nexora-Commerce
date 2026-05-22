package com.nexoracommerce.order.service;

import com.nexoracommerce.order.dto.response.OrderResponse;
import com.nexoracommerce.order.dto.response.OrderPreviewResponse;
import com.nexoracommerce.order.dto.request.OrderRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

/**
 * Service interface for Order operations
 */
public interface IOrderService {

    /**
     * Create a new order
     * @param request OrderRequest containing order details
     * @return created OrderResponse
     */
    OrderResponse createOrder(OrderRequest request, String userId);

    /**
     * Get order by ID
     * @param orderId the order ID
     * @return OrderResponse
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
     * Update order status
     * @param orderId the order ID
     * @param status new status
     * @return updated OrderResponse
     */
    OrderResponse updateOrderStatus(String orderId, String status);

    /**
     * Preview order price before checkout
     * @param request OrderRequest containing order details
     * @return OrderPreviewResponse with price breakdown
     */
    OrderPreviewResponse previewOrder(OrderRequest request);

    /**
     * Get all orders
     * @return list of all OrderResponse
     */
    List<OrderResponse> getAllOrders();

    /**
     * Get all orders with pagination (for admin)
     * @param pageable pagination parameters
     * @return Page of OrderResponse
     */
    Page<OrderResponse> getAllOrdersPageable(Pageable pageable);
}
