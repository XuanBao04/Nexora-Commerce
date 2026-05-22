package com.shopcart.product.dto.request;

import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import com.shopcart.common.validator.ValidEnum;
import com.shopcart.common.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductFormRequest {
    @Size(max = 100, message = "Product ID must not exceed 100 characters")
    private String id;

    @NotBlank(message = "Product name is required")
    @Size(min = 1, max = 255, message = "Product name must be between 1 and 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Product price is required")
    @Positive(message = "Product price must be greater than 0")
    private Long price;

    @NotBlank(message = "Product status is required")
    @ValidEnum(enumClass = ProductStatus.class, message = "Invalid product status. Must be one of: ACTIVE, INACTIVE")
    private String status;

    private MultipartFile image;
}
