package com.nexoracommerce.product.repository;

import com.nexoracommerce.common.repository.BaseRepository;
import com.nexoracommerce.product.entity.ProductImage;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(readOnly = true)
public interface ProductImageRepository extends BaseRepository<ProductImage, Long> {
    List<ProductImage> findByProductId(String productId);
}
