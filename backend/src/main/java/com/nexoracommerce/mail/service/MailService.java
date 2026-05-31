package com.nexoracommerce.mail.service;

public interface MailService {
    /**
     * Sends a rich HTML email confirmation with order details asynchronously.
     *
     * @param orderId the ID of the order to confirm
     */
    void sendOrderConfirmationEmail(String orderId);
}
