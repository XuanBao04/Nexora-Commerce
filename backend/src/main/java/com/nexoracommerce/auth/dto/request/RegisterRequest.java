package com.nexoracommerce.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Request DTO for user registration
 */
@Builder
public record RegisterRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    String username,

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    String password,

    @Size(max = 100, message = "Full name must be at most 100 characters")
    String fullName,

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must be at most 100 characters")
    String email
) {}
