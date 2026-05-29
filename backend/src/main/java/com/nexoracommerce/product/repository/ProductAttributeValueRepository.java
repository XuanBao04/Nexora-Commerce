package com.nexoracommerce.product.repository;

import com.nexoracommerce.common.repository.BaseRepository;
import com.nexoracommerce.product.entity.ProductAttributeValue;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductAttributeValueRepository extends BaseRepository<ProductAttributeValue, Long> {
    Optional<ProductAttributeValue> findByAttributeIdAndValue(Long attributeId, String value);
}
