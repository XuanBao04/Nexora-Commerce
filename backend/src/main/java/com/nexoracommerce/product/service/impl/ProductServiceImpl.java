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
import com.nexoracommerce.product.repository.ProductVariantRepository;
import com.nexoracommerce.product.repository.ProductImageRepository;
import com.nexoracommerce.product.dto.response.ProductResponse;
import com.nexoracommerce.product.dto.request.ProductFormRequest;
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
    private final ProductMapper productMapper;

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
        return productRepository.findById(productId)
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
        // Verify product exists
        getProductEntityById(productId);
        return inventoryService.getStock(productId);
    }

    @Override
    public boolean isProductAvailable(String productId) {
        return inventoryService.hasEnoughStock(productId, 1);
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
        Product savedProduct = productRepository.save(product);

        // Initialize default product variant with 0 quantity
        ProductVariant variant = ProductVariant.builder()
                .sku(productId) // default SKU matches productId
                .product(savedProduct)
                .price(request.price())
                .quantity(0)
                .reservedQuantity(0)
                .soldQuantity(0)
                .build();
        productVariantRepository.save(variant);

        // Initialize primary image if present
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
