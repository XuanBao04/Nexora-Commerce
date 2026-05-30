package com.nexoracommerce.product.service;

import com.nexoracommerce.product.dto.request.ProductFormRequest;
import com.nexoracommerce.product.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

/**
 * Service interface for Product operations
 */
public interface IProductService {

    /**
     * Get all products
     * @return list of ProductResponse
     */
    List<ProductResponse> getAllProducts();

    /**
     * Get all products with pagination
     * @param pageable pagination parameters
     * @return Page of ProductResponse
     */
    Page<ProductResponse> getAllProductsPageable(Pageable pageable);

    /**
     * Get product by ID
     * @param productId the product ID
     * @return ProductResponse
     */
    ProductResponse getProductById(String productId);

    /**
     * Search products by name
     * @param keyword search keyword
     * @return list of matching ProductResponse
     */
    List<ProductResponse> searchProductsByName(String keyword);

    /**
     * Search products by keyword with pagination
     * @param keyword search keyword
     * @param pageable pagination parameters
     * @return Page of ProductResponse
     */
    Page<ProductResponse> searchProductsByNamePageable(String keyword, Pageable pageable);

    /**
     * Get products filtered by keyword, category, and brand with pagination
     * @param keyword search keyword
     * @param categoryId category ID
     * @param brandId brand ID
     * @param minPrice minimum price filter
     * @param maxPrice maximum price filter
     * @param pageable pagination parameters
     * @return Page of ProductResponse
     */
    Page<ProductResponse> getProductsWithFilters(String keyword, Long categoryId, Long brandId, Long minPrice, Long maxPrice, com.nexoracommerce.common.enums.ProductStatus status, Pageable pageable);

    /**
     * Get available stock for a product
     * @param productId the product ID
     * @return available quantity
     */
    Integer getAvailableStock(String productId);

    /**
     * Check if product is available
     * @param productId the product ID
     * @return true if available, false otherwise
     */
    boolean isProductAvailable(String productId);

    /**
     * Get product with inventory details
     * @param productId the product ID
     * @return ProductResponse
     */
    ProductResponse getProductWithInventory(String productId);

    /**
     * Delete all products
     * @return number of products deleted
     */
    long deleteAllProducts();

    /**
     * Create a new product
     * @param request the product request
     * @return created ProductResponse
     */
    ProductResponse createProduct(ProductFormRequest request);

    /**
     * Update an existing product
     * @param productId the product ID
     * @param request the product request
     * @return updated ProductResponse
     */
    ProductResponse updateProduct(String productId, ProductFormRequest request);

    /**
     * Delete a product by ID
     * @param productId the product ID
     */
    void deleteProduct(String productId);
}
