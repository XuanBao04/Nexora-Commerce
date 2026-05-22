package com.nexoracommerce.auth.dto.response;

import lombok.Builder;

/**
 * Response DTO for authentication operations
 */
@Builder
public record AuthResponse(
    String userId,
    String username,
    String fullName,
    String email,
    String role,
    String message,
    String token,
    
    @com.fasterxml.jackson.annotation.JsonIgnore
    String refreshToken
) {}
