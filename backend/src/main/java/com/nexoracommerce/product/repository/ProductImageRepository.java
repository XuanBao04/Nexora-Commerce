package com.nexoracommerce.product.repository;

import com.nexoracommerce.common.repository.BaseRepository;
import com.nexoracommerce.product.entity.ProductImage;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImageRepository extends BaseRepository<ProductImage, Long> {
    List<ProductImage> findByProductId(String productId);
}
