package com.nexoracommerce.wishlist.service.impl;

import com.nexoracommerce.common.exception.BusinessLogicException;
import com.nexoracommerce.common.exception.ResourceNotFoundException;
import com.nexoracommerce.product.entity.Product;
import com.nexoracommerce.product.repository.ProductRepository;
import com.nexoracommerce.user.entity.User;
import com.nexoracommerce.user.repository.UserRepository;
import com.nexoracommerce.wishlist.dto.request.WishlistRequest;
import com.nexoracommerce.wishlist.dto.response.WishlistResponse;
import com.nexoracommerce.wishlist.entity.Wishlist;
import com.nexoracommerce.wishlist.mapper.WishlistMapper;
import com.nexoracommerce.wishlist.repository.WishlistRepository;
import com.nexoracommerce.wishlist.service.IWishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistServiceImpl implements IWishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final WishlistMapper wishlistMapper;

    @Override
    public Page<WishlistResponse> getWishlistByUserId(String userId, Pageable pageable) {
        log.info("Fetching wishlist for user: {}", userId);
        UUID userUuid = UUID.fromString(userId);
        
        // Ensure user exists
        if (!userRepository.existsById(userUuid)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        Page<Wishlist> wishlistPage = wishlistRepository.findByUserId(userUuid, pageable);
        return wishlistPage.map(wishlistMapper::toResponse);
    }

    @Override
    @Transactional
    public WishlistResponse addToWishlist(String userId, WishlistRequest request) {
        log.info("Adding product {} to wishlist for user: {}", request.productId(), userId);
        UUID userUuid = UUID.fromString(userId);

        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));

        if (wishlistRepository.existsByUserIdAndProductId(userUuid, request.productId())) {
            throw new BusinessLogicException("Product already exists in the wishlist");
        }

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();

        Wishlist saved = wishlistRepository.save(wishlist);
        return wishlistMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void removeFromWishlist(String userId, String productId) {
        log.info("Removing product {} from wishlist for user: {}", productId, userId);
        UUID userUuid = UUID.fromString(userId);

        // Ensure user exists
        if (!userRepository.existsById(userUuid)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        Wishlist wishlist = wishlistRepository.findByUserIdAndProductId(userUuid, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + productId + " is not in the wishlist of user " + userId));

        wishlistRepository.delete(wishlist);
    }
}
