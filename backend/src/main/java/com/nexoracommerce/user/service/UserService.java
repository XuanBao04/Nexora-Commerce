package com.nexoracommerce.user.service;

import com.nexoracommerce.user.dto.request.UpdateProfileRequest;
import com.nexoracommerce.user.dto.request.UserAddressRequest;
import com.nexoracommerce.user.dto.response.UserAddressResponse;
import com.nexoracommerce.user.dto.response.UserProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface IUserService {
    UserProfileResponse getUserProfile(UUID id);
    UserProfileResponse updateProfile(UUID id, UpdateProfileRequest request);
    List<UserAddressResponse> getUserAddresses(UUID userId);
    UserAddressResponse addUserAddress(UUID userId, UserAddressRequest request);
    UserAddressResponse updateAddress(UUID userId, Long addressId, UserAddressRequest request);
    void deleteAddress(UUID userId, Long addressId);
    Page<UserProfileResponse> getAllUsers(String keyword, Pageable pageable);
}
