package com.nexoracommerce.mail.service.impl;

import com.nexoracommerce.common.exception.ResourceNotFoundException;
import com.nexoracommerce.mail.service.MailService;
import com.nexoracommerce.order.entity.Order;
import com.nexoracommerce.order.entity.OrderItem;
import com.nexoracommerce.order.repository.OrderRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final OrderRepository orderRepository;

    @Value("${MAIL_FROM:Nexora Commerce <no-reply@nexoracommerce.com>}")
    private String mailFrom;

    @Async("taskExecutor")
    @Transactional(readOnly = true)
    @Override
    public void sendOrderConfirmationEmail(String orderId) {
        log.info("Starting asynchronous order confirmation email for orderId: {}", orderId);
        
        try {
            // Eagerly fetch order with items and user
            Order order = orderRepository.findByIdWithItemsAndUser(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
            
            String customerEmail = order.getUser().getEmail();
            if (customerEmail == null || customerEmail.trim().isEmpty()) {
                log.warn("Cannot send email for order: {}. User email is empty.", orderId);
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, 
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, 
                    StandardCharsets.UTF_8.name());

            String sender = mailFrom;
            if (sender != null) {
                sender = sender.trim();
                if (sender.startsWith("\"") && sender.endsWith("\"")) {
                    sender = sender.substring(1, sender.length() - 1);
                }
            }

            helper.setFrom(sender);
            helper.setTo(customerEmail);
            helper.setSubject("Nexora Commerce — Xác nhận đơn hàng " + order.getId());
            
            String htmlContent = buildOrderEmailTemplate(order);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Order confirmation email sent successfully to {} for orderId: {}", customerEmail, orderId);

        } catch (ResourceNotFoundException e) {
            log.error("Failed to send order email. Order not found: {}", orderId, e);
        } catch (MessagingException e) {
            log.error("SMTP / MIME messaging error sending order email for orderId: {}", orderId, e);
        } catch (Exception e) {
            log.error("Unexpected error sending order confirmation email for orderId: {}", orderId, e);
        }
    }

    private String buildOrderEmailTemplate(Order order) {
        // Escaping untrusted user inputs (SECURITY)
        String customerName = HtmlUtils.htmlEscape(order.getUser().getFullName() != null ? order.getUser().getFullName() : order.getUser().getUsername());
        String shippingAddress = HtmlUtils.htmlEscape(order.getShippingAddress());
        String phoneNumber = HtmlUtils.htmlEscape(order.getPhoneNumber());
        String customerNote = order.getCustomerNote() != null ? HtmlUtils.htmlEscape(order.getCustomerNote()) : "";
        String orderId = HtmlUtils.htmlEscape(order.getId());
        String paymentMethod = HtmlUtils.htmlEscape(order.getPaymentTransactions().stream()
                .findFirst()
                .map(t -> t.getPaymentMethod().name())
                .orElse("VNPAY"));

        String orderDate = order.getCreatedAt() != null 
                ? order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) 
                : "";

        // Premium HTML layout
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>");
        sb.append("<html>");
        sb.append("<head>");
        sb.append("  <meta charset='utf-8'>");
        sb.append("  <meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        sb.append("  <title>Xác nhận đơn hàng</title>");
        sb.append("  <style>");
        sb.append("    body { font-family: 'Inter', 'Roboto', 'Helvetica Neue', Arial, sans-serif; background-color: #f6f9fc; margin: 0; padding: 0; color: #333333; -webkit-font-smoothing: antialiased; }");
        sb.append("    .wrapper { width: 100%; background-color: #f6f9fc; padding: 40px 0; }");
        sb.append("    .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); }");
        sb.append("    .header { background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%); padding: 40px 30px; text-align: center; color: #ffffff; }");
        sb.append("    .header h1 { margin: 0; font-size: 24px; font-weight: 700; letter-spacing: 0.5px; }");
        sb.append("    .header p { margin: 10px 0 0 0; font-size: 14px; opacity: 0.9; }");
        sb.append("    .content { padding: 40px 30px; }");
        sb.append("    .welcome { font-size: 16px; line-height: 1.6; margin-bottom: 30px; }");
        sb.append("    .welcome h2 { font-size: 18px; color: #1e3c72; margin-top: 0; }");
        sb.append("    .card { background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 20px; margin-bottom: 30px; }");
        sb.append("    .card-title { font-size: 14px; font-weight: 700; text-transform: uppercase; color: #475569; margin-bottom: 12px; border-bottom: 1px dashed #cbd5e1; padding-bottom: 6px; }");
        sb.append("    .grid { display: table; width: 100%; }");
        sb.append("    .grid-row { display: table-row; }");
        sb.append("    .grid-label { display: table-cell; width: 120px; font-weight: 600; padding: 6px 0; font-size: 14px; color: #64748b; }");
        sb.append("    .grid-value { display: table-cell; padding: 6px 0; font-size: 14px; color: #1e293b; }");
        sb.append("    .table-responsive { width: 100%; overflow-x: auto; margin-bottom: 30px; }");
        sb.append("    table { width: 100%; border-collapse: collapse; text-align: left; }");
        sb.append("    th { padding: 12px; border-bottom: 2px solid #e2e8f0; font-size: 13px; font-weight: 700; color: #475569; text-transform: uppercase; }");
        sb.append("    td { padding: 12px; border-bottom: 1px solid #f1f5f9; font-size: 14px; color: #334155; }");
        sb.append("    .product-info { font-weight: 600; color: #1e293b; }");
        sb.append("    .product-variant { font-size: 12px; color: #64748b; margin-top: 2px; }");
        sb.append("    .price-summary { display: table; width: 100%; margin-top: 20px; }");
        sb.append("    .price-row { display: table-row; }");
        sb.append("    .price-label { display: table-cell; text-align: right; padding: 8px 12px; font-size: 14px; color: #64748b; }");
        sb.append("    .price-value { display: table-cell; text-align: right; padding: 8px 0; font-size: 14px; color: #1e293b; width: 120px; }");
        sb.append("    .total-row .price-label { font-weight: 700; font-size: 16px; color: #1e3c72; border-top: 2px solid #e2e8f0; padding-top: 15px; }");
        sb.append("    .total-row .price-value { font-weight: 700; font-size: 18px; color: #1e3c72; border-top: 2px solid #e2e8f0; padding-top: 15px; }");
        sb.append("    .footer { text-align: center; padding: 30px; background-color: #f1f5f9; border-top: 1px solid #e2e8f0; color: #64748b; font-size: 12px; }");
        sb.append("    .footer p { margin: 5px 0; }");
        sb.append("  </style>");
        sb.append("</head>");
        sb.append("<body>");
        sb.append("<div class='wrapper'>");
        sb.append("  <div class='container'>");
        
        // Header
        sb.append("    <div class='header'>");
        sb.append("      <h1>Nexora Commerce</h1>");
        sb.append("      <p>Cảm ơn bạn đã đặt mua sản phẩm tại cửa hàng chúng tôi!</p>");
        sb.append("    </div>");
        
        // Content
        sb.append("    <div class='content'>");
        sb.append("      <div class='welcome'>");
        sb.append("        <h2>Xin chào ").append(customerName).append(",</h2>");
        sb.append("        <p>Đơn hàng mã số <strong>").append(orderId).append("</strong> của bạn đã được thanh toán thành công và được chuyển sang trạng thái <strong>XÁC NHẬN</strong>. Dưới đây là thông tin chi tiết về hóa đơn đơn hàng của bạn.</p>");
        sb.append("      </div>");
        
        // Delivery / Order Info Cards
        sb.append("      <div class='card'>");
        sb.append("        <div class='card-title'>Thông tin đơn hàng</div>");
        sb.append("        <div class='grid'>");
        sb.append("          <div class='grid-row'>");
        sb.append("            <div class='grid-label'>Mã đơn hàng:</div>");
        sb.append("            <div class='grid-value'><strong>").append(orderId).append("</strong></div>");
        sb.append("          </div>");
        sb.append("          <div class='grid-row'>");
        sb.append("            <div class='grid-label'>Ngày đặt mua:</div>");
        sb.append("            <div class='grid-value'>").append(orderDate).append("</div>");
        sb.append("          </div>");
        sb.append("          <div class='grid-row'>");
        sb.append("            <div class='grid-label'>Thanh toán:</div>");
        sb.append("            <div class='grid-value'>").append(paymentMethod).append(" (Thành công)</div>");
        sb.append("          </div>");
        sb.append("        </div>");
        sb.append("      </div>");
        
        sb.append("      <div class='card'>");
        sb.append("        <div class='card-title'>Thông tin giao nhận</div>");
        sb.append("        <div class='grid'>");
        sb.append("          <div class='grid-row'>");
        sb.append("            <div class='grid-label'>Người nhận:</div>");
        sb.append("            <div class='grid-value'>").append(customerName).append("</div>");
        sb.append("          </div>");
        sb.append("          <div class='grid-row'>");
        sb.append("            <div class='grid-label'>Điện thoại:</div>");
        sb.append("            <div class='grid-value'>").append(phoneNumber).append("</div>");
        sb.append("          </div>");
        sb.append("          <div class='grid-row'>");
        sb.append("            <div class='grid-label'>Địa chỉ:</div>");
        sb.append("            <div class='grid-value'>").append(shippingAddress).append("</div>");
        sb.append("          </div>");
        if (!customerNote.isEmpty()) {
            sb.append("          <div class='grid-row'>");
            sb.append("            <div class='grid-label'>Ghi chú:</div>");
            sb.append("            <div class='grid-value'><em>").append(customerNote).append("</em></div>");
            sb.append("          </div>");
        }
        sb.append("        </div>");
        sb.append("      </div>");
        
        // Order Items Table
        sb.append("      <div class='table-responsive'>");
        sb.append("        <table>");
        sb.append("          <thead>");
        sb.append("            <tr>");
        sb.append("              <th>Sản phẩm</th>");
        sb.append("              <th style='text-align: center;'>Số lượng</th>");
        sb.append("              <th style='text-align: right;'>Đơn giá</th>");
        sb.append("              <th style='text-align: right;'>Thành tiền</th>");
        sb.append("            </tr>");
        sb.append("          </thead>");
        sb.append("          <tbody>");
        
        long calculatedSubtotal = 0;
        for (OrderItem item : order.getOrderItems()) {
            String pName = HtmlUtils.htmlEscape(item.getProductName());
            String vName = item.getVariantName() != null ? HtmlUtils.htmlEscape(item.getVariantName()) : "";
            int qty = item.getQuantity();
            long price = item.getPrice();
            long itemSubtotal = price * qty;
            calculatedSubtotal += itemSubtotal;

            sb.append("            <tr>");
            sb.append("              <td>");
            sb.append("                <div class='product-info'>").append(pName).append("</div>");
            if (!vName.isEmpty()) {
                sb.append("                <div class='product-variant'>").append(vName).append("</div>");
            }
            sb.append("              </td>");
            sb.append("              <td style='text-align: center;'>").append(qty).append("</td>");
            sb.append("              <td style='text-align: right;'>").append(formatCurrency(price)).append("</td>");
            sb.append("              <td style='text-align: right;'>").append(formatCurrency(itemSubtotal)).append("</td>");
            sb.append("            </tr>");
        }
        
        sb.append("          </tbody>");
        sb.append("        </table>");
        sb.append("      </div>");
        
        // Pricing Summary
        long discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : 0;
        long shipping = order.getShippingFee() != null ? order.getShippingFee() : 0;
        long total = order.getTotalPrice() != null ? order.getTotalPrice() : (calculatedSubtotal - discount + shipping);

        sb.append("      <div class='price-summary'>");
        sb.append("        <div class='price-row'>");
        sb.append("          <div class='price-label'>Tạm tính:</div>");
        sb.append("          <div class='price-value'>").append(formatCurrency(calculatedSubtotal)).append("</div>");
        sb.append("        </div>");
        if (discount > 0) {
            sb.append("        <div class='price-row'>");
            sb.append("          <div class='price-label'>Khuyến mãi:</div>");
            sb.append("          <div class='price-value' style='color: #dc2626;'>- ").append(formatCurrency(discount)).append("</div>");
            sb.append("          </div>");
        }
        sb.append("        <div class='price-row'>");
        sb.append("          <div class='price-label'>Phí vận chuyển:</div>");
        sb.append("          <div class='price-value'>").append(formatCurrency(shipping)).append("</div>");
        sb.append("        </div>");
        sb.append("        <div class='price-row total-row'>");
        sb.append("          <div class='price-label'>Tổng cộng thanh toán:</div>");
        sb.append("          <div class='price-value'>").append(formatCurrency(total)).append("</div>");
        sb.append("        </div>");
        sb.append("      </div>");
        
        sb.append("    </div>"); // end content
        
        // Footer
        sb.append("    <div class='footer'>");
        sb.append("      <p>Nexora Commerce — Hệ Thống E-Commerce Cao Cấp</p>");
        sb.append("      <p>Đây là email thông báo tự động. Vui lòng không phản hồi lại email này.</p>");
        sb.append("      <p>Mọi thắc mắc vui lòng liên hệ: support@nexoracommerce.com</p>");
        sb.append("    </div>");
        
        sb.append("  </div>"); // end container
        sb.append("</div>"); // end wrapper
        sb.append("</body>");
        sb.append("</html>");
        
        return sb.toString();
    }

    private String formatCurrency(long amount) {
        return String.format("%,d VNĐ", amount);
    }
}
