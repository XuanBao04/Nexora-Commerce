package com.nexoracommerce.auth.service;

import com.nexoracommerce.auth.dto.request.LoginRequest;
import com.nexoracommerce.auth.dto.request.RegisterRequest;
import com.nexoracommerce.auth.dto.response.AuthResponse;

/**
 * Service interface for Authentication operations
 */
public interface AuthService {
    
    AuthResponse login(LoginRequest request);

    AuthResponse register(RegisterRequest request);

    AuthResponse getCurrentUser(String username);

    AuthResponse refreshToken(String refreshToken);

    void logout(String refreshToken);
}
