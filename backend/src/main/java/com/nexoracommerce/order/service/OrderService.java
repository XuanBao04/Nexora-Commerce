package com.nexoracommerce.order.service;

import com.nexoracommerce.common.enums.OrderStatus;
import com.nexoracommerce.order.dto.request.OrderRequest;
import com.nexoracommerce.order.dto.request.OrderStatusChangeRequest;
import com.nexoracommerce.order.dto.response.OrderResponse;
import com.nexoracommerce.payment.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;
import com.nexoracommerce.order.dto.response.OrderPreviewResponse;

/**
 * Interface cung cấp các tác vụ xử lý đơn hàng
 */
public interface OrderService {
    /**
     * Tạo đơn hàng mới
     */
    OrderResponse createOrder(OrderRequest request, String userId);

    /**
     * Xem trước thông tin giá trị đơn hàng
     */
    OrderPreviewResponse previewOrder(OrderRequest request);

    /**
     * Lấy chi tiết đơn hàng cho Admin
     */
    OrderResponse getOrderByIdForAdmin(String orderId);

    /**
     * Lấy chi tiết đơn hàng cho User (có kiểm tra quyền)
     */
    OrderResponse getOrderById(String orderId, String userId);

    /**
     * Lấy danh sách đơn hàng có bộ lọc linh hoạt (cho Admin và User)
     */
    Page<OrderResponse> getOrdersWithFilters(String userId, OrderStatus status, PaymentStatus paymentStatus, String couponCode, LocalDateTime startDate, LocalDateTime endDate, Long minPrice, Pageable pageable);

    /**
     * Cập nhật trạng thái đơn hàng (cho Admin)
     */
    OrderResponse updateOrderStatus(String orderId, OrderStatusChangeRequest request);

    /**
     * Hủy đơn hàng (cho User)
     */
    OrderResponse cancelOrder(String orderId, String userId, String reason);

    /**
     * Lấy lịch sử trạng thái đơn hàng
     */
    java.util.List<com.nexoracommerce.order.dto.response.OrderStatusHistoryResponse> getOrderStatusHistory(String orderId);
}
