package com.nexoracommerce.ai.service;

import com.nexoracommerce.common.util.VectorUtils;
import com.nexoracommerce.product.dto.response.ProductResponse;
import com.nexoracommerce.product.entity.Product;
import com.nexoracommerce.product.entity.ProductVariant;
import com.nexoracommerce.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSearchService {

    private final EmbeddingModel embeddingModel;
    private final AiSearchRepositoryExecutor aiSearchRepositoryExecutor;
    private final ProductMapper productMapper;

    // Hybrid search: kết hợp vector embedding + FTS, fallback sang FTS nếu embedding lỗi
    @Transactional(readOnly = true)
    public List<ProductResponse> hybridSearch(String query, int limit) {
        log.info("Performing hybrid AI search for query: {}", query);

        List<ProductVariant> results;

        try {
            List<Double> queryEmbedding = embeddingModel.embed(query);
            String embeddingString = VectorUtils.toVectorString(queryEmbedding);
            results = aiSearchRepositoryExecutor.executeHybridSearch(query, embeddingString, limit);
            log.info("Hybrid search returned {} variant results", results.size());
        } catch (Exception e) {
            log.warn("Hybrid search failed, falling back to FTS-only: {}", e.getMessage());
            try {
                results = aiSearchRepositoryExecutor.executeFtsOnlySearch(query, limit);
                log.info("FTS fallback returned {} variant results", results.size());
            } catch (Exception ex) {
                log.error("FTS fallback also failed: {}", ex.getMessage(), ex);
                throw ex;
            }
        }

        return groupVariantsByProduct(results);
    }

    // Gom variant theo product, giữ thứ tự ranking
    private List<ProductResponse> groupVariantsByProduct(List<ProductVariant> variants) {
        Map<String, Product> productMap = new LinkedHashMap<>();

        for (ProductVariant variant : variants) {
            Product product = variant.getProduct();
            if (product != null) {
                productMap.putIfAbsent(product.getId(), product);
            }
        }

        return productMap.values().stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList());
    }
}
