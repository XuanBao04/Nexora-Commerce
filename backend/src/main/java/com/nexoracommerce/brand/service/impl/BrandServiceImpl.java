package com.nexoracommerce.brand.service.impl;

import com.nexoracommerce.brand.dto.request.BrandRequest;
import com.nexoracommerce.brand.dto.response.BrandResponse;
import com.nexoracommerce.brand.entity.Brand;
import com.nexoracommerce.brand.mapper.BrandMapper;
import com.nexoracommerce.brand.repository.BrandRepository;
import com.nexoracommerce.brand.service.IBrandService;
import com.nexoracommerce.common.exception.BusinessLogicException;
import com.nexoracommerce.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandServiceImpl implements IBrandService {

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
        Brand brand = brandRepository.findById(java.util.Objects.requireNonNull(id))
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
        String slug = request.slug();
        if (slug == null || slug.trim().isEmpty()) {
            slug = generateSlug(request.name());
        } else {
            slug = generateSlug(slug);
        }

        if (brandRepository.existsBySlug(slug)) {
            throw new BusinessLogicException("Brand slug already exists: " + slug);
        }

        Brand brand = Brand.builder()
                .name(request.name().trim())
                .slug(slug)
                .build();

        Brand savedBrand = brandRepository.save(java.util.Objects.requireNonNull(brand));
        return brandMapper.toResponse(savedBrand);
    }

    @Override
    @Transactional
    public BrandResponse updateBrand(Long id, BrandRequest request) {
        Brand brand = brandRepository.findById(java.util.Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));

        String slug = request.slug();
        if (slug == null || slug.trim().isEmpty()) {
            slug = generateSlug(request.name());
        } else {
            slug = generateSlug(slug);
        }

        if (brandRepository.existsBySlugAndIdNot(slug, id)) {
            throw new BusinessLogicException("Brand slug already in use by another brand: " + slug);
        }

        brand.setName(request.name().trim());
        brand.setSlug(slug);

        Brand updatedBrand = brandRepository.save(brand);
        return brandMapper.toResponse(updatedBrand);
    }

    @Override
    @Transactional
    public void deleteBrand(Long id) {
        if (!brandRepository.existsById(java.util.Objects.requireNonNull(id))) {
            throw new ResourceNotFoundException("Brand not found with id: " + id);
        }
        brandRepository.deleteById(java.util.Objects.requireNonNull(id));
    }

    private String generateSlug(String input) {
        if (input == null) return "";
        String temp = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String slug = pattern.matcher(temp).replaceAll("")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return slug;
    }

    @Override
    public Page<BrandResponse> getBrandsPageable(Pageable pageable) {
        Page<Brand> brandPage = brandRepository.findAll(java.util.Objects.requireNonNull(pageable));
        return brandPage.map(brandMapper::toResponse);
    }
}
