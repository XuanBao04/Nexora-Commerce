package com.nexoracommerce.brand.service.impl;

import com.nexoracommerce.brand.dto.request.BrandRequest;
import com.nexoracommerce.brand.dto.response.BrandResponse;
import com.nexoracommerce.brand.entity.Brand;
import com.nexoracommerce.brand.mapper.BrandMapper;
import com.nexoracommerce.brand.repository.BrandRepository;
import com.nexoracommerce.brand.service.BrandService;
import com.nexoracommerce.common.exception.BusinessLogicException;
import com.nexoracommerce.common.exception.ResourceNotFoundException;
import com.nexoracommerce.common.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    @Override
    public List<BrandResponse> getAllBrands() {
        return brandMapper.toResponseList(brandRepository.findAll());
    }

    @Override
    public BrandResponse getBrand(String idOrSlug) {
        if (idOrSlug.matches("^\\d+$")) {
            return getBrandById(Long.parseLong(idOrSlug));
        }
        return getBrandBySlug(idOrSlug);
    }

    @Override
    public BrandResponse getBrandById(Long id) {
        Brand brand = brandRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
        return brandMapper.toResponse(brand);
    }

    @Override
    public BrandResponse getBrandBySlug(String slug) {
        Brand brand = brandRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with slug: " + slug));
        return brandMapper.toResponse(brand);
    }

    @Override
    @Transactional
    public BrandResponse createBrand(BrandRequest request) {
        String slug = (request.slug() != null && !request.slug().trim().isEmpty())
                ? SlugUtils.generateSlug(request.slug())
                : SlugUtils.generateSlug(request.name());

        if (brandRepository.existsBySlug(slug)) {
            throw new BusinessLogicException("Brand slug already exists: " + slug);
        }

        Brand brand = Brand.builder()
                .name(request.name().trim())
                .slug(slug)
                .build();

        return brandMapper.toResponse(brandRepository.save(brand));
    }

    @Override
    @Transactional
    public BrandResponse updateBrand(Long id, BrandRequest request) {
        Brand brand = brandRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));

        String slug = (request.slug() != null && !request.slug().trim().isEmpty())
                ? SlugUtils.generateSlug(request.slug())
                : SlugUtils.generateSlug(request.name());

        if (brandRepository.existsBySlugAndIdNot(slug, id)) {
            throw new BusinessLogicException("Brand slug already in use by another brand: " + slug);
        }

        brand.setName(request.name().trim());
        brand.setSlug(slug);

        return brandMapper.toResponse(brandRepository.save(brand));
    }

    @Override
    @Transactional
    public void deleteBrand(Long id) {
        if (!brandRepository.existsById(Objects.requireNonNull(id))) {
            throw new ResourceNotFoundException("Brand not found with id: " + id);
        }
        brandRepository.deleteById(id);
    }

    @Override
    public Page<BrandResponse> getBrandsPageable(Pageable pageable) {
        return brandRepository.findAll(Objects.requireNonNull(pageable))
                .map(brandMapper::toResponse);
    }
}
