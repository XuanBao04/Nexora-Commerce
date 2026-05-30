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
    
    /**
     * Find all products with pagination
     */
    Page<Product> findAll(Pageable pageable);
    
    /**
     * Find all products with category and brand eagerly loaded to prevent N+1 queries.
     */
    @Query(value = "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand",
           countQuery = "SELECT COUNT(p) FROM Product p")
    Page<Product> findAllWithAssociations(Pageable pageable);

    /**
     * Find all products with associations
     */
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand")
    List<Product> findAllWithAssociations();
    
    /**
     * Search products by name with pagination
     */
    Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
    
    /**
     * Search products by name returning list with associations eagerly loaded (prevents N+1 and in-memory filtering).
     */
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> searchByNameWithAssociations(@Param("keyword") String keyword);

    /**
     * Find products by status with pagination
     */
    Page<Product> findByStatus(String status, Pageable pageable);
    
    /**
     * Search products by keyword (name or description) with associations eagerly fetched
     */
    @Query(value = "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Find products with keyword, categoryId, and brandId filters
     */
    @Query(value = "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
            "AND (:brandId IS NULL OR p.brand.id = :brandId) " +
            "AND (:status IS NULL OR p.status = :status) " +
            "AND EXISTS (SELECT v FROM ProductVariant v WHERE v.product = p AND (:minPrice IS NULL OR v.price >= :minPrice) AND (:maxPrice IS NULL OR v.price <= :maxPrice))",
           countQuery = "SELECT COUNT(p) FROM Product p " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
            "AND (:brandId IS NULL OR p.brand.id = :brandId) " +
            "AND (:status IS NULL OR p.status = :status) " +
            "AND EXISTS (SELECT v FROM ProductVariant v WHERE v.product = p AND (:minPrice IS NULL OR v.price >= :minPrice) AND (:maxPrice IS NULL OR v.price <= :maxPrice))")
    Page<Product> findProductsWithFilters(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("brandId") Long brandId,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            @Param("status") com.nexoracommerce.common.enums.ProductStatus status,
            Pageable pageable);

    /**
     * Find a product by ID with all associations eagerly loaded (variants, images, category, brand)
     * This prevents LazyInitializationException when accessing getPrice(), getQuantity(), getImageUrl()
     */
    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN FETCH p.variants " +
            "LEFT JOIN FETCH p.images " +
            "LEFT JOIN FETCH p.category " +
            "LEFT JOIN FETCH p.brand " +
            "WHERE p.id = :productId")
    Optional<Product> findByIdWithAssociations(@Param("productId") String productId);
}