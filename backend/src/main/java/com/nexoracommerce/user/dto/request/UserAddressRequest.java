package com.nexoracommerce.user.dto.request;

import com.nexoracommerce.common.validator.ValidPhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UserAddressRequest(
    @NotBlank(message = "Receiver name is required")
    @Size(max = 100, message = "Receiver name must be at most 100 characters")
    String receiverName,

    @NotBlank(message = "Phone number is required")
    @Size(max = 20, message = "Phone number must be at most 20 characters")
    @ValidPhoneNumber
    String phoneNumber,

    @NotBlank(message = "Address line is required")
    @Size(max = 255, message = "Address line must be at most 255 characters")
    String addressLine,

    Boolean isDefault
) {}
