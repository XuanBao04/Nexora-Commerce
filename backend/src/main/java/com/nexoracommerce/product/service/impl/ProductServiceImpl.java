package com.nexoracommerce.product.service.impl;

import com.nexoracommerce.common.enums.ProductStatus;
import com.nexoracommerce.common.exception.BusinessLogicException;
import com.nexoracommerce.common.exception.ResourceNotFoundException;
import com.nexoracommerce.common.service.CloudinaryService;
import com.nexoracommerce.common.util.UUIDUtils;
import com.nexoracommerce.constant.MessageConstant;
import com.nexoracommerce.inventory.service.InventoryService;
import com.nexoracommerce.product.dto.request.ProductFormRequest;
import com.nexoracommerce.product.dto.request.ProductVariantRequest;
import com.nexoracommerce.product.dto.response.ProductResponse;
import com.nexoracommerce.product.entity.Product;
import com.nexoracommerce.product.entity.ProductAttribute;
import com.nexoracommerce.product.entity.ProductAttributeValue;
import com.nexoracommerce.product.entity.ProductImage;
import com.nexoracommerce.product.entity.ProductVariant;
import com.nexoracommerce.product.mapper.ProductMapper;
import com.nexoracommerce.product.repository.ProductAttributeRepository;
import com.nexoracommerce.product.repository.ProductAttributeValueRepository;
import com.nexoracommerce.product.repository.ProductImageRepository;
import com.nexoracommerce.product.repository.ProductRepository;
import com.nexoracommerce.product.repository.ProductVariantRepository;
import com.nexoracommerce.product.service.ProductService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final CloudinaryService cloudinaryService;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductAttributeRepository productAttributeRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final ProductMapper productMapper;
    private final ObjectMapper objectMapper;

    private Product getProductEntityById(String productId) {
        return productRepository.findByIdWithAssociations(Objects.requireNonNull(productId))
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstant.Product.NOT_FOUND + productId));
    }

    @Override
    @Cacheable(value = "products", key = "#productId")
    public ProductResponse getProductById(String productId) {
        Product product = getProductEntityById(productId);

        // Ẩn sản phẩm INACTIVE với người dùng thường
        if (product.getStatus() == ProductStatus.INACTIVE) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin) {
                throw new ResourceNotFoundException("Sản phẩm đã bị khóa hoặc không tồn tại.");
            }
        }
        return productMapper.toProductResponse(product);
    }

    @Override
    public Page<ProductResponse> getProductsWithFilters(String keyword, Long categoryId, Long brandId,
                                                         Long minPrice, Long maxPrice, ProductStatus status,
                                                         Pageable pageable) {
        String sortByPrice = null;
        Pageable cleanPageable = pageable;

        if (pageable.getSort().isSorted()) {
            Optional<org.springframework.data.domain.Sort.Order> priceOrderOpt = pageable.getSort().stream()
                    .filter(order -> order.getProperty().equals("variants.price"))
                    .findFirst();

            if (priceOrderOpt.isPresent()) {
                org.springframework.data.domain.Sort.Direction direction = priceOrderOpt.get().getDirection();
                sortByPrice = direction.name(); // "ASC" or "DESC"
                
                // Extract other sort orders if present, to preserve them
                java.util.List<org.springframework.data.domain.Sort.Order> otherOrders = pageable.getSort().stream()
                        .filter(order -> !order.getProperty().equals("variants.price"))
                        .toList();
                
                cleanPageable = otherOrders.isEmpty()
                        ? org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())
                        : org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), org.springframework.data.domain.Sort.by(otherOrders));
            }
        }

        return productRepository.findProductsWithFilters(keyword, categoryId, brandId, minPrice, maxPrice, status, sortByPrice, cleanPageable)
                .map(productMapper::toProductResponse);
    }


    @Override
    public Integer getAvailableStock(String productId) {
        // Ưu tiên tìm theo SKU variant
        Optional<ProductVariant> variantOpt = productVariantRepository.findBySku(productId);
        if (variantOpt.isPresent()) {
            ProductVariant variant = variantOpt.get();
            return variant.getQuantity() - safeReserved(variant);
        }

        // Fallback: tìm theo Product ID → tổng tồn kho tất cả variant
        Product product = productRepository.findByIdWithAssociations(productId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstant.Product.NOT_FOUND + productId));

        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            return 0;
        }

        return product.getVariants().values().stream()
                .mapToInt(v -> v.getQuantity() - safeReserved(v))
                .sum();
    }

    @Override
    public boolean isProductAvailable(String productId) {
        Optional<ProductVariant> variantOpt = productVariantRepository.findBySku(productId);
        if (variantOpt.isPresent()) {
            ProductVariant variant = variantOpt.get();
            return (variant.getQuantity() - safeReserved(variant)) > 0;
        }

        Product product = productRepository.findByIdWithAssociations(productId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstant.Product.NOT_FOUND + productId));

        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            return false;
        }

        return product.getVariants().values().stream()
                .anyMatch(v -> (v.getQuantity() - safeReserved(v)) > 0);
    }



    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse createProduct(ProductFormRequest request) {
        String productId = UUIDUtils.generateUUIDv7().toString();

        // Upload ảnh lên Cloudinary
        String imageUrl = null;
        String imagePublicId = null;
        if (request.image() != null && !request.image().isEmpty()) {
            try {
                Map<?, ?> uploadResult = cloudinaryService.upload(request.image());
                imageUrl = (String) uploadResult.get("secure_url");
                imagePublicId = (String) uploadResult.get("public_id");
            } catch (IOException e) {
                log.error("Failed to upload image to Cloudinary", e);
                throw new BusinessLogicException("Tải ảnh lên Cloudinary thất bại: " + e.getMessage());
            }
        }

        Product product = Product.builder()
                .id(productId)
                .name(request.name())
                .description(request.description())
                .status(ProductStatus.valueOf(request.status()))
                .imagePublicId(imagePublicId)
                .build();
        Product savedProduct = productRepository.save(product);

        // Tạo variant tùy chỉnh hoặc variant mặc định
        boolean hasCustomVariants = false;
        if (request.variants() != null && !request.variants().trim().isEmpty()) {
            try {
                List<ProductVariantRequest> variantRequests = objectMapper.readValue(
                        request.variants(), new TypeReference<List<ProductVariantRequest>>() {});
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
                            Set<ProductAttributeValue> attrValues = new HashSet<>();
                            for (var attrReq : vr.attributes()) {
                                String attrName = attrReq.name().trim();
                                String attrVal = attrReq.value().trim();
                                if (!attrName.isEmpty() && !attrVal.isEmpty()) {
                                    ProductAttribute attribute = findOrCreateAttribute(attrName);
                                    ProductAttributeValue attributeValue = findOrCreateAttributeValue(attribute, attrVal);
                                    attrValues.add(attributeValue);
                                }
                            }
                            variant.setAttributeValues(attrValues);
                        }

                        productVariantRepository.save(variant);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to parse variants JSON: {}", request.variants(), e);
                throw new BusinessLogicException("Lỗi cấu trúc dữ liệu biến thể SKU: " + e.getMessage());
            }
        }

        // Tạo variant mặc định nếu không có variant tùy chỉnh
        if (!hasCustomVariants) {
            ProductVariant variant = ProductVariant.builder()
                    .sku(productId)
                    .product(savedProduct)
                    .price(request.price())
                    .quantity(0)
                    .reservedQuantity(0)
                    .soldQuantity(0)
                    .build();
            productVariantRepository.save(variant);
        }

        if (imageUrl != null) {
            ProductImage pImage = ProductImage.builder()
                    .product(savedProduct)
                    .imageUrl(imageUrl)
                    .isPrimary(true)
                    .build();
            productImageRepository.save(pImage);
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
        existingProduct.setStatus(ProductStatus.valueOf(request.status()));

        // Cập nhật ảnh nếu có ảnh mới
        if (request.image() != null && !request.image().isEmpty()) {
            try {
                Map<?, ?> uploadResult = cloudinaryService.upload(request.image());
                String newImageUrl = (String) uploadResult.get("secure_url");
                String newImagePublicId = (String) uploadResult.get("public_id");

                if (existingProduct.getImagePublicId() != null && !existingProduct.getImagePublicId().isEmpty()) {
                    try {
                        cloudinaryService.delete(existingProduct.getImagePublicId());
                    } catch (IOException e) {
                        log.warn("Failed to delete old image from Cloudinary: {}", existingProduct.getImagePublicId(), e);
                    }
                }

                existingProduct.setImageUrl(newImageUrl);
                existingProduct.setImagePublicId(newImagePublicId);
            } catch (IOException e) {
                log.error("Failed to upload updated image to Cloudinary", e);
                throw new BusinessLogicException("Cập nhật ảnh trên Cloudinary thất bại: " + e.getMessage());
            }
        }

        // Đồng bộ variant từ request
        List<ProductVariantRequest> variantRequests = new ArrayList<>();
        boolean hasCustomVariants = false;
        if (request.variants() != null && !request.variants().trim().isEmpty()) {
            try {
                List<ProductVariantRequest> parsed = objectMapper.readValue(
                        request.variants(), new TypeReference<List<ProductVariantRequest>>() {});
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
            Set<String> incomingSkus = new HashSet<>();
            for (ProductVariantRequest vr : variantRequests) {
                incomingSkus.add(vr.sku());
            }

            // Xóa variant không còn trong request
            List<ProductVariant> variantsToRemove = new ArrayList<>();
            for (String currentSku : existingProduct.getVariants().keySet()) {
                if (!incomingSkus.contains(currentSku)) {
                    variantsToRemove.add(existingProduct.getVariants().get(currentSku));
                }
            }
            for (ProductVariant v : variantsToRemove) {
                existingProduct.getVariants().remove(v.getSku());
                productVariantRepository.delete(v);
            }

            // Cập nhật hoặc tạo mới variant
            for (ProductVariantRequest vr : variantRequests) {
                ProductVariant variant = existingProduct.getVariants().get(vr.sku());
                if (variant != null) {
                    variant.setPrice(vr.price());
                    variant.setQuantity(vr.quantity() != null ? vr.quantity() : 0);
                } else {
                    variant = ProductVariant.builder()
                            .sku(vr.sku())
                            .product(existingProduct)
                            .price(vr.price())
                            .quantity(vr.quantity() != null ? vr.quantity() : 0)
                            .reservedQuantity(0)
                            .soldQuantity(0)
                            .build();
                }

                Set<ProductAttributeValue> attrValues = new HashSet<>();
                if (vr.attributes() != null) {
                    for (var attrReq : vr.attributes()) {
                        String attrName = attrReq.name().trim();
                        String attrVal = attrReq.value().trim();
                        if (!attrName.isEmpty() && !attrVal.isEmpty()) {
                            ProductAttribute attribute = findOrCreateAttribute(attrName);
                            ProductAttributeValue attributeValue = findOrCreateAttributeValue(attribute, attrVal);
                            attrValues.add(attributeValue);
                        }
                    }
                }
                variant.setAttributeValues(attrValues);

                existingProduct.getVariants().put(vr.sku(), variant);
                productVariantRepository.save(variant);
            }
        } else {
            // Đảm bảo có ít nhất 1 variant mặc định
            ProductVariant defaultVariant = existingProduct.getVariants().get(productId);
            if (defaultVariant == null) {
                List<ProductVariant> allVariants = new ArrayList<>(existingProduct.getVariants().values());
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

        if (product.getImagePublicId() != null && !product.getImagePublicId().isEmpty()) {
            try {
                cloudinaryService.delete(product.getImagePublicId());
            } catch (IOException e) {
                log.warn("Failed to delete product image from Cloudinary: {}", product.getImagePublicId(), e);
            }
        }

        productVariantRepository.findBySku(productId).ifPresent(productVariantRepository::delete);
        productRepository.delete(product);
    }

    private int safeReserved(ProductVariant variant) {
        return variant.getReservedQuantity() == null ? 0 : variant.getReservedQuantity();
    }

    private ProductAttribute findOrCreateAttribute(String name) {
        return productAttributeRepository.findByName(name)
                .orElseGet(() -> productAttributeRepository.save(
                        ProductAttribute.builder().name(name).build()));
    }

    private ProductAttributeValue findOrCreateAttributeValue(ProductAttribute attribute, String value) {
        return productAttributeValueRepository.findByAttributeIdAndValue(attribute.getId(), value)
                .orElseGet(() -> productAttributeValueRepository.save(
                        ProductAttributeValue.builder()
                                .attribute(attribute)
                                .value(value)
                                .build()));
    }
}
