package com.nexoracommerce.review.service.impl;

import com.nexoracommerce.common.exception.BusinessLogicException;
import com.nexoracommerce.common.exception.ResourceNotFoundException;
import com.nexoracommerce.order.entity.Order;
import com.nexoracommerce.order.repository.OrderRepository;
import com.nexoracommerce.product.entity.ProductVariant;
import com.nexoracommerce.product.repository.ProductVariantRepository;
import com.nexoracommerce.review.dto.request.AdminReplyRequest;
import com.nexoracommerce.review.dto.request.ProductReviewRequest;
import com.nexoracommerce.review.dto.response.ProductReviewResponse;
import com.nexoracommerce.review.entity.ProductReview;
import com.nexoracommerce.review.entity.ReviewImage;
import com.nexoracommerce.review.mapper.ProductReviewMapper;
import com.nexoracommerce.review.repository.ProductReviewRepository;
import com.nexoracommerce.review.service.IProductReviewService;
import com.nexoracommerce.user.entity.User;
import com.nexoracommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductReviewServiceImpl implements IProductReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductReviewMapper reviewMapper;

    @Override
    public Page<ProductReviewResponse> getReviewsByVariantSku(String variantSku, Pageable pageable) {
        log.info("Fetching reviews for variant SKU: {}", variantSku);
        
        if (!productVariantRepository.existsById(variantSku)) {
            throw new ResourceNotFoundException("Product variant not found with SKU: " + variantSku);
        }

        Page<ProductReview> reviews = reviewRepository.findByVariantSkuAndParentIsNull(variantSku, pageable);
        return reviews.map(reviewMapper::toResponse);
    }

    @Override
    @Transactional
    public ProductReviewResponse createReview(String userId, ProductReviewRequest request) {
        log.info("Creating review for variant SKU: {} by user: {}", request.variantSku(), userId);
        UUID userUuid = UUID.fromString(userId);

        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        ProductVariant variant = productVariantRepository.findBySku(request.variantSku())
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with SKU: " + request.variantSku()));

        Order order = null;
        if (request.orderId() != null && !request.orderId().trim().isEmpty()) {
            Order fetchedOrder = orderRepository.findById(request.orderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.orderId()));

            // Verify order belongs to the user
            if (fetchedOrder.getUser() == null || !fetchedOrder.getUser().getId().equals(userUuid)) {
                throw new BusinessLogicException("Order does not belong to the user");
            }

            // Verify order contains the product variant
            boolean containsProduct = fetchedOrder.getOrderItems().stream()
                    .anyMatch(item -> item.getVariant().getSku().equals(request.variantSku()));
            if (!containsProduct) {
                throw new BusinessLogicException("Order does not contain the specified product variant: " + request.variantSku());
            }
            order = fetchedOrder;
        }

        ProductReview review = ProductReview.builder()
                .user(user)
                .variant(variant)
                .order(order)
                .rating(request.rating())
                .comment(request.comment())
                .build();

        if (request.imageUrls() != null && !request.imageUrls().isEmpty()) {
            List<ReviewImage> images = request.imageUrls().stream()
                    .map(url -> ReviewImage.builder()
                            .imageUrl(url)
                            .review(review)
                            .build())
                    .toList();
            review.setImages(images);
        }

        ProductReview saved = reviewRepository.save(review);
        return reviewMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ProductReviewResponse adminReply(String adminUserId, Long reviewId, AdminReplyRequest request) {
        log.info("Admin {} is replying to review ID: {}", adminUserId, reviewId);
        UUID adminUuid = UUID.fromString(adminUserId);

        User adminUser = userRepository.findById(adminUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found with id: " + adminUserId));

        ProductReview parentReview = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        if (parentReview.getParent() != null) {
            throw new BusinessLogicException("Cannot reply to a reply. Only top-level reviews can be replied to.");
        }

        ProductReview reply = ProductReview.builder()
                .user(adminUser)
                .variant(parentReview.getVariant())
                .rating(5) // Default rating for reply
                .comment(request.comment())
                .parent(parentReview)
                .build();

        parentReview.getReplies().add(reply);
        ProductReview savedParent = reviewRepository.save(parentReview);
        return reviewMapper.toResponse(savedParent);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        log.info("Deleting review ID: {}", reviewId);
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        reviewRepository.delete(review);
    }
}
