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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * REST Controller for coupon and discount code management
 * Handles coupon CRUD operations, validation, and discount calculations
 */
@Validated
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupon Module", description = "Endpoints for promotion codes, discount calculation, and coupon administration")
public class CouponController {

    private final ICouponService couponService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "List all active coupons (Admin only)",
        description = "Requires ADMIN role. Retrieves all available promotion codes in the system."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Coupons retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions (must be ADMIN)")
    })
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getAllCoupons() {
        List<CouponResponse> coupons = couponService.getAllCoupons();
        return ResponseEntity.ok(ApiResponse.ok(coupons));
    }

    @GetMapping("/{code}")
    @Operation(
        summary = "Get coupon details by code",
        description = "Retrieves details for a specific coupon code including discount type and amount."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Coupon details retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Coupon code not found")
    })
    public ResponseEntity<ApiResponse<CouponResponse>> getCoupon(
            @Parameter(description = "The unique coupon code", example = "WELCOME10")
            @PathVariable String code) {
        CouponResponse coupon = couponService.getCouponResponseByCode(code);
        return ResponseEntity.ok(ApiResponse.ok(coupon));
    }

    @GetMapping("/{code}/validity")
    @Operation(
        summary = "Validate coupon code",
        description = "Checks whether a coupon code is currently valid (not expired, not used up)."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Coupon validity checked successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Coupon code not found")
    })
    public ResponseEntity<ApiResponse<Boolean>> validateCoupon(
            @Parameter(description = "The coupon code to validate", example = "WELCOME10")
            @PathVariable String code) {
        boolean isValid = couponService.isCouponValid(code);
        return ResponseEntity.ok(ApiResponse.ok(isValid));
    }

    @GetMapping("/{code}/discounts")
    @Operation(
        summary = "Calculate discount amount for a coupon",
        description = "Computes the discount amount that will be applied to an order using this coupon code."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Discount calculated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid order amount"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Coupon code not found")
    })
    public ResponseEntity<ApiResponse<Long>> getDiscount(
            @Parameter(description = "The coupon code", example = "WELCOME10")
            @PathVariable String code,
            @Parameter(description = "Order subtotal amount in VNĐ", example = "1000000")
            @Positive(message = "Order amount must be greater than 0") @RequestParam Long orderAmount) {
        Long discount = couponService.calculateDiscount(code, orderAmount);
        return ResponseEntity.ok(ApiResponse.ok(discount));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Create a new coupon (Admin only)",
        description = "Requires ADMIN role. Creates a new promotion code with discount rules."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Coupon created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or code already exists"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions (must be ADMIN)")
    })
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(
            @Valid @RequestBody CreateCouponRequest request) {
        CouponResponse created = couponService.createCoupon(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Update an existing coupon (Admin only)",
        description = "Requires ADMIN role. Modifies coupon properties like discount amount, expiry date, or usage limits."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Coupon updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions (must be ADMIN)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Coupon not found")
    })
    public ResponseEntity<ApiResponse<CouponResponse>> updateCoupon(
            @Parameter(description = "The coupon code to update", example = "WELCOME10")
            @PathVariable String code,
            @Valid @RequestBody UpdateCouponRequest request) {
        CouponResponse updated = couponService.updateCoupon(code, request);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Coupon updated successfully"));
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Delete a coupon (Admin only)",
        description = "Requires ADMIN role. Permanently removes a promotion code from the system."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Coupon deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions (must be ADMIN)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Coupon not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(
            @Parameter(description = "The coupon code to delete", example = "WELCOME10")
            @PathVariable String code) {
        couponService.deleteCoupon(code);
        return ResponseEntity.ok(ApiResponse.ok(null, "Coupon deleted successfully"));
    }
}
