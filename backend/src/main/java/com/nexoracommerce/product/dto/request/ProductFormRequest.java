package com.nexoracommerce.product.dto.request;

import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import com.nexoracommerce.common.validator.ValidEnum;
import com.nexoracommerce.common.enums.ProductStatus;

public record ProductFormRequest(
    @Size(max = 100, message = "Product ID must not exceed 100 characters")
    String id,

    @NotBlank(message = "Product name is required")
    @Size(min = 1, max = 255, message = "Product name must be between 1 and 255 characters")
    String name,

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description,

    @NotNull(message = "Product price is required")
    @Positive(message = "Product price must be greater than 0")
    Long price,

    @NotBlank(message = "Product status is required")
    @ValidEnum(enumClass = ProductStatus.class, message = "Invalid product status. Must be one of: ACTIVE, INACTIVE")
    String status,

    MultipartFile image,

    String variants
) {}
