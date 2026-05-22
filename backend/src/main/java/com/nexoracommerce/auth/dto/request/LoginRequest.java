package com.nexoracommerce.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * Request DTO for user login
 */
@Builder
public record LoginRequest(
    @NotBlank(message = "Username is required")
    String username,

    @NotBlank(message = "Password is required")
    String password
) {}
