package com.nexoracommerce.category.service;

import com.nexoracommerce.category.dto.request.CategoryRequest;
import com.nexoracommerce.category.dto.response.CategoryResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface CategoryService {
    /**
     * Lấy danh sách danh mục dưới dạng cây (tree)
     */
    List<CategoryResponse> getCategoryTree();

    /**
     * Lấy chi tiết danh mục theo ID hoặc Slug
     */
    CategoryResponse getCategory(String idOrSlug);

    /**
     * Lấy chi tiết danh mục theo ID
     */
    CategoryResponse getCategoryById(Long id);

    /**
     * Lấy chi tiết danh mục theo Slug
     */
    CategoryResponse getCategoryBySlug(String slug);

    /**
     * Tạo danh mục mới (Admin)
     */
    CategoryResponse createCategory(CategoryRequest request);

    /**
     * Cập nhật danh mục (Admin)
     */
    CategoryResponse updateCategory(Long id, CategoryRequest request);

    /**
     * Xóa danh mục (Admin)
     */
    void deleteCategory(Long id);

    /**
     * Lấy danh sách danh mục có phân trang
     */
    Page<CategoryResponse> getCategoriesPageable(Pageable pageable);
}
