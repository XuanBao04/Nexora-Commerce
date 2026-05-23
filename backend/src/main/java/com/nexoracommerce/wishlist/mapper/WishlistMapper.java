package com.nexoracommerce.wishlist.mapper;

import com.nexoracommerce.wishlist.dto.response.WishlistResponse;
import com.nexoracommerce.wishlist.entity.Wishlist;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WishlistMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productPrice", expression = "java(wishlist.getProduct().getPrice())")
    WishlistResponse toResponse(Wishlist wishlist);

    List<WishlistResponse> toResponseList(List<Wishlist> wishlists);
}
