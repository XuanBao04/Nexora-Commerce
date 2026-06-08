package com.nexoracommerce.brand.service;

import com.nexoracommerce.brand.dto.request.BrandRequest;
import com.nexoracommerce.brand.dto.response.BrandResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface BrandService {
    /**
     * Lấy danh sách tất cả thương hiệu
     */
    List<BrandResponse> getAllBrands();

    /**
     * Lấy chi tiết thương hiệu theo ID hoặc Slug
     */
    BrandResponse getBrand(String idOrSlug);

    /**
     * Lấy chi tiết thương hiệu theo ID
     */
    BrandResponse getBrandById(Long id);

    /**
     * Lấy chi tiết thương hiệu theo Slug
     */
    BrandResponse getBrandBySlug(String slug);

    /**
     * Tạo thương hiệu mới (Admin)
     */
    BrandResponse createBrand(BrandRequest request);

    /**
     * Cập nhật thương hiệu (Admin)
     */
    BrandResponse updateBrand(Long id, BrandRequest request);

    /**
     * Xóa thương hiệu (Admin)
     */
    void deleteBrand(Long id);

    /**
     * Lấy danh sách thương hiệu có phân trang
     */
    Page<BrandResponse> getBrandsPageable(Pageable pageable);
}
