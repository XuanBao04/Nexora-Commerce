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
public class ProductServiceImpl implements IProductService {

    private final ProductRepository productRepository;
    private final IInventoryService inventoryService;
    private final CloudinaryService cloudinaryService;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    @Cacheable(value = "products", key = "'all'")
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Page<Product> getAllProductsPageable(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @Override
    @Cacheable(value = "products", key = "#productId")
    public Product getProductById(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstant.Product.NOT_FOUND + productId));
    }

    @Override
    public List<Product> searchProductsByName(String keyword) {
        return productRepository.findAll().stream()
                .filter(p -> p.getName().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
    }

    @Override
    public Page<Product> searchProductsByNamePageable(String keyword, Pageable pageable) {
        return productRepository.searchByKeyword(keyword, pageable);
    }

    @Override
    public Integer getAvailableStock(String productId) {
        // Verify product exists
        getProductById(productId);
        return inventoryService.getStock(productId);
    }

    @Override
    public boolean isProductAvailable(String productId) {
        return inventoryService.hasEnoughStock(productId, 1);
    }

    @Override
    public Product getProductWithInventory(String productId) {
        // TODO: Implement logic
        return getProductById(productId);
    }

    @Override
    @CacheEvict(value = "products", allEntries = true)
    public long deleteAllProducts() {
        long count = productRepository.count();
        productRepository.deleteAll();
        return count;
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public Product createProduct(com.nexoracommerce.product.dto.request.ProductFormRequest request) {
        // Always generate a time-ordered UUID v7 for new products
        String productId = com.nexoracommerce.common.util.UUIDUtils.generateUUIDv7().toString();

        String imageUrl = null;
        String imagePublicId = null;

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            try {
                java.util.Map<?, ?> uploadResult = cloudinaryService.upload(request.getImage());
                imageUrl = (String) uploadResult.get("secure_url");
                imagePublicId = (String) uploadResult.get("public_id");
            } catch (java.io.IOException e) {
                log.error("Failed to upload image to Cloudinary", e);
                throw new BusinessLogicException("Tải ảnh lên Cloudinary thất bại: " + e.getMessage());
            }
        }

        Product product = Product.builder()
                .id(productId)
                .name(request.getName())
                .description(request.getDescription())
                .status(com.nexoracommerce.common.enums.ProductStatus.valueOf(request.getStatus()))
                .imagePublicId(imagePublicId)
                .build();
        Product savedProduct = productRepository.save(product);

        // Initialize default product variant with 0 quantity
        ProductVariant variant = ProductVariant.builder()
                .sku(productId) // default SKU matches productId
                .product(savedProduct)
                .price(request.getPrice())
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

        return savedProduct;
    }

    @Override
    @CacheEvict(value = "products", allEntries = true)
    public Product updateProduct(String productId, com.nexoracommerce.product.dto.request.ProductFormRequest request) {
        Product existingProduct = getProductById(productId);
        existingProduct.setName(request.getName());
        existingProduct.setDescription(request.getDescription());
        existingProduct.setPrice(request.getPrice());
        existingProduct.setStatus(com.nexoracommerce.common.enums.ProductStatus.valueOf(request.getStatus()));

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            try {
                java.util.Map<?, ?> uploadResult = cloudinaryService.upload(request.getImage());
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

        return productRepository.save(existingProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteProduct(String productId) {
        Product product = getProductById(productId);

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
