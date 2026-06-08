package com.nexoracommerce.payment.controller;
import com.nexoracommerce.checkout.service.CheckoutService;
import com.nexoracommerce.payment.config.VnPayConfig;
import com.nexoracommerce.payment.service.VnPayService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.bind.annotation.*;
import com.nexoracommerce.payment.enums.PaymentMethod;


import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Module", description = "Endpoints for handling payment gateway webhooks (IPN)")
public class PaymentController {

    private final VnPayService vnPayService;
    private final VnPayConfig vnPayConfig;
    private final CheckoutService checkoutService;

    @GetMapping("/vnpay/return")
    public ResponseEntity<Void> vnpayReturn(
            @RequestParam Map<String, String> queryParams,
            HttpServletRequest request) {
        log.info("Received VNPAY return: {}", queryParams);

        String result;
        try {
            result = processVnpayCallback(queryParams) ? "SUCCESS" : "FAILED";
        } catch (Exception e) {
            log.error("Error processing VNPAY return", e);
            result = "FAILED";
        }

        UriComponentsBuilder redirectBuilder = UriComponentsBuilder.fromUriString(vnPayConfig.getFrontendReturnUrl());
        if (request.getQueryString() != null) {
            redirectBuilder.query(request.getQueryString());
        }

        URI redirectUri = redirectBuilder
                .queryParam("nexora_Result", result)
                .build(true)
                .toUri();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(redirectUri)
                .build();
    }

    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> queryParams) {
        log.info("Received VNPAY IPN: {}", queryParams);
        try {
            boolean processed = processVnpayCallback(queryParams);
            return processed
                    ? ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"))
                    : ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid signature"));
        } catch (Exception e) {
            log.error("Error processing VNPAY IPN", e);
            return ResponseEntity.ok(Map.of("RspCode", "99", "Message", "Unknown error"));
        }
    }

    private boolean processVnpayCallback(Map<String, String> queryParams) {
        Map<String, String> signatureParams = new HashMap<>(queryParams);
        boolean isValidSignature = vnPayService.verifySignature(signatureParams);
        if (!isValidSignature) {
            log.warn("Invalid VNPAY signature");
            return false;
        }

        String orderId = queryParams.get("vnp_TxnRef");
        String responseCode = queryParams.get("vnp_ResponseCode");
        String transactionStatus = queryParams.get("vnp_TransactionStatus");
        String transactionNo = queryParams.get("vnp_TransactionNo");
        Long amount = parseVnpayAmount(queryParams.get("vnp_Amount"));
        boolean paymentSuccessful = "00".equals(responseCode) && "00".equals(transactionStatus);

        checkoutService.processPayment(orderId, paymentSuccessful, transactionNo, amount, PaymentMethod.VNPAY);
        return true;
    }

    private Long parseVnpayAmount(String amountValue) {
        try {
            return amountValue != null ? Long.parseLong(amountValue) / 100 : null;
        } catch (NumberFormatException e) {
            log.warn("Invalid vnp_Amount format: {}", amountValue);
            return null;
        }
    }
}
