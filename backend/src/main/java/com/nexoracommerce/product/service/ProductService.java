package com.nexoracommerce.product.service;

import com.nexoracommerce.product.dto.request.ProductFormRequest;
import com.nexoracommerce.product.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interface cung cấp các tác vụ xử lý sản phẩm
 */
public interface ProductService {

    /**
     * Lấy chi tiết sản phẩm theo ID
     */
    ProductResponse getProductById(String productId);

    /**
     * Lấy danh sách sản phẩm (có lọc, phân trang và sắp xếp động)
     */
    Page<ProductResponse> getProductsWithFilters(String keyword, Long categoryId, Long brandId, Long minPrice, Long maxPrice, com.nexoracommerce.common.enums.ProductStatus status, Pageable pageable);

    /**
     * Kiểm tra số lượng tồn kho khả dụng của sản phẩm
     */
    Integer getAvailableStock(String productId);

    /**
     * Kiểm tra sản phẩm có còn hàng hay không
     */
    boolean isProductAvailable(String productId);

    /**
     * Tạo mới một sản phẩm
     */
    ProductResponse createProduct(ProductFormRequest request);

    /**
     * Cập nhật thông tin sản phẩm
     */
    ProductResponse updateProduct(String productId, ProductFormRequest request);

    /**
     * Xóa sản phẩm theo ID
     */
    void deleteProduct(String productId);
}
