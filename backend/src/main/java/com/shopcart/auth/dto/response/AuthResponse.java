package com.shopcart.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for authentication operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String userId;
    private String username;
    private String fullName;
    private String email;
    private String role;
    private String message;
    private String token;
    
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String refreshToken;
}
