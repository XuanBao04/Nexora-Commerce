package com.nexoracommerce.user.dto.response;

import lombok.Builder;

@Builder
public record UserAddressResponse(
    Long id,
    String receiverName,
    String phoneNumber,
    String addressLine,
    Boolean isDefault
) {}
