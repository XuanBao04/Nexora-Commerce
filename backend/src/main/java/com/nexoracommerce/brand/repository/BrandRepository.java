package com.nexoracommerce.brand.repository;

import com.nexoracommerce.brand.entity.Brand;
import com.nexoracommerce.common.repository.BaseRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public interface BrandRepository extends BaseRepository<Brand, Long> {
    Optional<Brand> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
}
