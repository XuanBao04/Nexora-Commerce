package com.nexoracommerce.coupon.controller;

import com.nexoracommerce.common.response.ApiResponse;
import com.nexoracommerce.coupon.dto.request.CreateCouponRequest;
import com.nexoracommerce.coupon.dto.request.UpdateCouponRequest;
import com.nexoracommerce.coupon.dto.response.CouponResponse;
import com.nexoracommerce.coupon.service.ICouponService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final ICouponService couponService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getAllCoupons() {
        List<CouponResponse> coupons = couponService.getAllCoupons();
        return ResponseEntity.ok(ApiResponse.ok(coupons));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<CouponResponse>> getCoupon(@PathVariable String code) {
        CouponResponse coupon = couponService.getCouponResponseByCode(code);
        return ResponseEntity.ok(ApiResponse.ok(coupon));
    }

    @GetMapping("/{code}/validity")
    public ResponseEntity<ApiResponse<Boolean>> validateCoupon(@PathVariable String code) {
        boolean isValid = couponService.isCouponValid(code);
        return ResponseEntity.ok(ApiResponse.ok(isValid));
    }

    @GetMapping("/{code}/discounts")
    public ResponseEntity<ApiResponse<Long>> getDiscount(
            @PathVariable String code,
            @Positive(message = "Order amount must be greater than 0") @RequestParam Long orderAmount) {
        Long discount = couponService.calculateDiscount(code, orderAmount);
        return ResponseEntity.ok(ApiResponse.ok(discount));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(
            @Valid @RequestBody CreateCouponRequest request) {
        CouponResponse created = couponService.createCoupon(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CouponResponse>> updateCoupon(
            @PathVariable String code,
            @Valid @RequestBody UpdateCouponRequest request) {
        CouponResponse updated = couponService.updateCoupon(code, request);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Coupon updated successfully"));
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable String code) {
        couponService.deleteCoupon(code);
        return ResponseEntity.ok(ApiResponse.ok(null, "Coupon deleted successfully"));
    }
}
