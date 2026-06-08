package com.nexoracommerce.user.service;

import com.nexoracommerce.user.dto.request.UpdateProfileRequest;
import com.nexoracommerce.user.dto.request.UserAddressRequest;
import com.nexoracommerce.user.dto.response.UserAddressResponse;
import com.nexoracommerce.user.dto.response.UserProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface UserService {
    /**
     * Lấy thông tin hồ sơ người dùng
     */
    UserProfileResponse getUserProfile(UUID id);

    /**
     * Cập nhật thông tin hồ sơ người dùng
     */
    UserProfileResponse updateProfile(UUID id, UpdateProfileRequest request);

    /**
     * Lấy danh sách địa chỉ của người dùng
     */
    List<UserAddressResponse> getUserAddresses(UUID userId);

    /**
     * Thêm địa chỉ mới cho người dùng
     */
    UserAddressResponse addUserAddress(UUID userId, UserAddressRequest request);

    /**
     * Cập nhật địa chỉ hiện có
     */
    UserAddressResponse updateAddress(UUID userId, Long addressId, UserAddressRequest request);

    /**
     * Xóa địa chỉ của người dùng
     */
    void deleteAddress(UUID userId, Long addressId);

    /**
     * Lấy danh sách tất cả người dùng (hỗ trợ tìm kiếm và phân trang cho Admin)
     */
    Page<UserProfileResponse> getAllUsers(String keyword, Pageable pageable);
}
