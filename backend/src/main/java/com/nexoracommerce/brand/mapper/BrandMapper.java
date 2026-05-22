package com.nexoracommerce.brand.mapper;

import com.nexoracommerce.brand.dto.response.BrandResponse;
import com.nexoracommerce.brand.entity.Brand;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BrandMapper {

    BrandResponse toResponse(Brand brand);

    List<BrandResponse> toResponseList(List<Brand> brands);

    Set<BrandResponse> toResponseSet(Set<Brand> brands);
}
