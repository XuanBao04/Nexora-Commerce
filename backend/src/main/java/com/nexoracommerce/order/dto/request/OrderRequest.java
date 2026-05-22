package com.nexoracommerce.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import com.nexoracommerce.common.validator.ValidPhoneNumber;
import com.nexoracommerce.common.validator.ValidAddress;
import java.util.List;

public record OrderRequest(
    @NotBlank(message = "User ID is required")
    String userId,
    
    @NotEmpty(message = "Order items list cannot be empty")
    @Valid
    List<OrderItemRequest> orderItems,

    @Size(max = 50, message = "Coupon code must not exceed 50 characters")
    String couponCode,  // Optional coupon code

    @NotBlank(message = "Shipping address is required")
    @ValidAddress(message = "Shipping address must be between 5 and 255 characters")
    String shippingAddress,

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters")
    String city,

    @NotBlank(message = "District is required")
    @Size(min = 2, max = 100, message = "District must be between 2 and 100 characters")
    String district,

    @NotBlank(message = "Ward is required")
    @Size(min = 2, max = 100, message = "Ward must be between 2 and 100 characters")
    String ward,

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    String postalCode,  // Optional postal code

    @NotBlank(message = "Phone number is required")
    @ValidPhoneNumber(message = "Invalid phone number format. Expected Vietnamese phone number (0xxxxx or +84xxxx format)")
    String phoneNumber
) {}
