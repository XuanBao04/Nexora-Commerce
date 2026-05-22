package com.shopcart.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import com.shopcart.common.validator.ValidPhoneNumber;
import com.shopcart.common.validator.ValidAddress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequest {
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotEmpty(message = "Order items list cannot be empty")
    @Valid
    private List<OrderItemRequest> orderItems;

    @Size(max = 50, message = "Coupon code must not exceed 50 characters")
    private String couponCode;  // Optional coupon code

    @NotBlank(message = "Shipping address is required")
    @ValidAddress(message = "Shipping address must be between 5 and 255 characters")
    private String shippingAddress;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters")
    private String city;

    @NotBlank(message = "District is required")
    @Size(min = 2, max = 100, message = "District must be between 2 and 100 characters")
    private String district;

    @NotBlank(message = "Ward is required")
    @Size(min = 2, max = 100, message = "Ward must be between 2 and 100 characters")
    private String ward;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;  // Optional postal code

    @NotBlank(message = "Phone number is required")
    @ValidPhoneNumber(message = "Invalid phone number format. Expected Vietnamese phone number (0xxxxx or +84xxxx format)")
    private String phoneNumber;
}
