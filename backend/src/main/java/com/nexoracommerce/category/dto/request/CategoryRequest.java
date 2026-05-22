package com.nexoracommerce.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must be at most 100 characters")
    String name,

    @Size(max = 100, message = "Slug must be at most 100 characters")
    String slug, // Optional, auto-generated if blank

    Long parentId // Optional parent category ID for nested categories
) {}
