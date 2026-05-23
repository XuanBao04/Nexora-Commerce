---
name: springdoc-openapi-skill
description: Rules and guidelines for designing comprehensive, secure, and developer-friendly REST API documentation using SpringDoc OpenAPI 3 & Swagger UI in Spring Boot 3.x.
---

## Scope & Activation Rules
This skill is activated automatically when creating, configuring, or modifying any **Swagger/OpenAPI configuration class**, REST API controllers, or request/response DTOs in a Spring Boot application. All API documentation elements must strictly adhere to these rules to maintain clear, secure, and standardized developer-facing contracts.

---

## System Directives

### DO
1. **Configure Global Security Scheme:** Configure a global JWT Bearer Security Scheme (SecurityScheme of type HTTP with Bearer format) in a central configuration class. This generates the global **"Authorize"** round padlock button on the Swagger UI, allowing frontend developers to enter their JWT token once to authorize all secure endpoints automatically.
2. **Implement Package-based Selective Grouping:** Use SpringDoc's `GroupedOpenApi` bean to scan only the business controller packages (e.g., `com.nexoracommerce.*.controller`). Explicitly hide Spring Boot's internal, actuator, or default framework endpoints.
3. **Use Descriptive Operation Metadata:** Annotate every controller endpoint method with `@Operation`. Provide a concise `summary` (e.g., `"Create a new product"`) and a detailed `description` explaining pre-requisites, side effects, database triggers, or security roles required (e.g., `"Requires role ADMIN. Creates a product entity and uploads primary image to Cloudinary."`).
4. **Document Standard Error Scenarios:** Decorate operations with `@ApiResponses` containing `@ApiResponse` details for every exception handled by the `@RestControllerAdvice`. Always model common errors:
   - `400 Bad Request` (input validation failures).
   - `401 Unauthorized` (missing/expired token).
   - `403 Forbidden` (lack of role/permissions).
   - `404 Not Found` (resource missing).
5. **Describe DTO schemas with Examples:** Annotate Java 21 DTO records and their fields with `@Schema`. Provide a descriptive `description` and a helpful, realistic `example` value for each field. This drastically improves the auto-generated client model generation for React/TypeScript frontend developers.

### DO NOT
1. **Never Hardcode Token Parameters:** Do not add individual query parameters, headers, or `@RequestHeader("Authorization")` parameters to controller method signatures to support tokens. Rely entirely on the global `@SecurityRequirement(name = "bearerAuth")` declaration.
2. **Never Expose Default Actuator or Error Routes:** Do not let `/error`, `/actuator`, `/profile`, or similar framework endpoints clutter the public business API documentation. Keep groups distinct.
3. **Never Leave Swagger Unsecured in Sensitive Environments:** Do not allow public access to documentation in production environments without authorization. Use profile-based switching (`@Profile({"dev", "staging"})`) or Spring Security filters to guard it.

---

## Standard Reference Pattern

Below is the production-ready clean architecture blueprint illustrating a global OpenAPI configuration and an annotated controller.

### 1. Global OpenAPI Central Configuration (`OpenApiConfig.java`)

```java
package com.nexoracommerce.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Global Configuration for SpringDoc OpenAPI 3.
 * Configures package filtering and JWT security schemes globally.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("Nexora-Public-API")
                .pathsToMatch("/api/v1/**")
                .packagesToScan("com.nexoracommerce")
                .build();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Nexora-Commerce REST API")
                        .version("1.0.0")
                        .description("E-Commerce high-performance RESTful API documentation for frontend integration.")
                        .contact(new Contact()
                                .name("Nexora Core Dev Team")
                                .email("dev@nexoracommerce.com")))
                // Integrate standard "Authorize" lock button at top-right of Swagger UI
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Please paste your JWT Access Token in the box below to authorize.")));
    }
}
```

### 2. Beautifully Annotated DTO (`ProductFormRequest.java`)

```java
package com.nexoracommerce.product.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Schema(description = "DTO representing the input details required to create or update a product")
public record ProductFormRequest(
    @Schema(description = "Unique Custom product ID code", example = "P001")
    @NotBlank(message = "Product ID is required")
    String id,

    @Schema(description = "Full commercially attractive product name", example = "iPhone 15 Pro Max 256GB")
    @NotBlank(message = "Product name is required")
    String name,

    @Schema(description = "Detailed HTML or markdown product description", example = "The latest flagship iPhone featuring titanium frame and A17 Pro chip.")
    String description,

    @Schema(description = "Retail sale price in Vietnamese Dong (VND)", example = "34990000")
    @Positive(message = "Price must be greater than 0")
    Long price,

    @Schema(description = "Parent Category database primary key ID", example = "12")
    Long categoryId,

    @Schema(description = "Brand database primary key ID", example = "3")
    Long brandId
) {}
```

### 3. Comprehensive Controller Documentation Pattern (`ProductController.java`)

```java
package com.nexoracommerce.product.controller;

import com.nexoracommerce.common.response.ApiResponse;
import com.nexoracommerce.product.dto.request.ProductFormRequest;
import com.nexoracommerce.product.dto.response.ProductResponse;
import com.nexoracommerce.product.service.IProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Module", description = "Endpoints for catalog, pricing, and variant administration")
public class ProductController {

    private final IProductService productService;

    @GetMapping("/{productId}")
    @Operation(
        summary = "Retrieve product details by unique product ID",
        description = "Fetches a product's full details, including active categories, brands, images, and embedded variant items."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product details retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found under the provided ID")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @Parameter(description = "The unique product code", example = "P001")
            @PathVariable String productId) {
        
        ProductResponse response = productService.getProductById(productId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Create a new product catalog item",
        description = "Requires ADMIN role. Stores a new product catalog entry and binds category and brand references."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product catalog created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation parameters failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Bearer credentials missing or malformed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient role permissions (must be ADMIN)")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductFormRequest request) {
        
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }
}
```
