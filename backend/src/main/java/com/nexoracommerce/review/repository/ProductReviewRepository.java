package com.nexoracommerce.review.repository;

import com.nexoracommerce.common.repository.BaseRepository;
import com.nexoracommerce.review.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface ProductReviewRepository extends BaseRepository<ProductReview, Long> {
    Page<ProductReview> findByVariantSkuAndParentIsNull(String variantSku, Pageable pageable);
}
