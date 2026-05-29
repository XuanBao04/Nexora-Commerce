package com.nexoracommerce.brand.controller;

import com.nexoracommerce.brand.dto.request.BrandRequest;
import com.nexoracommerce.brand.dto.response.BrandResponse;
import com.nexoracommerce.brand.service.IBrandService;
import com.nexoracommerce.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * REST Controller for brand management
 * Handles brand CRUD operations and brand catalog queries
 */
@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
@Tag(name = "Brand Module", description = "Endpoints for product brands, brand catalog, and administration")
public class BrandController {

    private final IBrandService brandService;

    @GetMapping
    @Operation(
        summary = "Get all brands",
        description = "Retrieves a list of all available product brands in the catalog."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Brands retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getAllBrands() {
        List<BrandResponse> response = brandService.getAllBrands();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{idOrSlug}")
    @Operation(
        summary = "Get brand details by ID or slug",
        description = "Retrieves brand details by either its database ID or URL slug."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Brand details retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Brand not found")
    })
    public ResponseEntity<ApiResponse<BrandResponse>> getBrand(
            @Parameter(description = "Brand ID or slug", example = "apple")
            @PathVariable String idOrSlug) {
        BrandResponse response = brandService.getBrand(idOrSlug);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Create a new brand (Admin only)",
        description = "Requires ADMIN role. Creates a new product brand entry."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Brand created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions (must be ADMIN)")
    })
    public ResponseEntity<ApiResponse<BrandResponse>> createBrand(
            @Valid @RequestBody BrandRequest request) {
        BrandResponse response = brandService.createBrand(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @PutMapping("/{brandId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Update an existing brand (Admin only)",
        description = "Requires ADMIN role. Modifies brand properties like name, logo, or slug."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Brand updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions (must be ADMIN)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Brand not found")
    })
    public ResponseEntity<ApiResponse<BrandResponse>> updateBrand(
            @Parameter(description = "Brand database ID", example = "1")
            @PathVariable Long brandId,
            @Valid @RequestBody BrandRequest request) {
        BrandResponse response = brandService.updateBrand(brandId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Brand updated successfully"));
    }

    @DeleteMapping("/{brandId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Delete a brand (Admin only)",
        description = "Requires ADMIN role. Permanently removes a brand from the system."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Brand deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions (must be ADMIN)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Brand not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteBrand(
            @Parameter(description = "Brand database ID", example = "1")
            @PathVariable Long brandId) {
        brandService.deleteBrand(brandId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Brand deleted successfully"));
    }
}
