package com.nexoracommerce.inventory.mapper;

import com.nexoracommerce.inventory.dto.response.InventoryResponse;
import com.nexoracommerce.product.entity.ProductVariant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InventoryMapper {

    @Mapping(target = "productId", source = "sku")
    @Mapping(target = "availableQuantity", expression = "java(variant.getQuantity() - (variant.getReservedQuantity() == null ? 0 : variant.getReservedQuantity()))")
    @Mapping(target = "reservedQuantity", expression = "java(variant.getReservedQuantity() == null ? 0 : variant.getReservedQuantity())")
    @Mapping(target = "soldQuantity", expression = "java(variant.getSoldQuantity() == null ? 0 : variant.getSoldQuantity())")
    InventoryResponse toResponse(ProductVariant variant);

    List<InventoryResponse> toResponseList(List<ProductVariant> variants);

    Set<InventoryResponse> toResponseSet(Set<ProductVariant> variants);
}
