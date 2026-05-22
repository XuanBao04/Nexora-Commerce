package com.nexoracommerce.checkout.controller;

import com.nexoracommerce.checkout.dto.PaymentRequest;
import com.nexoracommerce.checkout.dto.PaymentResponse;
import com.nexoracommerce.checkout.service.ICheckoutService;
import com.nexoracommerce.common.response.ApiResponse;
import com.nexoracommerce.order.dto.request.OrderRequest;
import com.nexoracommerce.order.dto.response.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkouts")
@RequiredArgsConstructor
public class CheckoutController {

    private final ICheckoutService checkoutService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwner(authentication, #request.userId())")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(@Valid @RequestBody OrderRequest request) {
        OrderResponse order = checkoutService.checkoutWithRedisProtection(request, request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(order));
    }

    @PostMapping("/orders/{orderId}/payments")
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwnerOfOrder(authentication, #orderId)")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @PathVariable String orderId,
            @Valid @RequestBody PaymentRequest paymentRequest) {
        PaymentResponse response = checkoutService.processPayment(orderId, paymentRequest.successful());
        return ResponseEntity.ok(ApiResponse.ok(response, response.message()));
    }
}
