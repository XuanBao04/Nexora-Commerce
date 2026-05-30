package com.nexoracommerce.payment.controller;

import com.nexoracommerce.common.response.ApiResponse;
import com.nexoracommerce.checkout.service.ICheckoutService;
import com.nexoracommerce.payment.service.VnPayService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Module", description = "Endpoints for handling payment gateway webhooks (IPN)")
public class PaymentController {

    private final VnPayService vnPayService;
    private final ICheckoutService checkoutService;

    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> queryParams) {
        log.info("Received VNPAY IPN: {}", queryParams);
        try {
            boolean isValidSignature = vnPayService.verifySignature(queryParams);
            if (!isValidSignature) {
                log.warn("Invalid VNPAY signature");
                return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid signature"));
            }

            String orderId = queryParams.get("vnp_TxnRef");
            String responseCode = queryParams.get("vnp_ResponseCode");
            String transactionNo = queryParams.get("vnp_TransactionNo");
            Long amount = null;
            try {
                String amountStr = queryParams.get("vnp_Amount");
                if (amountStr != null) amount = Long.parseLong(amountStr) / 100;
            } catch (Exception e) {
                log.warn("Invalid vnp_Amount format");
            }

            if ("00".equals(responseCode)) {
                // Success
                checkoutService.processPayment(orderId, true, transactionNo, amount, com.nexoracommerce.order.enums.PaymentMethod.VNPAY);
                return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
            } else {
                // Failed or cancelled
                checkoutService.processPayment(orderId, false, transactionNo, amount, com.nexoracommerce.order.enums.PaymentMethod.VNPAY);
                return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
            }
        } catch (Exception e) {
            log.error("Error processing VNPAY IPN", e);
            return ResponseEntity.ok(Map.of("RspCode", "99", "Message", "Unknown error"));
        }
    }
}
