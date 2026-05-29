package com.nexoracommerce.product.service.impl;

import com.nexoracommerce.constant.MessageConstant;
import com.nexoracommerce.product.entity.Product;
import com.nexoracommerce.common.exception.ResourceNotFoundException;
import com.nexoracommerce.common.exception.BusinessLogicException;
import com.nexoracommerce.product.repository.ProductRepository;
import com.nexoracommerce.inventory.service.IInventoryService;
import com.nexoracommerce.product.service.IProductService;
import com.nexoracommerce.common.service.CloudinaryService;
import com.nexoracommerce.product.entity.ProductVariant;
import com.nexoracommerce.product.entity.ProductImage;
import com.nexoracommerce.product.entity.ProductAttribute;
import com.nexoracommerce.product.entity.ProductAttributeValue;
import com.nexoracommerce.product.repository.ProductVariantRepository;
import com.nexoracommerce.product.repository.ProductImageRepository;
import com.nexoracommerce.product.repository.ProductAttributeRepository;
import com.nexoracommerce.product.repository.ProductAttributeValueRepository;
import com.nexoracommerce.product.dto.response.ProductResponse;
import com.nexoracommerce.product.dto.request.ProductFormRequest;
import com.nexoracommerce.product.dto.request.ProductVariantRequest;
import com.nexoracommerce.product.mapper.ProductMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Service implementation for Product operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements IProductService {

    private final ProductRepository productRepository;
    private final IInventoryService inventoryService;
    private final CloudinaryService cloudinaryService;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductAttributeRepository productAttributeRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final ProductMapper productMapper;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    @Cacheable(value = "products", key = "'all'")
    public List<ProductResponse> getAllProducts() {
        return productMapper.toResponseList(productRepository.findAllWithAssociations());
    }

    @Override
    public Page<ProductResponse> getAllProductsPageable(Pageable pageable) {
        return productRepository.findAllWithAssociations(pageable).map(productMapper::toProductResponse);
    }

    private Product getProductEntityById(String productId) {
        return productRepository.findByIdWithAssociations(java.util.Objects.requireNonNull(productId))
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstant.Product.NOT_FOUND + productId));
    }

    @Override
    @Cacheable(value = "products", key = "#productId")
    public ProductResponse getProductById(String productId) {
        return productMapper.toProductResponse(getProductEntityById(productId));
    }

    @Override
    public List<ProductResponse> searchProductsByName(String keyword) {
        return productRepository.searchByNameWithAssociations(keyword).stream()
                .map(productMapper::toProductResponse)
                .toList();
    }

    @Override
    public Page<ProductResponse> searchProductsByNamePageable(String keyword, Pageable pageable) {
        return productRepository.searchByKeyword(keyword, pageable).map(productMapper::toProductResponse);
    }

    @Override
    public Page<ProductResponse> getProductsWithFilters(String keyword, Long categoryId, Long brandId, Long minPrice, Long maxPrice, Pageable pageable) {
        return productRepository.findProductsWithFilters(keyword, categoryId, brandId, minPrice, maxPrice, pageable)
                .map(productMapper::toProductResponse);
    }

    @Override
    public Integer getAvailableStock(String productId) {
        // 1. Try finding by variant SKU first (cart / inventory tracks stock by SKU)
        java.util.Optional<ProductVariant> variantOpt = productVariantRepository.findBySku(productId);
        if (variantOpt.isPresent()) {
            ProductVariant variant = variantOpt.get();
            return variant.getQuantity() - (variant.getReservedQuantity() == null ? 0 : variant.getReservedQuantity());
        }

        // 2. Fallback to finding by Product ID and sum the stock of all its variants
        Product product = productRepository.findByIdWithAssociations(productId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstant.Product.NOT_FOUND + productId));
        
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            return 0;
        }
        
        return product.getVariants().values().stream()
                .mapToInt(v -> v.getQuantity() - (v.getReservedQuantity() == null ? 0 : v.getReservedQuantity()))
                .sum();
    }

    @Override
    public boolean isProductAvailable(String productId) {
        // 1. Try finding by variant SKU first
        java.util.Optional<ProductVariant> variantOpt = productVariantRepository.findBySku(productId);
        if (variantOpt.isPresent()) {
            ProductVariant variant = variantOpt.get();
            return (variant.getQuantity() - (variant.getReservedQuantity() == null ? 0 : variant.getReservedQuantity())) > 0;
        }

        // 2. Fallback to finding by Product ID and verify if at least one variant has available stock
        Product product = productRepository.findByIdWithAssociations(productId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstant.Product.NOT_FOUND + productId));
        
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            return false;
        }
        
        return product.getVariants().values().stream()
                .anyMatch(v -> (v.getQuantity() - (v.getReservedQuantity() == null ? 0 : v.getReservedQuantity())) > 0);
    }

    @Override
    public ProductResponse getProductWithInventory(String productId) {
        // TODO: Implement logic
        return getProductById(productId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public long deleteAllProducts() {
        long count = productRepository.count();
        productRepository.deleteAll();
        return count;
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse createProduct(ProductFormRequest request) {
        // Always generate a time-ordered UUID v7 for new products
        String productId = com.nexoracommerce.common.util.UUIDUtils.generateUUIDv7().toString();

        String imageUrl = null;
        String imagePublicId = null;

        if (request.image() != null && !request.image().isEmpty()) {
            try {
                java.util.Map<?, ?> uploadResult = cloudinaryService.upload(request.image());
                imageUrl = (String) uploadResult.get("secure_url");
                imagePublicId = (String) uploadResult.get("public_id");
            } catch (java.io.IOException e) {
                log.error("Failed to upload image to Cloudinary", e);
                throw new BusinessLogicException("Tải ảnh lên Cloudinary thất bại: " + e.getMessage());
            }
        }

        Product product = Product.builder()
                .id(productId)
                .name(request.name())
                .description(request.description())
                .status(com.nexoracommerce.common.enums.ProductStatus.valueOf(request.status()))
                .imagePublicId(imagePublicId)
                .build();
        Product savedProduct = productRepository.save(java.util.Objects.requireNonNull(product));

        // Initialize custom product variants or default product variant
        boolean hasCustomVariants = false;
        if (request.variants() != null && !request.variants().trim().isEmpty()) {
            try {
                java.util.List<ProductVariantRequest> variantRequests = objectMapper.readValue(
                        request.variants(),
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.List<ProductVariantRequest>>() {}
                );
                if (variantRequests != null && !variantRequests.isEmpty()) {
                    hasCustomVariants = true;
                    for (ProductVariantRequest vr : variantRequests) {
                        ProductVariant variant = ProductVariant.builder()
                                .sku(vr.sku())
                                .product(savedProduct)
                                .price(vr.price())
                                .quantity(vr.quantity() != null ? vr.quantity() : 0)
                                .reservedQuantity(0)
                                .soldQuantity(0)
                                .build();
                        
                        if (vr.attributes() != null && !vr.attributes().isEmpty()) {
                            java.util.Set<ProductAttributeValue> attrValues = new java.util.HashSet<>();
                            for (var attrReq : vr.attributes()) {
                                String attrName = attrReq.name().trim();
                                String attrVal = attrReq.value().trim();
                                if (!attrName.isEmpty() && !attrVal.isEmpty()) {
                                    ProductAttribute attribute = productAttributeRepository.findByName(attrName)
                                            .orElseGet(() -> productAttributeRepository.save(
                                                    ProductAttribute.builder().name(attrName).build()
                                            ));
                                    ProductAttributeValue attributeValue = productAttributeValueRepository
                                            .findByAttributeIdAndValue(attribute.getId(), attrVal)
                                            .orElseGet(() -> productAttributeValueRepository.save(
                                                    ProductAttributeValue.builder()
                                                            .attribute(attribute)
                                                            .value(attrVal)
                                                            .build()
                                            ));
                                    attrValues.add(attributeValue);
                                }
                            }
                            variant.setAttributeValues(attrValues);
                        }
                        
                        productVariantRepository.save(java.util.Objects.requireNonNull(variant));
                    }
                }
            } catch (Exception e) {
                log.error("Failed to parse variants JSON: {}", request.variants(), e);
                throw new BusinessLogicException("Lỗi cấu trúc dữ liệu biến thể SKU: " + e.getMessage());
            }
        }

        if (!hasCustomVariants) {
            ProductVariant variant = ProductVariant.builder()
                    .sku(productId) // default SKU matches productId
                    .product(savedProduct)
                    .price(request.price())
                    .quantity(0)
                    .reservedQuantity(0)
                    .soldQuantity(0)
                    .build();
            productVariantRepository.save(java.util.Objects.requireNonNull(variant));
        }

        // Initialize primary image if present
        if (imageUrl != null) {
            ProductImage pImage = ProductImage.builder()
                    .product(savedProduct)
                    .imageUrl(imageUrl)
                    .isPrimary(true)
                    .build();
            productImageRepository.save(java.util.Objects.requireNonNull(pImage));
        }

        return productMapper.toProductResponse(savedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse updateProduct(String productId, ProductFormRequest request) {
        Product existingProduct = getProductEntityById(productId);
        existingProduct.setName(request.name());
        existingProduct.setDescription(request.description());
        existingProduct.setPrice(request.price());
        existingProduct.setStatus(com.nexoracommerce.common.enums.ProductStatus.valueOf(request.status()));

        if (request.image() != null && !request.image().isEmpty()) {
            try {
                java.util.Map<?, ?> uploadResult = cloudinaryService.upload(request.image());
                String newImageUrl = (String) uploadResult.get("secure_url");
                String newImagePublicId = (String) uploadResult.get("public_id");

                // Purge old image if exists
                if (existingProduct.getImagePublicId() != null && !existingProduct.getImagePublicId().isEmpty()) {
                    try {
                        cloudinaryService.delete(existingProduct.getImagePublicId());
                    } catch (java.io.IOException e) {
                        log.warn("Failed to delete old image from Cloudinary: {}", existingProduct.getImagePublicId(), e);
                    }
                }

                existingProduct.setImageUrl(newImageUrl);
                existingProduct.setImagePublicId(newImagePublicId);
            } catch (java.io.IOException e) {
                log.error("Failed to upload updated image to Cloudinary", e);
                throw new BusinessLogicException("Cập nhật ảnh trên Cloudinary thất bại: " + e.getMessage());
            }
        }

        // Parse and sync custom product variants
        java.util.List<ProductVariantRequest> variantRequests = new java.util.ArrayList<>();
        boolean hasCustomVariants = false;
        if (request.variants() != null && !request.variants().trim().isEmpty()) {
            try {
                java.util.List<ProductVariantRequest> parsed = objectMapper.readValue(
                        request.variants(),
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.List<ProductVariantRequest>>() {}
                );
                if (parsed != null && !parsed.isEmpty()) {
                    variantRequests = parsed;
                    hasCustomVariants = true;
                }
            } catch (Exception e) {
                log.error("Failed to parse variants JSON in updateProduct: {}", request.variants(), e);
                throw new BusinessLogicException("Lỗi cấu trúc dữ liệu biến thể SKU: " + e.getMessage());
            }
        }

        if (hasCustomVariants) {
            java.util.Set<String> incomingSkus = new java.util.HashSet<>();
            for (ProductVariantRequest vr : variantRequests) {
                incomingSkus.add(vr.sku());
            }

            // Delete variants that are no longer in the request to prevent orphans
            java.util.List<ProductVariant> variantsToRemove = new java.util.ArrayList<>();
            for (String currentSku : existingProduct.getVariants().keySet()) {
                if (!incomingSkus.contains(currentSku)) {
                    variantsToRemove.add(existingProduct.getVariants().get(currentSku));
                }
            }
            for (ProductVariant v : variantsToRemove) {
                existingProduct.getVariants().remove(v.getSku());
                productVariantRepository.delete(v);
            }

            // Save or update incoming variants
            for (ProductVariantRequest vr : variantRequests) {
                ProductVariant variant = existingProduct.getVariants().get(vr.sku());
                if (variant != null) {
                    // Update existing
                    variant.setPrice(vr.price());
                    variant.setQuantity(vr.quantity() != null ? vr.quantity() : 0);
                } else {
                    // Create new variant
                    variant = ProductVariant.builder()
                            .sku(vr.sku())
                            .product(existingProduct)
                            .price(vr.price())
                            .quantity(vr.quantity() != null ? vr.quantity() : 0)
                            .reservedQuantity(0)
                            .soldQuantity(0)
                            .build();
                }

                // Resolve attribute values
                java.util.Set<ProductAttributeValue> attrValues = new java.util.HashSet<>();
                if (vr.attributes() != null) {
                    for (var attrReq : vr.attributes()) {
                        String attrName = attrReq.name().trim();
                        String attrVal = attrReq.value().trim();
                        if (!attrName.isEmpty() && !attrVal.isEmpty()) {
                            ProductAttribute attribute = productAttributeRepository.findByName(attrName)
                                    .orElseGet(() -> productAttributeRepository.save(
                                            ProductAttribute.builder().name(attrName).build()
                                    ));
                            ProductAttributeValue attributeValue = productAttributeValueRepository
                                    .findByAttributeIdAndValue(attribute.getId(), attrVal)
                                    .orElseGet(() -> productAttributeValueRepository.save(
                                            ProductAttributeValue.builder()
                                                    .attribute(attribute)
                                                    .value(attrVal)
                                                    .build()
                                    ));
                            attrValues.add(attributeValue);
                        }
                    }
                }
                variant.setAttributeValues(attrValues);
                
                existingProduct.getVariants().put(vr.sku(), variant);
                productVariantRepository.save(variant);
            }
        } else {
            // Fallback: Ensure at least one matching default SKU
            ProductVariant defaultVariant = existingProduct.getVariants().get(productId);
            if (defaultVariant == null) {
                java.util.List<ProductVariant> allVariants = new java.util.ArrayList<>(existingProduct.getVariants().values());
                for (ProductVariant v : allVariants) {
                    productVariantRepository.delete(v);
                }
                existingProduct.getVariants().clear();

                defaultVariant = ProductVariant.builder()
                        .sku(productId)
                        .product(existingProduct)
                        .price(request.price())
                        .quantity(0)
                        .reservedQuantity(0)
                        .soldQuantity(0)
                        .build();
                existingProduct.getVariants().put(productId, defaultVariant);
                productVariantRepository.save(defaultVariant);
            } else {
                defaultVariant.setPrice(request.price());
                productVariantRepository.save(defaultVariant);
            }
        }

        return productMapper.toProductResponse(productRepository.save(existingProduct));
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteProduct(String productId) {
        Product product = getProductEntityById(productId);

        // Purge image from Cloudinary if exists
        if (product.getImagePublicId() != null && !product.getImagePublicId().isEmpty()) {
            try {
                cloudinaryService.delete(product.getImagePublicId());
            } catch (java.io.IOException e) {
                log.warn("Failed to delete product image from Cloudinary on product deletion: {}", product.getImagePublicId(), e);
            }
        }

        // Purge variant record if exists to avoid orphan records in DB
        productVariantRepository.findBySku(productId).ifPresent(productVariantRepository::delete);

        productRepository.delete(product);
    }
}
