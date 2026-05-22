package com.nexoracommerce.brand.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BrandRequest(
    @NotBlank(message = "Brand name is required")
    @Size(max = 100, message = "Brand name must be at most 100 characters")
    String name,

    @Size(max = 100, message = "Slug must be at most 100 characters")
    String slug // Optional, auto-generated if blank
) {}
