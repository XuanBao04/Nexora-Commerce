package com.nexoracommerce.category.service.impl;

import com.nexoracommerce.category.dto.request.CategoryRequest;
import com.nexoracommerce.category.dto.response.CategoryResponse;
import com.nexoracommerce.category.entity.Category;
import com.nexoracommerce.category.mapper.CategoryMapper;
import com.nexoracommerce.category.repository.CategoryRepository;
import com.nexoracommerce.category.service.ICategoryService;
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
public class CategoryServiceImpl implements ICategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryResponse> getCategoryTree() {
        List<Category> roots = categoryRepository.findByParentIsNull();
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
        String slug = request.slug();
        if (slug == null || slug.trim().isEmpty()) {
            slug = generateSlug(request.name());
        } else {
            slug = generateSlug(slug);
        }

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

        Category savedCategory = categoryRepository.save(java.util.Objects.requireNonNull(category));
        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        String slug = request.slug();
        if (slug == null || slug.trim().isEmpty()) {
            slug = generateSlug(request.name());
        } else {
            slug = generateSlug(slug);
        }

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

            // Prevent cyclic hierarchy
            if (isDescendant(category, parent)) {
                throw new BusinessLogicException("Cannot set parent to a descendant category (creates cyclic reference)");
            }
        }

        category.setName(request.name().trim());
        category.setSlug(slug);
        category.setParent(parent);

        Category updatedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(java.util.Objects.requireNonNull(id))) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(java.util.Objects.requireNonNull(id));
    }

    private boolean isDescendant(Category category, Category potentialDescendant) {
        Category current = potentialDescendant.getParent();
        while (current != null) {
            if (current.getId().equals(category.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
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
    public Page<CategoryResponse> getCategoriesPageable(Pageable pageable) {
        Page<Category> categoryPage = categoryRepository.findAll(java.util.Objects.requireNonNull(pageable));
        return categoryPage.map(categoryMapper::toResponse);
    }
}
