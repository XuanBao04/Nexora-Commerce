package com.nexoracommerce.auth.service.impl;

import com.nexoracommerce.auth.dto.request.LoginRequest;
import com.nexoracommerce.auth.dto.request.RegisterRequest;
import com.nexoracommerce.auth.dto.response.AuthResponse;
import com.nexoracommerce.auth.service.AuthService;
import com.nexoracommerce.common.exception.BusinessLogicException;
import com.nexoracommerce.common.exception.InvalidInputException;
import com.nexoracommerce.common.exception.ResourceNotFoundException;
import com.nexoracommerce.config.security.JwtService;
import com.nexoracommerce.constant.MessageConstant;
import com.nexoracommerce.user.entity.Role;
import com.nexoracommerce.user.entity.User;
import com.nexoracommerce.user.repository.RoleRepository;
import com.nexoracommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

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

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // Lưu refresh token vào Redis với TTL
        String redisKey = "refresh_token:" + refreshToken;
        redisTemplate.opsForValue().set(redisKey, user.getUsername(), refreshTokenExpirationMs, TimeUnit.MILLISECONDS);

        return toAuthResponse(user, MessageConstant.Auth.LOGIN_SUCCESS, accessToken, refreshToken);
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessLogicException(MessageConstant.Auth.USERNAME_EXISTS + request.username());
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
                .roles(Set.of(customerRole))
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

        // Kiểm tra token tồn tại trong Redis (chống revoke)
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
            redisTemplate.delete(redisKey);
            throw new InvalidInputException("Refresh token is expired or invalid");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newAccessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        // Xóa token cũ, lưu token mới (Token Rotation)
        redisTemplate.delete(redisKey);
        String newRedisKey = "refresh_token:" + newRefreshToken;
        redisTemplate.opsForValue().set(newRedisKey, username, refreshTokenExpirationMs, TimeUnit.MILLISECONDS);

        return toAuthResponse(user, "Token refreshed successfully", newAccessToken, newRefreshToken);
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isEmpty()) {
            redisTemplate.delete("refresh_token:" + refreshToken);
        }
    }

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
}
