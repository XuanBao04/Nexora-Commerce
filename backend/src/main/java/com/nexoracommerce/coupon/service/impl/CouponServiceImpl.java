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
import com.nexoracommerce.coupon.service.CouponService;
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
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponMapper couponMapper;

    private static final ZoneId VN_TIMEZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int MAX_DISCOUNT_PERCENT = 100;
    private static final int MIN_DISCOUNT_PERCENT = 1;

    @Override
    public Coupon validateAndGetCoupon(String couponCode, Long orderAmount) {
        if (isBlank(couponCode)) return null;

        validateOrderAmount(orderAmount);

        Coupon coupon = couponRepository.findById(couponCode)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstant.Coupon.NOT_FOUND + couponCode));

        validateCouponState(coupon);
        validateMinimumOrderAmount(coupon, orderAmount);

        return coupon;
    }

    @Override
    public boolean isCouponValid(String couponCode) {
        if (isBlank(couponCode)) return false;

        try {
            Coupon coupon = couponRepository.findById(Objects.requireNonNull(couponCode)).orElse(null);
            if (coupon == null) return false;
            return isCouponActive(coupon) && !isCouponExpired(coupon);
        } catch (Exception e) {
            log.warn("Error validating coupon: {}", couponCode, e);
            return false;
        }
    }

    @Override
    public Long calculateDiscount(String couponCode, Long orderAmount) {
        Coupon coupon = validateAndGetCoupon(couponCode, orderAmount);
        if (coupon == null) return 0L;

        long discountAmount = (orderAmount * coupon.getDiscountPercent()) / 100;
        return Math.min(discountAmount, orderAmount);
    }

    @Override
    public Coupon getCouponByCode(String couponCode) {
        validateCouponCodeNotBlank(couponCode);
        return couponRepository.findById(Objects.requireNonNull(couponCode))
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
        return couponMapper.toResponse(couponRepository.save(Objects.requireNonNull(coupon)));
    }

    @Override
    public List<CouponResponse> getAllCoupons() {
        return couponMapper.toResponseList(couponRepository.findAll());
    }

    @Override
    @Transactional
    public CouponResponse updateCoupon(String code, UpdateCouponRequest request) {
        Objects.requireNonNull(request, "Coupon update request cannot be null");
        validateCouponCodeNotBlank(code);

        Coupon existing = couponRepository.findById(Objects.requireNonNull(code))
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstant.Coupon.NOT_FOUND + code));

        if (request.discountPercent() != null) validateDiscountPercent(request.discountPercent());
        if (request.minimumOrderAmount() != null) validateMinimumOrderAmount(request.minimumOrderAmount());
        if (request.expiryDate() != null) validateExpiryDate(request.expiryDate());

        couponMapper.updateEntityFromRequest(request, existing);
        return couponMapper.toResponse(couponRepository.save(existing));
    }

    @Override
    @Transactional
    public void deleteCoupon(String code) {
        validateCouponCodeNotBlank(code);
        if (!couponRepository.existsById(Objects.requireNonNull(code))) {
            throw new ResourceNotFoundException(MessageConstant.Coupon.NOT_FOUND + code);
        }
        couponRepository.deleteById(code);
        log.info("Deleted coupon: {}", code);
    }

    private void validateCouponCodeNotBlank(String couponCode) {
        if (isBlank(couponCode)) {
            throw new BusinessLogicException(MessageConstant.Coupon.CODE_REQUIRED);
        }
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private void validateDiscountPercent(Integer percent) {
        if (percent == null || percent < MIN_DISCOUNT_PERCENT || percent > MAX_DISCOUNT_PERCENT) {
            throw new BusinessLogicException(
                String.format(MessageConstant.Coupon.DISCOUNT_RANGE, MIN_DISCOUNT_PERCENT, MAX_DISCOUNT_PERCENT));
        }
    }

    private void validateOrderAmount(Long orderAmount) {
        if (orderAmount == null || orderAmount <= 0) {
            throw new BusinessLogicException(MessageConstant.Coupon.INVALID_ORDER_AMOUNT);
        }
    }

    private void validateMinimumOrderAmount(Long minAmount) {
        if (minAmount != null && minAmount < 0) {
            throw new BusinessLogicException(MessageConstant.Coupon.MIN_ORDER_AMOUNT_NEGATIVE);
        }
    }

    private void validateMinimumOrderAmount(Coupon coupon, Long orderAmount) {
        if (orderAmount < coupon.getMinimumOrderAmount()) {
            throw new BusinessLogicException(
                String.format(MessageConstant.Coupon.MIN_ORDER_REQUIRED,
                    coupon.getMinimumOrderAmount(), orderAmount));
        }
    }

    private void validateCouponState(Coupon coupon) {
        if (!isCouponActive(coupon)) {
            throw new BusinessLogicException(MessageConstant.Coupon.INACTIVE + coupon.getCode());
        }
        if (isCouponExpired(coupon)) {
            throw new BusinessLogicException(MessageConstant.Coupon.EXPIRED + coupon.getCode());
        }
    }

    private boolean isCouponActive(Coupon coupon) {
        return coupon.getActive() != null && coupon.getActive();
    }

    private boolean isCouponExpired(Coupon coupon) {
        if (coupon.getExpiryDate() == null) return false;
        return LocalDateTime.now(VN_TIMEZONE).isAfter(coupon.getExpiryDate());
    }

    private void validateExpiryDate(LocalDateTime expiryDate) {
        if (expiryDate != null && expiryDate.isBefore(LocalDateTime.now(VN_TIMEZONE))) {
            throw new BusinessLogicException(MessageConstant.Coupon.FUTURE_EXPIRY);
        }
    }
}
