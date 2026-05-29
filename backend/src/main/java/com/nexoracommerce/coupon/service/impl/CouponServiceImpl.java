package com.nexoracommerce.coupon.service.impl;

import com.nexoracommerce.constant.MessageConstant;
import com.nexoracommerce.coupon.dto.request.CreateCouponRequest;
import com.nexoracommerce.coupon.dto.request.UpdateCouponRequest;
import com.nexoracommerce.coupon.dto.response.CouponResponse;
import com.nexoracommerce.coupon.entity.Coupon;
import com.nexoracommerce.coupon.mapper.CouponMapper;
import com.nexoracommerce.common.exception.BusinessLogicException;
import com.nexoracommerce.common.exception.ResourceNotFoundException;
import com.nexoracommerce.coupon.repository.CouponRepository;
import com.nexoracommerce.coupon.service.ICouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponServiceImpl implements ICouponService {
    
    private final CouponRepository couponRepository;
    private final CouponMapper couponMapper;
    
    private static final ZoneId VN_TIMEZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int MAX_DISCOUNT_PERCENT = 100;
    private static final int MIN_DISCOUNT_PERCENT = 1;

    // ======================== Coupon Validation ========================

    @Override
    public Coupon validateAndGetCoupon(String couponCode, Long orderAmount) {
        if (isBlankCouponCode(couponCode)) {
            return null;  // No coupon provided
        }

        validateOrderAmount(orderAmount);

        Coupon coupon = couponRepository.findById(couponCode)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstant.Coupon.NOT_FOUND + couponCode));

        validateCouponState(coupon);
        validateMinimumOrderAmount(coupon, orderAmount);

        return coupon;
    }

    @Override
    public boolean isCouponValid(String couponCode) {
        if (isBlankCouponCode(couponCode)) {
            return false;
        }

        try {
            Coupon coupon = couponRepository.findById(java.util.Objects.requireNonNull(couponCode)).orElse(null);

            if (coupon == null) {
                return false;
            }

            return isCouponActive(coupon) && !isCouponExpired(coupon);
        } catch (Exception e) {
            log.warn("Error validating coupon: {}", couponCode, e);
            return false;
        }
    }

    // ======================== Discount Calculation ========================

    @Override
    public Long calculateDiscount(String couponCode, Long orderAmount) {
        Coupon coupon = validateAndGetCoupon(couponCode, orderAmount);
        if (coupon == null) {
            return 0L;
        }

        long discountAmount = (orderAmount * coupon.getDiscountPercent()) / 100;
        
        // Cap discount at order amount to prevent negative final price
        return Math.min(discountAmount, orderAmount);
    }

    // ======================== CRUD Operations ========================

    @Override
    public Coupon getCouponByCode(String couponCode) {
        validateCouponCodeNotBlank(couponCode);
        
        return couponRepository.findById(java.util.Objects.requireNonNull(couponCode))
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstant.Coupon.NOT_FOUND + couponCode));
    }

    @Override
    public CouponResponse getCouponResponseByCode(String couponCode) {
        return couponMapper.toResponse(getCouponByCode(couponCode));
    }

    @Override
    @Transactional
    public CouponResponse createCoupon(CreateCouponRequest request) {
        Objects.requireNonNull(request, "Coupon request cannot be null");
        validateCouponCodeNotBlank(request.code());
        validateDiscountPercent(request.discountPercent());
        validateMinimumOrderAmount(request.minimumOrderAmount());
        if (request.expiryDate() != null) {
            validateExpiryDate(request.expiryDate());
        }

        Coupon coupon = couponMapper.toEntity(request);
        Coupon saved = couponRepository.save(java.util.Objects.requireNonNull(coupon));
        return couponMapper.toResponse(saved);
    }

    @Override
    public List<CouponResponse> getAllCoupons() {
        return couponMapper.toResponseList(couponRepository.findAll());
    }

    @Override
    @Transactional
    public CouponResponse updateCoupon(String code, UpdateCouponRequest request) {
        Objects.requireNonNull(request, "Coupon update request data cannot be null");
        validateCouponCodeNotBlank(code);

        Coupon existing = couponRepository.findById(java.util.Objects.requireNonNull(code))
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstant.Coupon.NOT_FOUND + code));

        if (request.discountPercent() != null) {
            validateDiscountPercent(request.discountPercent());
        }
        if (request.minimumOrderAmount() != null) {
            validateMinimumOrderAmount(request.minimumOrderAmount());
        }
        if (request.expiryDate() != null) {
            validateExpiryDate(request.expiryDate());
        }

        couponMapper.updateEntityFromRequest(request, existing);

        Coupon saved = couponRepository.save(java.util.Objects.requireNonNull(existing));
        return couponMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCoupon(String code) {
        validateCouponCodeNotBlank(code);

        if (!couponRepository.existsById(java.util.Objects.requireNonNull(code))) {
            throw new ResourceNotFoundException(MessageConstant.Coupon.NOT_FOUND + code);
        }

        couponRepository.deleteById(java.util.Objects.requireNonNull(code));
        log.info("Deleted coupon: {}", code);
    }

    // ======================== Private Validation Methods ========================

    /**
     * Validate coupon code is not null or empty
     */
    private void validateCouponCodeNotBlank(String couponCode) {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            throw new BusinessLogicException(MessageConstant.Coupon.CODE_REQUIRED);
        }
    }

    /**
     * Check if coupon code is blank
     */
    private boolean isBlankCouponCode(String couponCode) {
        return couponCode == null || couponCode.trim().isEmpty();
    }

    /**
     * Validate discount percent is within valid range
     */
    private void validateDiscountPercent(Integer percent) {
        if (percent == null || percent < MIN_DISCOUNT_PERCENT || percent > MAX_DISCOUNT_PERCENT) {
            throw new BusinessLogicException(
                String.format(MessageConstant.Coupon.DISCOUNT_RANGE, MIN_DISCOUNT_PERCENT, MAX_DISCOUNT_PERCENT)
            );
        }
    }

    /**
     * Validate order amount is not null and positive
     */
    private void validateOrderAmount(Long orderAmount) {
        if (orderAmount == null || orderAmount <= 0) {
            throw new BusinessLogicException(MessageConstant.Coupon.INVALID_ORDER_AMOUNT);
        }
    }

    /**
     * Validate minimum order amount is not negative
     */
    private void validateMinimumOrderAmount(Long minAmount) {
        if (minAmount != null && minAmount < 0) {
            throw new BusinessLogicException(MessageConstant.Coupon.MIN_ORDER_AMOUNT_NEGATIVE);
        }
    }

    /**
     * Validate coupon meets minimum order amount requirement
     */
    private void validateMinimumOrderAmount(Coupon coupon, Long orderAmount) {
        if (orderAmount < coupon.getMinimumOrderAmount()) {
            throw new BusinessLogicException(
                String.format(MessageConstant.Coupon.MIN_ORDER_REQUIRED,
                    coupon.getMinimumOrderAmount(), orderAmount)
            );
        }
    }

    /**
     * Validate coupon state (active and not expired)
     */
    private void validateCouponState(Coupon coupon) {
        if (!isCouponActive(coupon)) {
            throw new BusinessLogicException(MessageConstant.Coupon.INACTIVE + coupon.getCode());
        }

        if (isCouponExpired(coupon)) {
            throw new BusinessLogicException(MessageConstant.Coupon.EXPIRED + coupon.getCode());
        }
    }

    /**
     * Check if coupon is active
     */
    private boolean isCouponActive(Coupon coupon) {
        return coupon.getActive() != null && coupon.getActive();
    }

    /**
     * Check if coupon is expired
     */
    private boolean isCouponExpired(Coupon coupon) {
        if (coupon.getExpiryDate() == null) {
            return false;
        }
        return getCurrentTime().isAfter(coupon.getExpiryDate());
    }

    /**
     * Validate expiry date is not in the past
     */
    private void validateExpiryDate(LocalDateTime expiryDate) {
        if (expiryDate != null && expiryDate.isBefore(getCurrentTime())) {
            throw new BusinessLogicException(MessageConstant.Coupon.FUTURE_EXPIRY);
        }
    }

    /**
     * Get current time in Vietnam timezone
     */
    private LocalDateTime getCurrentTime() {
        return LocalDateTime.now(VN_TIMEZONE);
    }
}
