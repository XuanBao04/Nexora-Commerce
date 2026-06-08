package com.nexoracommerce.coupon.service;

import com.nexoracommerce.coupon.dto.request.CreateCouponRequest;
import com.nexoracommerce.coupon.dto.request.UpdateCouponRequest;
import com.nexoracommerce.coupon.dto.response.CouponResponse;
import com.nexoracommerce.coupon.entity.Coupon;
import java.util.List;

public interface ICouponService {
    
    Coupon validateAndGetCoupon(String couponCode, Long orderAmount);

    Long calculateDiscount(String couponCode, Long orderAmount);

    CouponResponse getCouponResponseByCode(String couponCode);

    Coupon getCouponByCode(String couponCode);

    CouponResponse createCoupon(CreateCouponRequest request);

    boolean isCouponValid(String couponCode);

    List<CouponResponse> getAllCoupons();

    CouponResponse updateCoupon(String code, UpdateCouponRequest request);

    void deleteCoupon(String code);
}
