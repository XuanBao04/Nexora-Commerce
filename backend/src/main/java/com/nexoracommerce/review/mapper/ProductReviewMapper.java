package com.nexoracommerce.review.mapper;

import com.nexoracommerce.review.dto.response.ProductReviewResponse;
import com.nexoracommerce.review.entity.ProductReview;
import com.nexoracommerce.review.entity.ReviewImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductReviewMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "variantSku", source = "variant.sku")
    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "imageUrls", source = "images")
    ProductReviewResponse toResponse(ProductReview review);

    List<ProductReviewResponse> toResponseList(List<ProductReview> reviews);

    default List<String> mapImages(List<ReviewImage> images) {
        if (images == null) return null;
        return images.stream().map(ReviewImage::getImageUrl).toList();
    }
}
