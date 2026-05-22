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

import java.util.List;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandController {

    private final IBrandService brandService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getAllBrands() {
        List<BrandResponse> response = brandService.getAllBrands();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{idOrSlug}")
    public ResponseEntity<ApiResponse<BrandResponse>> getBrand(@PathVariable String idOrSlug) {
        BrandResponse response = brandService.getBrand(idOrSlug);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BrandResponse>> createBrand(@Valid @RequestBody BrandRequest request) {
        BrandResponse response = brandService.createBrand(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @PutMapping("/{brandId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BrandResponse>> updateBrand(
            @PathVariable Long brandId,
            @Valid @RequestBody BrandRequest request) {
        BrandResponse response = brandService.updateBrand(brandId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Brand updated successfully"));
    }

    @DeleteMapping("/{brandId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBrand(@PathVariable Long brandId) {
        brandService.deleteBrand(brandId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Brand deleted successfully"));
    }
}
