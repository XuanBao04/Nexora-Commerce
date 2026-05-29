package com.nexoracommerce.brand.service;

import com.nexoracommerce.brand.dto.request.BrandRequest;
import com.nexoracommerce.brand.dto.response.BrandResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface IBrandService {
    List<BrandResponse> getAllBrands();
    BrandResponse getBrand(String idOrSlug);
    BrandResponse getBrandById(Long id);
    BrandResponse getBrandBySlug(String slug);
    BrandResponse createBrand(BrandRequest request);
    BrandResponse updateBrand(Long id, BrandRequest request);
    void deleteBrand(Long id);
    Page<BrandResponse> getBrandsPageable(Pageable pageable);
}
