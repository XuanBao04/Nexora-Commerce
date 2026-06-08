package com.nexoracommerce.category.service;

import com.nexoracommerce.category.dto.request.CategoryRequest;
import com.nexoracommerce.category.dto.response.CategoryResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ICategoryService {
    List<CategoryResponse> getCategoryTree();
    CategoryResponse getCategory(String idOrSlug);
    CategoryResponse getCategoryById(Long id);
    CategoryResponse getCategoryBySlug(String slug);
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse updateCategory(Long id, CategoryRequest request);
    void deleteCategory(Long id);
    Page<CategoryResponse> getCategoriesPageable(Pageable pageable);
}
