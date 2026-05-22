---
name: spring-dto-mapping-skill
description: Enforce immutable Java 21 Record DTO designs, compile-time MapStruct mappings, context-driven request/response separation, and strict JSR-303 validation rules for Spring Boot 3.x enterprise applications.
---

## Scope & Activation Rules

Activate this skill when:
- Designing or modifying DTOs (Data Transfer Objects) for request payloads or API responses.
- Writing or refactoring data mapper layers (e.g., MapStruct interfaces, conversion helpers).
- Tying controllers to service layers and handling data mapping logic.
- Implementing request validation constraints (`@Valid`, JSR-303 annotations).
- Reviewing performance bottlenecks related to object mapping, heap allocation, or JSON serialization.

---

## System Directives

### DO

- **Use Java 21 Records for all DTOs**: Ensure that all request payloads and response bodies are defined as Java `record` types. This guarantees complete immutability, thread-safety, clear syntax, and efficient memory layout without Lombok boilerplates.
- **Enforce Compile-Time MapStruct Mapping**: Mandate `MapStruct` as the exclusive mapping library. Configure it with `componentModel = "spring"` so that mappers are generated as fully managed Spring beans at compile-time.
- **Enforce Context-Driven DTO Separation**: Always separate Request DTOs and Response DTOs per business use case. Apply the "One Screen/One Operation = One DTO" guideline. E.g., `ProductCreateRequest`, `ProductUpdateRequest`, `ProductDetailResponse`, and `ProductPreviewResponse`.
- **Apply JSR-303 Validation on Request DTOs**: Restrict valid inputs strictly at the API entry point by placing annotations like `@NotBlank`, `@NotNull`, `@Size`, `@Min`, `@Max`, `@Email`, and `@Pattern` directly inside Request Record definitions.
- **Configure Custom Null Safety in MapStruct**: Use annotations such as `@MappingTarget` and configure `nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE` for partial updates to avoid overwriting existing entity fields with `null`.
- **Leverage MapStruct Mapping Inheritance**: Utilize `@InheritConfiguration` and `@InheritInverseConfiguration` to reuse mapping configurations across similar operations (e.g., mapping lists or symmetric request/response logic).
- **Use Nested Records for Complex/Composed Data Structures**: Group related properties in request or response records (e.g., `AddressRecord`, `PaymentInfoRecord`) instead of flattening everything into a single mammoth DTO.
- **Keep Service Layer Pure**: Only pass DTOs into service methods and return DTOs out of service methods. Do not leak database entities to controllers or clients.

### DO NOT

- **Do NOT Use ModelMapper, BeanUtils, or Reflection**: Never import or use reflection-based mapping tools (`org.modelmapper`, `org.springframework.beans.BeanUtils`, Apache Commons `BeanUtils`). They consume massive heap resources, bypass compiler checks, fail silently, and degrade throughput by up to 100x.
- **Do NOT Write Manual Getter/Setter Chains in Services**: Never write repetitive manual assignment loops or boilerplate code (`entity.setName(dto.name())`) in the Service Layer. This bloats services with mapping logic, introduces null-pointer risks, and violates single-responsibility principles.
- **Do NOT Reuse DTOs Across Contexts**: Never reuse a single DTO for multiple separate CRUD contexts (e.g., sharing a single `ProductDto` for creating, updating, details retrieval, and list preview). This leads to fields being nullable where they shouldn't be and exposes the system to data injection or data leakage vulnerabilities.
- **Do NOT Use Mutable POJOs (Lombok `@Data` or `@Setter`) for DTOs**: Never define DTO classes with getters and setters. Mutable DTOs can be modified mid-transit, introducing bugs in multithreaded environments and breaking serialization cache invariants.
- **Do NOT Validate Response DTOs**: Do not pollute response records with JSR-303 validations. Validation belongs on requests/inputs, not on responses.
- **Do NOT Reference JPA Entities Inside DTO Records**: Never declare a JPA entity field inside a DTO. Doing so leaks the persistence context, risks unintended database writes (dirty checking), and triggers `LazyInitializationException` during JSON serialization.

---

## Production Reference Implementation

Here is the reference implementation of a context-driven, record-based E-commerce Product Creation flow, demonstrating strict separation, JSR-303 validation, MapStruct compile-time mapping, and a clean Service layer.

### 1. Request DTOs (Immutable Records with JSR-303 Validation)

```java
package com.nexoracommerce.product.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Immutable Request Record for creating a new Product.
 * Implements strict input boundary validations.
 */
public record ProductCreateRequest(
    @NotBlank(message = "Product name must not be blank")
    @Size(min = 3, max = 150, message = "Product name must be between 3 and 150 characters")
    String name,

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description,

    @NotBlank(message = "SKU must not be blank")
    @Pattern(regexp = "^[A-Z0-9_-]{5,30}$", message = "SKU must be alphanumeric uppercase, 5 to 30 characters")
    String sku,

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than or equal to 0.01")
    BigDecimal price,

    @NotNull(message = "Category ID is required")
    Long categoryId,

    @NotNull(message = "Brand ID is required")
    Long brandId,

    @NotEmpty(message = "Product must have at least one variant")
    @Valid
    List<VariantCreateRequest> variants,

    @Size(max = 5, message = "Product can have a maximum of 5 images")
    @Valid
    List<ImageCreateRequest> images
) {}
```

```java
package com.nexoracommerce.product.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record VariantCreateRequest(
    @NotBlank(message = "Variant SKU must not be blank")
    @Pattern(regexp = "^[A-Z0-9_-]{5,35}$", message = "Variant SKU must be uppercase alphanumeric")
    String sku,

    @NotBlank(message = "Variant name/size is required")
    String name,

    @NotNull(message = "Price override is required")
    @DecimalMin(value = "0.00", message = "Price override must be non-negative")
    BigDecimal price,

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock cannot be negative")
    Integer stock
) {}
```

```java
package com.nexoracommerce.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImageCreateRequest(
    @NotBlank(message = "Image URL must not be blank")
    @Size(max = 255, message = "Image URL must not exceed 255 characters")
    String imageUrl,

    boolean isPrimary
) {}
```

### 2. Response DTOs (Immutable Records for API Output Projection)

```java
package com.nexoracommerce.product.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Immutable Response Record designed specifically for detailed product screens.
 * Exposes computed properties and formatted values safely without leaking entity states.
 */
public record ProductDetailResponse(
    Long id,
    String name,
    String description,
    String sku,
    BigDecimal price,
    Long categoryId,
    String categoryName,
    Long brandId,
    String brandName,
    List<VariantResponse> variants,
    List<ImageResponse> images,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

```java
package com.nexoracommerce.product.dto.response;

import java.math.BigDecimal;

public record VariantResponse(
    String sku,
    String name,
    BigDecimal price,
    Integer stock,
    boolean isAvailable
) {}
```

```java
package com.nexoracommerce.product.dto.response;

public record ImageResponse(
    Long id,
    String imageUrl,
    boolean isPrimary
) {}
```

### 3. Compile-Time MapStruct Mapper Interface

```java
package com.nexoracommerce.product.mapper;

import com.nexoracommerce.product.dto.request.ProductCreateRequest;
import com.nexoracommerce.product.dto.request.VariantCreateRequest;
import com.nexoracommerce.product.dto.response.ProductDetailResponse;
import com.nexoracommerce.product.dto.response.VariantResponse;
import com.nexoracommerce.product.entity.Product;
import com.nexoracommerce.product.entity.ProductVariant;
import org.mapstruct.*;

import java.util.List;

/**
 * High-performance compile-time object mapper powered by MapStruct.
 * Zero reflection, zero runtime cost, and fully integrated with Spring's ApplicationContext.
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ProductMapper {

    // 1. Mapping Request Record to Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true) // Handled in Service Layer
    @Mapping(target = "brand", ignore = true)    // Handled in Service Layer
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "variants", source = "variants")
    @Mapping(target = "images", source = "images")
    Product toEntity(ProductCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true) // Handled by bidirectional helper
    ProductVariant toVariantEntity(VariantCreateRequest request);

    // 2. Mapping Entity to Response Record
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "brandId", source = "brand.id")
    @Mapping(target = "brandName", source = "brand.name")
    ProductDetailResponse toDetailResponse(Product product);

    @Mapping(target = "isAvailable", expression = "java(variant.getStock() > 0)")
    VariantResponse toVariantResponse(ProductVariant variant);

    List<ProductDetailResponse> toDetailResponseList(List<Product> products);

    // Bidirectional helper binder setup automatically
    @AfterMapping
    default void linkRelationships(@MappingTarget Product product) {
        if (product.getVariants() != null) {
            product.getVariants().forEach(variant -> variant.setProduct(product));
        }
        if (product.getImages() != null) {
            product.getImages().forEach(image -> image.setProduct(product));
        }
    }
}
```

### 4. Service Layer Integration (No Manual Setters/Getters, Clean & Pure)

```java
package com.nexoracommerce.product.service.impl;

import com.nexoracommerce.brand.entity.Brand;
import com.nexoracommerce.brand.repository.BrandRepository;
import com.nexoracommerce.category.entity.Category;
import com.nexoracommerce.category.repository.CategoryRepository;
import com.nexoracommerce.product.dto.request.ProductCreateRequest;
import com.nexoracommerce.product.dto.response.ProductDetailResponse;
import com.nexoracommerce.product.entity.Product;
import com.nexoracommerce.product.mapper.ProductMapper;
import com.nexoracommerce.product.repository.ProductRepository;
import com.nexoracommerce.product.service.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements IProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductDetailResponse createProduct(ProductCreateRequest request) {
        // 1. Fetch categories and brands (handling reference checks early)
        Category category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + request.categoryId()));
            
        Brand brand = brandRepository.findById(request.brandId())
            .orElseThrow(() -> new IllegalArgumentException("Brand not found with ID: " + request.brandId()));

        // 2. Perform zero-cost compile-time mapping from Record to JPA Entity
        Product product = productMapper.toEntity(request);
        
        // 3. Set associated managed entities
        product.setCategory(category);
        product.setBrand(brand);

        // 4. Save to Database
        Product savedProduct = productRepository.save(product);

        // 5. Convert entity back to Response Record for pure output boundary representation
        return productMapper.toDetailResponse(savedProduct);
    }
}
```

---

## Anti-Patterns & Automated Fixes

### Anti-Pattern 1: Reflection-based Object Mapping (ModelMapper / BeanUtils)
*Description*: Calling libraries that inspect getters and setters at runtime via reflection. This causes silent failures on mismatched fields, huge memory footprint, and CPU throttling.

#### Bad Code
```java
// VIOLATION: Reflection-based runtime mapping
import org.modelmapper.ModelMapper;

public ProductDetailResponse mapToResponse(Product product) {
    ModelMapper modelMapper = new ModelMapper();
    return modelMapper.map(product, ProductDetailResponse.class); // Slow, untyped, fails silently
}
```

#### Fixed Code
```java
// FIXED: Explicit compile-time mapping with MapStruct injection
private final ProductMapper productMapper;

public ProductDetailResponse mapToResponse(Product product) {
    return productMapper.toDetailResponse(product); // Native bytecode mapping, verified at compile-time
}
```

---

### Anti-Pattern 2: Manual Getter/Setter Bloat in Service Methods
*Description*: Writing massive blocks of manual set/get code inside business logic layers, increasing line count, testing requirements, and spelling mistake risks.

#### Bad Code
```java
// VIOLATION: Service layer cluttered with mapping boilerplate and manual calls
@Transactional
public ProductResponse createProductLegacy(ProductCreateRequest request) {
    Product product = new Product();
    product.setName(request.getName());
    product.setDescription(request.getDescription());
    product.setPrice(request.getPrice());
    product.setSku(request.getSku());
    // ... 50 lines of variants manual conversion ...
    Product saved = productRepository.save(product);
    
    ProductResponse response = new ProductResponse();
    response.setId(saved.getId());
    response.setName(saved.getName());
    return response;
}
```

#### Fixed Code
```java
// FIXED: Service method handles business logic only, delegating mapping cleanly
@Transactional
public ProductDetailResponse createProduct(ProductCreateRequest request) {
    Product product = productMapper.toEntity(request);
    Product saved = productRepository.save(product);
    return productMapper.toDetailResponse(saved);
}
```

---

### Anti-Pattern 3: Reusing a Single DTO/POJO Across Multiple Layers and HTTP Verbs
*Description*: Using a single mutable class for creating, updating, and returning resources. This allows API clients to inject values into read-only fields or leaves critical validation annotations ineffective.

#### Bad Code
```java
// VIOLATION: Mutable POJO used for all operations
@Data
public class ProductDto {
    private Long id; // Read-only on Response, but exposed to injection on POST Create
    @NotBlank
    private String name;
    private LocalDateTime createdAt; // Modified by malicious clients easily
}
```

#### Fixed Code
```java
// FIXED: Separate, context-driven immutable records
public record ProductCreateRequest(
    @NotBlank String name
) {}

public record ProductResponse(
    Long id,
    String name,
    LocalDateTime createdAt
) {}
```

---

## Verification Commands

To verify that the project is strictly adhering to compiler-safe compile-time DTO record mapping and has zero reflection-based mapping components, run the following automated checks.

### 1. Execute Clean Compile and Check MapStruct Class Generation
Run Maven compile. This will trigger MapStruct's annotation processor (`mapstruct-processor`) to generate type-safe implementation classes:
```bash
mvn clean compile -DskipTests
```
Ensure that the output builds successfully. Confirm the generated implementation files exist at:
```bash
ls -la target/generated-sources/annotations/com/nexoracommerce/*/mapper/
```
All Mapper interfaces must have matching `Impl.java` counterparts generated at this path containing pure Java getters/setters/initializers.

### 2. Guardrails Scan Against Reflection Mapping Libraries
Scan the project codebase and configuration descriptors to guarantee no reflection mapper imports are left:
```bash
# Ensure no ModelMapper references exist
! grep -rn "org.modelmapper" src/

# Ensure no Spring BeanUtils copyProperties are called
! grep -rn "BeanUtils.copyProperties" src/
```

### 3. Ensure Strict Validation Compliance
Check that controllers are utilizing `@Valid` at request bodies to enforce Record validations:
```bash
# Ensure controllers use @Valid alongside RequestBody
grep -rn "@RequestBody @Valid" src/main/java/com/nexoracommerce/
```
