package com.nexoracommerce.product.controller;

import com.nexoracommerce.common.response.ApiResponse;
import com.nexoracommerce.product.dto.request.ProductFormRequest;
import com.nexoracommerce.product.dto.response.ProductResponse;
import com.nexoracommerce.product.service.IProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Module", description = "Endpoints for catalog, pricing, and variant administration")
public class ProductController {

    private final IProductService productService;

    @GetMapping
    @Operation(
        summary = "Retrieve all product catalog items with pagination",
        description = "Fetches a page of products sorted by ID in ascending order by default."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products page retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<ProductResponse> productPage = productService.getAllProductsPageable(pageable);
        return ResponseEntity.ok(
                ApiResponse.okWithPagination(
                        productPage.getContent(),
                        ApiResponse.PaginationInfo.from(productPage)
                )
        );
    }

    @GetMapping(params = "keyword")
    @Operation(
        summary = "Search products by keyword",
        description = "Performs a text-based fuzzy search for products containing the keyword in their name or description."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results page retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing or empty keyword parameter")
    })
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(
            @Parameter(description = "Search phrase or term", example = "iPhone")
            @NotBlank(message = "Search keyword is required") @RequestParam String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<ProductResponse> productPage = productService.searchProductsByNamePageable(keyword, pageable);
        return ResponseEntity.ok(
                ApiResponse.okWithPagination(
                        productPage.getContent(),
                        ApiResponse.PaginationInfo.from(productPage)
                )
        );
    }

    @GetMapping("/{productId}")
    @Operation(
        summary = "Retrieve product details by product ID",
        description = "Fetches complete details for a single product catalog item by its custom code."
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

    @GetMapping("/{productId}/stock")
    @Operation(
        summary = "Get available stock count",
        description = "Retrieves current warehouse stock count for a specific product ID."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock count retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<Integer>> getAvailableStock(
            @Parameter(description = "The unique product code", example = "P001")
            @PathVariable String productId) {
        Integer stock = productService.getAvailableStock(productId);
        return ResponseEntity.ok(ApiResponse.ok(stock));
    }

    @GetMapping("/{productId}/availability")
    @Operation(
        summary = "Check product availability status",
        description = "Checks whether a product is in stock and available for sale."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Availability status checked successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<Boolean>> isProductAvailable(
            @Parameter(description = "The unique product code", example = "P001")
            @PathVariable String productId) {
        boolean available = productService.isProductAvailable(productId);
        return ResponseEntity.ok(ApiResponse.ok(available));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Create a new product item",
        description = "Requires ADMIN role. Stores a new product entry along with multipart image file attachments."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product successfully created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Input validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions (must be ADMIN)")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @ModelAttribute ProductFormRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @PutMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Update an existing product item",
        description = "Requires ADMIN role. Modifies properties and uploads/updates product images."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product successfully updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Input validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions (must be ADMIN)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @Parameter(description = "The unique product code", example = "P001")
            @PathVariable String productId,
            @Valid @ModelAttribute ProductFormRequest request) {
        ProductResponse response = productService.updateProduct(productId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Product updated successfully"));
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Delete a single product item",
        description = "Requires ADMIN role. Permanently removes a product from the database catalog."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions (must be ADMIN)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @Parameter(description = "The unique product code", example = "P001")
            @PathVariable String productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Product deleted successfully"));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Bulk delete products (Disabled)",
        description = "Requires ADMIN role. Always fails with a 403 status to prevent bulk deletion accidents."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bulk deletion is disabled for safety")
    })
    public ResponseEntity<ApiResponse<Void>> deleteAllProducts() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiResponse.<Void>failure(
                        "Bulk delete is disabled for safety. Delete products individually using DELETE /api/v1/products/{productId}",
                        HttpStatus.FORBIDDEN.value()
                )
        );
    }
}
