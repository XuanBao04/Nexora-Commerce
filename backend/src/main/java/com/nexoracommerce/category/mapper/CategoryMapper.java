package com.nexoracommerce.category.mapper;

import com.nexoracommerce.category.dto.response.CategoryResponse;
import com.nexoracommerce.category.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    @Mapping(target = "parentId", source = "parent.id")
    CategoryResponse toResponse(Category category);

    List<CategoryResponse> toResponseList(List<Category> categories);

    Set<CategoryResponse> toResponseSet(Set<Category> categories);
}
