package com.nexoracommerce.ai.service;

import com.nexoracommerce.common.util.VectorUtils;
import com.nexoracommerce.product.entity.ProductVariant;
import com.nexoracommerce.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingGeneratorJob {

    private final ProductVariantRepository productVariantRepository;
    private final EmbeddingModel embeddingModel;

    @Async
    public void generateEmbeddingsForAllVariants() {
        log.info("Starting embedding generation for all product variants...");
        List<ProductVariant> variants = productVariantRepository.findAllWithAssociations();

        int count = 0;
        for (ProductVariant variant : variants) {
            try {
                String textChunk = buildTextChunk(variant);
                List<Double> embedding = embeddingModel.embed(textChunk);
                String embeddingString = VectorUtils.toVectorString(embedding);
                productVariantRepository.updateSearchData(variant.getSku(), textChunk, embeddingString);
                count++;
                log.debug("Generated embedding for SKU: {}", variant.getSku());
            } catch (Exception e) {
                log.error("Failed to generate embedding for SKU: {}", variant.getSku(), e);
            }
        }
        log.info("Successfully generated embeddings for {}/{} variants.", count, variants.size());
    }

    private String buildTextChunk(ProductVariant variant) {
        StringBuilder sb = new StringBuilder();

        if (variant.getProduct() != null) {
            sb.append(variant.getProduct().getName()).append(". ");
            if (variant.getProduct().getBrand() != null) {
                sb.append("Thương hiệu: ").append(variant.getProduct().getBrand().getName()).append(". ");
            }
            if (variant.getProduct().getCategory() != null) {
                sb.append("Danh mục: ").append(variant.getProduct().getCategory().getName()).append(". ");
            }
            if (variant.getProduct().getDescription() != null) {
                sb.append(variant.getProduct().getDescription()).append(" ");
            }
        }

        if (variant.getAttributeValues() != null && !variant.getAttributeValues().isEmpty()) {
            String attributes = variant.getAttributeValues().stream()
                    .map(attrVal -> attrVal.getAttribute().getName() + ": " + attrVal.getValue())
                    .collect(Collectors.joining(", "));
            sb.append("Thuộc tính: ").append(attributes).append(". ");
        }

        return sb.toString().trim();
    }
}
