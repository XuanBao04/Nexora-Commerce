package com.nexoracommerce.checkout.scheduler;

import com.nexoracommerce.common.enums.OrderStatus;
import com.nexoracommerce.order.entity.Order;
import com.nexoracommerce.order.repository.OrderRepository;
import com.nexoracommerce.order.service.OrderStatusHistoryService;
import com.nexoracommerce.payment.enums.PaymentStatus;
import com.nexoracommerce.payment.service.PaymentRollbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentTimeoutScheduler {

    private final OrderRepository orderRepository;
    private final PaymentRollbackService paymentRollbackService;
    private final OrderStatusHistoryService orderStatusHistoryService;

    /**
     * Tác vụ tự động quét mỗi 5 phút để hủy các đơn hàng VNPay chưa thanh toán quá 15 phút
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void cancelExpiredUnpaidVnPayOrders() {
        log.info("Running job to cancel expired unpaid VNPAY orders...");

        LocalDateTime thresholdTime = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusMinutes(15);
        List<Order> expiredOrders = orderRepository.findExpiredUnpaidVnPayOrders(thresholdTime);

        if (expiredOrders.isEmpty()) {
            log.info("No expired VNPAY orders found.");
            return;
        }

        log.info("Found {} expired VNPAY orders. Processing cancellation...", expiredOrders.size());

        for (Order order : expiredOrders) {
            try {
                log.info("Cancelling expired order: orderId={}, createdAt={}", order.getId(), order.getCreatedAt());

                // 1. Hoàn trả tồn kho (ở cả Database và Redis)
                paymentRollbackService.rollbackPaymentFailure(order);

                // 2. Chuyển trạng thái đơn hàng sang CANCELLED
                order.setStatus(OrderStatus.CANCELLED);
                order.setPaymentStatus(PaymentStatus.UNPAID);
                orderRepository.save(order);

                // 3. Ghi lại lịch sử trạng thái
                orderStatusHistoryService.recordStatusChange(
                    order.getId(),
                    OrderStatus.CANCELLED.name(),
                    "SYSTEM",
                    "Order automatically cancelled due to payment timeout (15 minutes expired)."
                );

                log.info("Successfully cancelled expired order: {}", order.getId());
            } catch (Exception e) {
                log.error("Failed to cancel expired order: {}", order.getId(), e);
            }
        }
    }
}
