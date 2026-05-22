package com.nexoracommerce.category.controller;

import com.nexoracommerce.category.dto.request.CategoryRequest;
import com.nexoracommerce.category.dto.response.CategoryResponse;
import com.nexoracommerce.category.service.ICategoryService;
import com.nexoracommerce.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final ICategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategoryTree() {
        List<CategoryResponse> response = categoryService.getCategoryTree();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{idOrSlug}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategory(@PathVariable String idOrSlug) {
        CategoryResponse response = categoryService.getCategory(idOrSlug);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.updateCategory(categoryId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Category updated successfully"));
    }

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Category deleted successfully"));
    }
}
