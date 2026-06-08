package com.nexoracommerce.product.repository;
import com.nexoracommerce.common.repository.BaseRepository;

import com.nexoracommerce.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public interface ProductRepository extends BaseRepository<Product, String> {
    
    // Chỉ giữ lại câu query lọc động và tìm sản phẩm theo ID kèm associations
    
    /**
     * Lọc sản phẩm theo từ khóa, danh mục, thương hiệu, khoảng giá, trạng thái và sắp xếp theo giá
     */
    @Query(
        value = """
            SELECT p 
            FROM Product p 
            LEFT JOIN FETCH p.category 
            LEFT JOIN FETCH p.brand 
            WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) 
              AND (:categoryId IS NULL OR p.category.id = :categoryId) 
              AND (:brandId IS NULL OR p.brand.id = :brandId) 
              AND (:status IS NULL OR p.status = :status) 
              AND EXISTS (
                  SELECT v 
                  FROM ProductVariant v 
                  WHERE v.product = p 
                    AND (:minPrice IS NULL OR v.price >= :minPrice) 
                    AND (:maxPrice IS NULL OR v.price <= :maxPrice)
              )
            ORDER BY 
              CASE WHEN :sortByPrice = 'ASC' THEN (
                  SELECT MIN(v.price) 
                  FROM ProductVariant v 
                  WHERE v.product = p 
                    AND (:minPrice IS NULL OR v.price >= :minPrice) 
                    AND (:maxPrice IS NULL OR v.price <= :maxPrice)
              ) END ASC,
              CASE WHEN :sortByPrice = 'DESC' THEN (
                  SELECT MIN(v.price) 
                  FROM ProductVariant v 
                  WHERE v.product = p 
                    AND (:minPrice IS NULL OR v.price >= :minPrice) 
                    AND (:maxPrice IS NULL OR v.price <= :maxPrice)
              ) END DESC,
              p.id DESC
        """,
        countQuery = """
            SELECT COUNT(p) 
            FROM Product p 
            WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) 
              AND (:categoryId IS NULL OR p.category.id = :categoryId) 
              AND (:brandId IS NULL OR p.brand.id = :brandId) 
              AND (:status IS NULL OR p.status = :status) 
              AND EXISTS (
                  SELECT v 
                  FROM ProductVariant v 
                  WHERE v.product = p 
                    AND (:minPrice IS NULL OR v.price >= :minPrice) 
                    AND (:maxPrice IS NULL OR v.price <= :maxPrice)
              )
        """
    )
    Page<Product> findProductsWithFilters(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("brandId") Long brandId,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            @Param("status") com.nexoracommerce.common.enums.ProductStatus status,
            @Param("sortByPrice") String sortByPrice,
            Pageable pageable);

    /**
     * Tìm sản phẩm theo ID kèm các liên kết (variants, images, category, brand) để tránh Lazy load
     */
    @Query("""
        SELECT DISTINCT p 
        FROM Product p 
        LEFT JOIN FETCH p.variants 
        LEFT JOIN FETCH p.images 
        LEFT JOIN FETCH p.category 
        LEFT JOIN FETCH p.brand 
        WHERE p.id = :productId
    """)
    Optional<Product> findByIdWithAssociations(@Param("productId") String productId);
}