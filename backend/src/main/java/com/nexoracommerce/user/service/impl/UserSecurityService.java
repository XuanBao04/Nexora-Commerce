package com.nexoracommerce.user.service.impl;

import com.nexoracommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("userSecurity")
@RequiredArgsConstructor
public class UserSecurityService {

    private final UserRepository userRepository;

    public boolean isOwner(Authentication authentication, String userId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .map(user -> String.valueOf(user.getId()).equals(userId))
                .orElse(false);
    }

    public boolean isOwner(Authentication authentication, UUID userId) {
        if (userId == null) {
            return false;
        }
        return isOwner(authentication, userId.toString());
    }
}
