package com.nexoracommerce.user.service.impl;

import com.nexoracommerce.common.exception.BusinessLogicException;
import com.nexoracommerce.common.exception.ResourceNotFoundException;
import com.nexoracommerce.user.dto.request.UpdateProfileRequest;
import com.nexoracommerce.user.dto.request.UserAddressRequest;
import com.nexoracommerce.user.dto.response.UserAddressResponse;
import com.nexoracommerce.user.dto.response.UserProfileResponse;
import com.nexoracommerce.user.entity.User;
import com.nexoracommerce.user.entity.UserAddress;
import com.nexoracommerce.user.mapper.UserMapper;
import com.nexoracommerce.user.repository.UserAddressRepository;
import com.nexoracommerce.user.repository.UserRepository;
import com.nexoracommerce.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    public UserProfileResponse getUserProfile(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UUID id, UpdateProfileRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (request.fullName() != null && !request.fullName().trim().isEmpty()) {
            user.setFullName(request.fullName().trim());
        }

        if (request.email() != null && !request.email().trim().isEmpty()) {
            String newEmail = request.email().trim();
            if (!newEmail.equalsIgnoreCase(user.getEmail())) {
                if (userRepository.existsByEmail(newEmail)) {
                    throw new BusinessLogicException("Email is already in use by another user: " + newEmail);
                }
                user.setEmail(newEmail);
            }
        }

        if (request.password() != null && !request.password().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.password().trim()));
        }

        User updatedUser = userRepository.save(user);
        return userMapper.toProfileResponse(updatedUser);
    }

    @Override
    public List<UserAddressResponse> getUserAddresses(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return userAddressRepository.findByUserId(userId).stream()
                .map(userMapper::toAddressResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserAddressResponse addUserAddress(UUID userId, UserAddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<UserAddress> existingAddresses = userAddressRepository.findByUserId(userId);
        boolean isFirstAddress = existingAddresses.isEmpty();
        boolean makeDefault = isFirstAddress || Boolean.TRUE.equals(request.isDefault());

        if (makeDefault) {
            // Unset existing default address if any
            userAddressRepository.findByUserIdAndIsDefaultTrue(userId)
                    .ifPresent(addr -> {
                        addr.setIsDefault(false);
                        userAddressRepository.save(addr);
                    });
        }

        UserAddress address = UserAddress.builder()
                .user(user)
                .receiverName(request.receiverName().trim())
                .phoneNumber(request.phoneNumber().trim())
                .addressLine(request.addressLine().trim())
                .isDefault(makeDefault)
                .build();

        UserAddress savedAddress = userAddressRepository.save(address);
        return userMapper.toAddressResponse(savedAddress);
    }

    @Override
    @Transactional
    public UserAddressResponse updateAddress(UUID userId, Long addressId, UserAddressRequest request) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

        if (!address.getUser().getId().equals(userId)) {
            throw new BusinessLogicException("Address does not belong to user with id: " + userId);
        }

        boolean makeDefault = Boolean.TRUE.equals(request.isDefault());
        if (makeDefault && !Boolean.TRUE.equals(address.getIsDefault())) {
            // Unset existing default address if any
            userAddressRepository.findByUserIdAndIsDefaultTrue(userId)
                    .ifPresent(addr -> {
                        addr.setIsDefault(false);
                        userAddressRepository.save(addr);
                    });
            address.setIsDefault(true);
        } else if (!makeDefault && Boolean.TRUE.equals(address.getIsDefault())) {
            // Do not allow unsetting the default address if it's the only one
            List<UserAddress> existingAddresses = userAddressRepository.findByUserId(userId);
            if (existingAddresses.size() > 1) {
                // Set another address as default
                existingAddresses.stream()
                        .filter(addr -> !addr.getId().equals(addressId))
                        .findFirst()
                        .ifPresent(addr -> {
                            addr.setIsDefault(true);
                            userAddressRepository.save(addr);
                        });
                address.setIsDefault(false);
            }
        }

        address.setReceiverName(request.receiverName().trim());
        address.setPhoneNumber(request.phoneNumber().trim());
        address.setAddressLine(request.addressLine().trim());

        UserAddress updatedAddress = userAddressRepository.save(address);
        return userMapper.toAddressResponse(updatedAddress);
    }

    @Override
    @Transactional
    public void deleteAddress(UUID userId, Long addressId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

        if (!address.getUser().getId().equals(userId)) {
            throw new BusinessLogicException("Address does not belong to user with id: " + userId);
        }

        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        userAddressRepository.delete(address);

        if (wasDefault) {
            // Set another address as default if available
            List<UserAddress> remaining = userAddressRepository.findByUserId(userId);
            if (!remaining.isEmpty()) {
                UserAddress newDefault = remaining.get(0);
                newDefault.setIsDefault(true);
                userAddressRepository.save(newDefault);
            }
        }
    }
}
