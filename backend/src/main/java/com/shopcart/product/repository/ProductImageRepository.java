package com.shopcart.product.repository;

import com.shopcart.common.repository.BaseRepository;
import com.shopcart.product.entity.ProductImage;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImageRepository extends BaseRepository<ProductImage, Long> {
    List<ProductImage> findByProductId(String productId);
}
