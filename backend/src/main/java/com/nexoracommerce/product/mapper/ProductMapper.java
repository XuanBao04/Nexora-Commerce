package com.nexoracommerce.product.mapper;

import com.nexoracommerce.product.entity.Product;
import com.nexoracommerce.product.dto.response.ProductResponse;
import com.nexoracommerce.product.dto.request.ProductRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    ProductResponse toProductResponse(Product product);

    @Mapping(target = "status", expression = "java(request.status() != null ? com.nexoracommerce.common.enums.ProductStatus.valueOf(request.status()) : null)")
    Product toEntity(ProductRequest request);

    List<ProductResponse> toResponseList(List<Product> products);

    Set<ProductResponse> toResponseSet(Set<Product> products);
}
