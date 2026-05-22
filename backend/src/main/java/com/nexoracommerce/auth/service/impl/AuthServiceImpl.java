package com.nexoracommerce.auth.service.impl;

import com.nexoracommerce.constant.MessageConstant;

import com.nexoracommerce.auth.dto.request.LoginRequest;
import com.nexoracommerce.auth.dto.request.RegisterRequest;
import com.nexoracommerce.auth.dto.response.AuthResponse;
import com.nexoracommerce.user.entity.User;
import com.nexoracommerce.common.exception.BusinessLogicException;
import com.nexoracommerce.common.exception.InvalidInputException;
import com.nexoracommerce.common.exception.ResourceNotFoundException;
import com.nexoracommerce.user.repository.UserRepository;
import com.nexoracommerce.user.repository.RoleRepository;
import com.nexoracommerce.user.entity.Role;
import com.nexoracommerce.auth.service.IAuthService;
import com.nexoracommerce.config.security.JwtService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for Authentication operations
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements IAuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResourceNotFoundException(
                        MessageConstant.Auth.USER_NOT_FOUND + request.username()));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidInputException(MessageConstant.Auth.INVALID_PASSWORD);
        }

        // Generate JWT tokens
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // Store refresh token in Redis with TTL
        String redisKey = "refresh_token:" + refreshToken;
        redisTemplate.opsForValue().set(redisKey, user.getUsername(), refreshTokenExpirationMs, java.util.concurrent.TimeUnit.MILLISECONDS);

        return toAuthResponse(user, MessageConstant.Auth.LOGIN_SUCCESS, accessToken, refreshToken);
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessLogicException(
                    MessageConstant.Auth.USERNAME_EXISTS + request.username());
        }

        if (request.email() != null && userRepository.existsByEmail(request.email())) {
            throw new BusinessLogicException("Email already exists: " + request.email());
        }

        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_CUSTOMER").build()));

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .email(request.email())
                .roles(java.util.Set.of(customerRole))
                .build();

        userRepository.save(user);

        return toAuthResponse(user, MessageConstant.Auth.REGISTER_SUCCESS);
    }

    @Override
    public AuthResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        MessageConstant.Auth.USER_NOT_FOUND + username));

        return toAuthResponse(user, MessageConstant.Auth.USER_INFO_RETRIEVED);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new InvalidInputException("Refresh token is missing");
        }

        // Validate that this refresh token exists in Redis (revocation check)
        String redisKey = "refresh_token:" + refreshToken;
        String cachedUsername = (String) redisTemplate.opsForValue().get(redisKey);
        if (cachedUsername == null) {
            throw new InvalidInputException("Refresh token has been revoked or expired");
        }

        String username = jwtService.extractUsername(refreshToken);
        if (username == null || !username.equals(cachedUsername)) {
            throw new InvalidInputException("Invalid refresh token");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            // Cleanup invalid/expired token in Redis
            redisTemplate.delete(redisKey);
            throw new InvalidInputException("Refresh token is expired or invalid");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newAccessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        // Delete old refresh token from Redis (Rotation)
        redisTemplate.delete(redisKey);

        // Store new refresh token in Redis with TTL
        String newRedisKey = "refresh_token:" + newRefreshToken;
        redisTemplate.opsForValue().set(newRedisKey, username, refreshTokenExpirationMs, java.util.concurrent.TimeUnit.MILLISECONDS);

        return toAuthResponse(user, "Token refreshed successfully", newAccessToken, newRefreshToken);
    }

    /**
     * Convert User entity to AuthResponse DTO
     */
    private AuthResponse toAuthResponse(User user, String message) {
        return toAuthResponse(user, message, null, null);
    }

    private AuthResponse toAuthResponse(User user, String message, String token, String refreshToken) {
        String roleStr = user.getRoles().stream()
                .map(Role::getName)
                .findFirst()
                .orElse("ROLE_CUSTOMER");

        return AuthResponse.builder()
                .userId(user.getId() != null ? user.getId().toString() : null)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(roleStr)
                .message(message)
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isEmpty()) {
            String redisKey = "refresh_token:" + refreshToken;
            redisTemplate.delete(redisKey);
        }
    }
}
