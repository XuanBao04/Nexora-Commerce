package com.shopcart.product.controller;

import com.shopcart.product.dto.response.ProductResponse;
import com.shopcart.product.entity.Product;
import com.shopcart.product.service.IProductService;
import com.shopcart.product.mapper.ProductMapper;
import com.shopcart.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

/**
 * REST Controller for Product operations
 */
@RestController
@RequiredArgsConstructor
public class ProductController {

    private final IProductService productService;
    private final ProductMapper productMapper;

    /**
     * Get all products without pagination
     * @return ApiResponse with list of ProductResponse
     */
    @GetMapping("/api/products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        List<ProductResponse> responses = products.stream()
                .map(productMapper::toProductResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    /**
     * Get all products with pagination
     * @param page page number (0-indexed)
     * @param size page size
     * @param sort sorting field (default: id)
     * @return ApiResponse with paginated ProductResponse
     */
    @GetMapping("/api/products/paginated")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProductsPaginated(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "id") String sort) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).ascending());
        Page<Product> productPage = productService.getAllProductsPageable(pageable);
        
        List<ProductResponse> responses = productPage.getContent().stream()
                .map(productMapper::toProductResponse)
                .toList();
        
        return ResponseEntity.ok(
                ApiResponse.okWithPagination(responses, 
                    ApiResponse.PaginationInfo.from(productPage))
        );
    }

    /**
     * Get product by ID
     * @param productId the product ID
     * @return ApiResponse with ProductResponse
     */
    @GetMapping("/api/products/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable String productId) {
        Product product = productService.getProductById(productId);
        ProductResponse response = productMapper.toProductResponse(product);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Search products by keyword with pagination
     * @param keyword search keyword
     * @param page page number
     * @param size page size
     * @return ApiResponse with paginated search results
     */
    @GetMapping("/api/products/search")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productService.searchProductsByNamePageable(keyword, pageable);
        
        List<ProductResponse> responses = productPage.getContent().stream()
                .map(productMapper::toProductResponse)
                .toList();
        
        return ResponseEntity.ok(
                ApiResponse.okWithPagination(responses,
                    ApiResponse.PaginationInfo.from(productPage))
        );
    }

    /**
     * Get available stock for a product
     * @param productId the product ID
     * @return ApiResponse with available quantity
     */
    @GetMapping("/api/products/{productId}/stock")
    public ResponseEntity<ApiResponse<Integer>> getAvailableStock(@PathVariable String productId) {
        Integer stock = productService.getAvailableStock(productId);
        return ResponseEntity.ok(ApiResponse.ok(stock));
    }

    /**
     * Check if product is available
     * @param productId the product ID
     * @return ApiResponse with availability status
     */
    @GetMapping("/api/products/{productId}/availability")
    public ResponseEntity<ApiResponse<Boolean>> isProductAvailable(@PathVariable String productId) {
        boolean available = productService.isProductAvailable(productId);
        return ResponseEntity.ok(ApiResponse.ok(available));
    }

    /**
     * Create a new product (ADMIN only)
     * @param request ProductRequest
     * @return ApiResponse with created ProductResponse
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/api/admin/products", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @ModelAttribute com.shopcart.product.dto.request.ProductFormRequest request) {
        Product product = productService.createProduct(request);
        ProductResponse response = productMapper.toProductResponse(product);
        return ResponseEntity.status(201).body(ApiResponse.created(response));
    }

    /**
     * Update an existing product (ADMIN only)
     * @param productId the product ID to update
     * @param request ProductRequest with updated data
     * @return ApiResponse with updated ProductResponse
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/api/admin/products/{productId}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable String productId,
            @Valid @ModelAttribute com.shopcart.product.dto.request.ProductFormRequest request) {
        Product product = productService.updateProduct(productId, request);
        ProductResponse response = productMapper.toProductResponse(product);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Delete a specific product (ADMIN only)
     * @param productId the product ID to delete
     * @return ApiResponse with success message
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/admin/products/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable String productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * WARNING: Bulk delete all products is DISABLED for safety
     * Use individual DELETE /{productId} endpoint instead
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/admin/products")
    public ResponseEntity<ApiResponse<String>> deleteAllProducts() {
        // DISABLED: Bulk delete is dangerous and can lead to accidental data loss
        // To delete products, use DELETE /api/products/{productId} for each product
        return ResponseEntity.status(403).body(
            ApiResponse.success(null, "Bulk delete is disabled for safety. Delete products individually using DELETE /api/products/{productId}", 403)
        );
    }
}
