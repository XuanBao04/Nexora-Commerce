package com.nexoracommerce.coupon.mapper;

import com.nexoracommerce.coupon.dto.request.CreateCouponRequest;
import com.nexoracommerce.coupon.dto.request.UpdateCouponRequest;
import com.nexoracommerce.coupon.dto.response.CouponResponse;
import com.nexoracommerce.coupon.entity.Coupon;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Set;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CouponMapper {

    Coupon toEntity(CreateCouponRequest request);

    CouponResponse toResponse(Coupon coupon);

    List<CouponResponse> toResponseList(List<Coupon> coupons);

    Set<CouponResponse> toResponseSet(Set<Coupon> coupons);

    void updateEntityFromRequest(UpdateCouponRequest request, @MappingTarget Coupon coupon);
}
