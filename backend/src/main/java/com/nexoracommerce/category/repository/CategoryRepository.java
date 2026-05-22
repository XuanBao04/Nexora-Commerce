package com.nexoracommerce.category.repository;

import com.nexoracommerce.category.entity.Category;
import com.nexoracommerce.common.repository.BaseRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public interface CategoryRepository extends BaseRepository<Category, Long> {
    Optional<Category> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    List<Category> findByParentIsNull(); // Get all root categories
}
