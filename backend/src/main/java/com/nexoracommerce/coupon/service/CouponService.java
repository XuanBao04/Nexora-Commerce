package com.nexoracommerce.coupon.service;

import com.nexoracommerce.coupon.dto.request.CreateCouponRequest;
import com.nexoracommerce.coupon.dto.request.UpdateCouponRequest;
import com.nexoracommerce.coupon.dto.response.CouponResponse;
import com.nexoracommerce.coupon.entity.Coupon;
import java.util.List;

public interface CouponService {
    /**
     * Xác thực và lấy thông tin Coupon
     */
    Coupon validateAndGetCoupon(String couponCode, Long orderAmount);

    /**
     * Tính toán số tiền được giảm giá
     */
    Long calculateDiscount(String couponCode, Long orderAmount);

    /**
     * Lấy chi tiết Coupon (dưới dạng Response)
     */
    CouponResponse getCouponResponseByCode(String couponCode);

    /**
     * Lấy chi tiết Coupon (dưới dạng Entity)
     */
    Coupon getCouponByCode(String couponCode);

    /**
     * Tạo mã giảm giá mới (Admin)
     */
    CouponResponse createCoupon(CreateCouponRequest request);

    /**
     * Kiểm tra mã giảm giá có hợp lệ không
     */
    boolean isCouponValid(String couponCode);

    /**
     * Lấy danh sách tất cả mã giảm giá
     */
    List<CouponResponse> getAllCoupons();

    /**
     * Cập nhật mã giảm giá (Admin)
     */
    CouponResponse updateCoupon(String code, UpdateCouponRequest request);

    /**
     * Xóa mã giảm giá (Admin)
     */
    void deleteCoupon(String code);
}
