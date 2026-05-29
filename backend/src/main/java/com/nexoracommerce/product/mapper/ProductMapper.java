package com.nexoracommerce.product.mapper;

import com.nexoracommerce.product.entity.Product;
import com.nexoracommerce.product.dto.response.ProductResponse;
import com.nexoracommerce.product.dto.request.ProductRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.nexoracommerce.product.dto.response.ProductVariantResponse;
import com.nexoracommerce.product.dto.response.ProductVariantAttributeResponse;
import com.nexoracommerce.product.entity.ProductVariant;
import java.util.Map;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    ProductResponse toProductResponse(Product product);

    @Mapping(target = "status", expression = "java(request.status() != null ? com.nexoracommerce.common.enums.ProductStatus.valueOf(request.status()) : null)")
    Product toEntity(ProductRequest request);

    List<ProductResponse> toResponseList(List<Product> products);

    Set<ProductResponse> toResponseSet(Set<Product> products);

    default List<ProductVariantResponse> mapVariants(Map<String, ProductVariant> variants) {
        if (variants == null) {
            return List.of();
        }
        return variants.values().stream()
                .map(v -> new ProductVariantResponse(
                        v.getSku(),
                        v.getPrice(),
                        v.getQuantity(),
                        v.getReservedQuantity(),
                        v.getSoldQuantity(),
                        v.getAttributeValues() == null ? List.of() : v.getAttributeValues().stream()
                                .map(attrVal -> new ProductVariantAttributeResponse(
                                        attrVal.getAttribute().getName(),
                                        attrVal.getValue()
                                ))
                                .toList()
                ))
                .toList();
    }
}
