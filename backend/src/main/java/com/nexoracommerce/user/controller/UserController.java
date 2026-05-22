package com.nexoracommerce.user.controller;

import com.nexoracommerce.common.response.ApiResponse;
import com.nexoracommerce.user.dto.request.UpdateProfileRequest;
import com.nexoracommerce.user.dto.request.UserAddressRequest;
import com.nexoracommerce.user.dto.response.UserAddressResponse;
import com.nexoracommerce.user.dto.response.UserProfileResponse;
import com.nexoracommerce.user.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @GetMapping("/{userId}/profile")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #userId)")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(@PathVariable UUID userId) {
        UserProfileResponse response = userService.getUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{userId}/profile")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #userId)")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse response = userService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Profile updated successfully"));
    }

    @GetMapping("/{userId}/addresses")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #userId)")
    public ResponseEntity<ApiResponse<List<UserAddressResponse>>> getUserAddresses(@PathVariable UUID userId) {
        List<UserAddressResponse> response = userService.getUserAddresses(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{userId}/addresses")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #userId)")
    public ResponseEntity<ApiResponse<UserAddressResponse>> addUserAddress(
            @PathVariable UUID userId,
            @Valid @RequestBody UserAddressRequest request) {
        UserAddressResponse response = userService.addUserAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @PutMapping("/{userId}/addresses/{addressId}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #userId)")
    public ResponseEntity<ApiResponse<UserAddressResponse>> updateAddress(
            @PathVariable UUID userId,
            @PathVariable Long addressId,
            @Valid @RequestBody UserAddressRequest request) {
        UserAddressResponse response = userService.updateAddress(userId, addressId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Address updated successfully"));
    }

    @DeleteMapping("/{userId}/addresses/{addressId}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #userId)")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable UUID userId,
            @PathVariable Long addressId) {
        userService.deleteAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Address deleted successfully"));
    }
}
