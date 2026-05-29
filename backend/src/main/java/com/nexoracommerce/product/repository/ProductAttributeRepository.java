package com.nexoracommerce.product.repository;

import com.nexoracommerce.common.repository.BaseRepository;
import com.nexoracommerce.product.entity.ProductAttribute;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductAttributeRepository extends BaseRepository<ProductAttribute, Long> {
    Optional<ProductAttribute> findByName(String name);
}
