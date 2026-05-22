package com.nexoracommerce.coupon.controller;

import com.nexoracommerce.coupon.dto.request.CreateCouponRequest;
import com.nexoracommerce.coupon.dto.request.UpdateCouponRequest;
import com.nexoracommerce.coupon.dto.response.CouponResponse;
import com.nexoracommerce.coupon.entity.Coupon;
import com.nexoracommerce.coupon.service.ICouponService;
import com.nexoracommerce.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {
    private final ICouponService couponService;

    /**
     * Get all coupons (Admin only)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Coupon>>> getAllCoupons() {
        List<Coupon> coupons = couponService.getAllCoupons();
        return ResponseEntity.ok(ApiResponse.ok(coupons));
    }

    /**
     * Get coupon by code (public endpoint)
     */
    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<Coupon>> getCoupon(@PathVariable String code) {
        Coupon coupon = couponService.getCouponByCode(code);
        return ResponseEntity.ok(ApiResponse.ok(coupon));
    }

    /**
     * Check if coupon is valid
     */
    @GetMapping("/{code}/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateCoupon(@PathVariable String code) {
        boolean isValid = couponService.isCouponValid(code);
        return ResponseEntity.ok(ApiResponse.ok(isValid));
    }

    /**
     * Calculate discount for given coupon and order amount
     */
    @GetMapping("/{code}/discount")
    public ResponseEntity<ApiResponse<Long>> getDiscount(
            @PathVariable String code,
            @RequestParam Long orderAmount) {
        try {
            Long discount = couponService.calculateDiscount(code, orderAmount);
            return ResponseEntity.ok(ApiResponse.ok(discount));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.success(null, "Invalid coupon or order amount: " + e.getMessage(), 400)
            );
        }
    }

    /**
     * Create new coupon (Admin only)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<Coupon>> createCoupon(
            @Valid @RequestBody CreateCouponRequest request) {
        Coupon coupon = Coupon.builder()
                .code(request.getCode())
                .discountPercent(request.getDiscountPercent())
                .active(request.getActive() != null ? request.getActive() : true)
                .minimumOrderAmount(request.getMinimumOrderAmount() != null ? request.getMinimumOrderAmount() : 0L)
                .expiryDate(request.getExpiryDate())
                .build();
        Coupon created = couponService.createCoupon(coupon);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    /**
     * Update coupon (Admin only)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{code}")
    public ResponseEntity<ApiResponse<Coupon>> updateCoupon(
            @PathVariable String code,
            @Valid @RequestBody UpdateCouponRequest request) {
        Coupon coupon = Coupon.builder()
                .discountPercent(request.getDiscountPercent())
                .active(request.getActive())
                .minimumOrderAmount(request.getMinimumOrderAmount())
                .expiryDate(request.getExpiryDate())
                .build();
        Coupon updated = couponService.updateCoupon(code, coupon);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    /**
     * Delete coupon (Admin only)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{code}")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable String code) {
        couponService.deleteCoupon(code);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
