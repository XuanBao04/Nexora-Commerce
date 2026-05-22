package com.nexoracommerce.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpdateProfileRequest(
    @Size(max = 100, message = "Full name must be at most 100 characters")
    String fullName,

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must be at most 100 characters")
    String email,

    @Size(min = 6, message = "Password must be at least 6 characters")
    String password // Optional
) {}
