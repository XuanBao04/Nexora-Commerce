package com.nexoracommerce.auth.controller;

import com.nexoracommerce.auth.dto.request.LoginRequest;
import com.nexoracommerce.auth.dto.request.RegisterRequest;
import com.nexoracommerce.auth.dto.response.AuthResponse;
import com.nexoracommerce.auth.service.IAuthService;
import com.nexoracommerce.common.annotation.RateLimited;
import com.nexoracommerce.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/authentications")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    private final IAuthService authService;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    @Value("${jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @RateLimited(maxRequests = 5, windowSeconds = 60)
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        setRefreshTokenCookie(response, authResponse.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(authResponse, "Login successful"));
    }

    @RateLimited(maxRequests = 5, windowSeconds = 60)
    @PostMapping("/registrations")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse authResponse = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(authResponse));
    }

    @PostMapping("/tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.refreshToken(refreshToken);
        setRefreshTokenCookie(response, authResponse.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(authResponse, "Token refreshed successfully"));
    }

    @GetMapping("/profiles/current")
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser(Authentication authentication) {
        AuthResponse authResponse = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(authResponse));
    }

    @DeleteMapping("/sessions")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken);
        SecurityContextHolder.clearContext();
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.ok(null, "Logout successful. Please remove access token on client side."));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        long maxAgeSeconds = refreshTokenExpirationMs / 1000;
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
