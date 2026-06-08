package com.nexoracommerce.category.service.impl;

import com.nexoracommerce.category.dto.request.CategoryRequest;
import com.nexoracommerce.category.dto.response.CategoryResponse;
import com.nexoracommerce.category.entity.Category;
import com.nexoracommerce.category.mapper.CategoryMapper;
import com.nexoracommerce.category.repository.CategoryRepository;
import com.nexoracommerce.category.service.CategoryService;
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
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryResponse> getCategoryTree() {
        List<Category> all = categoryRepository.findAllWithChildren();
        List<Category> roots = all.stream()
                .filter(c -> c.getParent() == null)
                .toList();
        return categoryMapper.toResponseList(roots);
    }

    @Override
    public CategoryResponse getCategory(String idOrSlug) {
        if (idOrSlug.matches("^\\d+$")) {
            return getCategoryById(Long.parseLong(idOrSlug));
        }
        return getCategoryBySlug(idOrSlug);
    }

    @Override
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return categoryMapper.toResponse(category);
    }

    @Override
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String slug = (request.slug() != null && !request.slug().trim().isEmpty())
                ? SlugUtils.generateSlug(request.slug())
                : SlugUtils.generateSlug(request.name());

        if (categoryRepository.existsBySlug(slug)) {
            throw new BusinessLogicException("Category slug already exists: " + slug);
        }

        Category parent = null;
        if (request.parentId() != null) {
            parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + request.parentId()));
        }

        Category category = Category.builder()
                .name(request.name().trim())
                .slug(slug)
                .parent(parent)
                .build();

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        String slug = (request.slug() != null && !request.slug().trim().isEmpty())
                ? SlugUtils.generateSlug(request.slug())
                : SlugUtils.generateSlug(request.name());

        if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
            throw new BusinessLogicException("Category slug already in use by another category: " + slug);
        }

        Category parent = null;
        if (request.parentId() != null) {
            if (request.parentId().equals(id)) {
                throw new BusinessLogicException("A category cannot be its own parent");
            }
            parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + request.parentId()));

            // Chống vòng lặp phân cấp
            if (isDescendant(category, parent)) {
                throw new BusinessLogicException("Cannot set parent to a descendant category (creates cyclic reference)");
            }
        }

        category.setName(request.name().trim());
        category.setSlug(slug);
        category.setParent(parent);

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(Objects.requireNonNull(id))) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
    }

    private boolean isDescendant(Category category, Category potentialDescendant) {
        Category current = potentialDescendant.getParent();
        while (current != null) {
            if (current.getId().equals(category.getId())) return true;
            current = current.getParent();
        }
        return false;
    }

    @Override
    public Page<CategoryResponse> getCategoriesPageable(Pageable pageable) {
        return categoryRepository.findAll(Objects.requireNonNull(pageable))
                .map(categoryMapper::toResponse);
    }
}
