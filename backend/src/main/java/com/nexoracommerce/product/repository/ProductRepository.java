package com.nexoracommerce.product.repository;
import com.nexoracommerce.common.repository.BaseRepository;

import com.nexoracommerce.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends BaseRepository<Product, String> {
    
    /**
     * Find all products with pagination
     */
    Page<Product> findAll(Pageable pageable);
    
    /**
     * Search products by name with pagination
     */
    Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
    
    /**
     * Find products by status with pagination
     */
    Page<Product> findByStatus(String status, Pageable pageable);
    
    /**
     * Search products by keyword (name or description)
     */
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}