package com.nexoracommerce.ai.service;

import com.nexoracommerce.product.entity.ProductVariant;
import com.nexoracommerce.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiSearchRepositoryExecutor {

    private final ProductVariantRepository productVariantRepository;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<ProductVariant> executeHybridSearch(String query, String embeddingString, int limit) {
        log.debug("Executing hybrid search query in new transaction");
        return productVariantRepository.hybridSearch(query, embeddingString, limit);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<ProductVariant> executeFtsOnlySearch(String query, int limit) {
        log.debug("Executing FTS-only search query in new transaction");
        return productVariantRepository.ftsOnlySearch(query, limit);
    }
}
