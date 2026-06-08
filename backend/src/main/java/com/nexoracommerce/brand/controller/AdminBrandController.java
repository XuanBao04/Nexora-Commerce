package com.nexoracommerce.brand.controller;

import com.nexoracommerce.brand.dto.response.BrandResponse;
import com.nexoracommerce.brand.service.BrandService;
import com.nexoracommerce.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/brands")
@RequiredArgsConstructor
@Tag(name = "Admin Brand Module", description = "Endpoints for admin to manage brands")
public class AdminBrandController {

    private final BrandService brandService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get all brands", description = "Fetch a paginated list of all brands.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Brands retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getAllBrands(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        
        // Hard limit protection against DoS
        if (pageable.getPageSize() > 100) {
            pageable = PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort());
        }

        Page<BrandResponse> brandPage = brandService.getBrandsPageable(pageable);
        
        return ResponseEntity.ok(
                ApiResponse.okWithPagination(
                        brandPage.getContent(),
                        ApiResponse.PaginationInfo.from(brandPage)
                )
        );
    }
}
