package com.nexoracommerce.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import com.nexoracommerce.common.validator.ValidPhoneNumber;
import com.nexoracommerce.common.validator.ValidAddress;
import java.util.List;

/**
 * DTO for order request
 * Used when creating a new order with items and shipping details
 */
public record OrderRequest(
    @NotBlank(message = "User ID is required")
    String userId,
    
    @NotEmpty(message = "Order items list cannot be empty")
    @Valid
    List<OrderItemRequest> orderItems,

    @Size(max = 50, message = "Coupon code must not exceed 50 characters")
    String couponCode,                // Optional coupon code

    @NotBlank(message = "Shipping address is required")
    @ValidAddress(message = "Shipping address must be between 5 and 255 characters")
    String shippingAddress,

    @NotBlank(message = "Phone number is required")
    @ValidPhoneNumber(message = "Invalid phone number format. Expected Vietnamese phone number (0xxxxx or +84xxxx format)")
    String phoneNumber,
    
    @Size(max = 500, message = "Customer note must not exceed 500 characters")
    String customerNote               // Optional: Customer notes at checkout
) {}
