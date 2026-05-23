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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/authentications")
@RequiredArgsConstructor
@Tag(name = "Authentication Module", description = "Endpoints for user session login, registration, token refresh, and profiles")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    private final IAuthService authService;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    @Value("${jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @RateLimited(maxRequests = 5, windowSeconds = 60)
    @PostMapping("/sessions")
    @Operation(
        summary = "User session login",
        description = "Authenticates user credentials. Returns JWT access token in the response and sets a secure HttpOnly refresh token cookie."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful, tokens generated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid validation parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials provided")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        setRefreshTokenCookie(response, authResponse.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(authResponse, "Login successful"));
    }

    @RateLimited(maxRequests = 5, windowSeconds = 60)
    @PostMapping("/registrations")
    @Operation(
        summary = "Register a new user account",
        description = "Creates a new user in the system with default roles."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Registration successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation parameters failed or email already registered")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse authResponse = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(authResponse));
    }

    @PostMapping("/tokens")
    @Operation(
        summary = "Refresh JWT access token",
        description = "Generates a new JWT access token using the HttpOnly refresh token cookie."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh token missing, invalid, or expired")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.refreshToken(refreshToken);
        setRefreshTokenCookie(response, authResponse.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(authResponse, "Token refreshed successfully"));
    }

    @GetMapping("/profiles/current")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Get current authenticated user profile",
        description = "Requires a valid JWT token. Fetches current session's authenticated user details."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile details retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser(Authentication authentication) {
        AuthResponse authResponse = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(authResponse));
    }

    @DeleteMapping("/sessions")
    @Operation(
        summary = "User logout",
        description = "Revokes user refresh token and clears the secure HttpOnly cookie."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logout successful")
    })
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
