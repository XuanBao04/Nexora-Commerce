package com.nexoracommerce.product.mapper;

import com.nexoracommerce.product.entity.Product;
import com.nexoracommerce.product.dto.response.ProductResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

  
    public ProductResponse toProductResponse(Product product) {
        if (product == null) {
            return null;
        }

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .status(product.getStatus() != null ? product.getStatus().toString() : null)
                .imageUrl(product.getImageUrl())
                .imagePublicId(product.getImagePublicId())
                .build();
    }

   
    public Product toEntity(Product product) {
        return product;
    }

    public Product toEntity(com.nexoracommerce.product.dto.request.ProductRequest request) {
        if (request == null) {
            return null;
        }

        Product product = Product.builder()
                .id(request.getId())
                .name(request.getName())
                .description(request.getDescription())
                .status(request.getStatus() != null ? com.nexoracommerce.common.enums.ProductStatus.valueOf(request.getStatus()) : null)
                .build();
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        return product;
    }
}
