package com.nexoracommerce.review.service.impl;

import com.nexoracommerce.review.entity.ProductReview;
import com.nexoracommerce.review.repository.ProductReviewRepository;
import com.nexoracommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("reviewSecurity")
@RequiredArgsConstructor
public class ReviewSecurityService {

    private final UserRepository userRepository;
    private final ProductReviewRepository reviewRepository;

    public boolean isReviewOwner(Authentication authentication, Long reviewId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Optional<ProductReview> reviewOpt = reviewRepository.findById(reviewId);
        if (reviewOpt.isEmpty()) {
            return true; // Let the service layer throw ResourceNotFoundException instead of AccessDeniedException
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .map(user -> reviewOpt.get().getUser().getId().equals(user.getId()))
                .orElse(false);
    }
}
