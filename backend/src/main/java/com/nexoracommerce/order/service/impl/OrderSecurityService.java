package com.nexoracommerce.order.service.impl;

import com.nexoracommerce.order.repository.OrderRepository;

import com.nexoracommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;


@Service("orderSecurity")
@RequiredArgsConstructor
public class OrderSecurityService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    
    public boolean isOwner(Authentication authentication, String userId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .map(user -> String.valueOf(user.getId()).equals(userId))
                .orElse(false);
    }

    public boolean isOwnerOfOrder(Authentication authentication, String orderId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        java.util.Optional<com.nexoracommerce.order.entity.Order> orderOpt = orderRepository.findById(java.util.Objects.requireNonNull(orderId));
        if (orderOpt.isEmpty()) {
            return true;
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .map(user -> orderOpt.get().getUser().getId().equals(user.getId()))
                .orElse(false);
    }
}
