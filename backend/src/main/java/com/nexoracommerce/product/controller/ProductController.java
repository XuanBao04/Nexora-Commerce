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

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final IProductService productService;

    @GetMapping
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
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(
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
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable String productId) {
        ProductResponse response = productService.getProductById(productId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{productId}/stock")
    public ResponseEntity<ApiResponse<Integer>> getAvailableStock(@PathVariable String productId) {
        Integer stock = productService.getAvailableStock(productId);
        return ResponseEntity.ok(ApiResponse.ok(stock));
    }

    @GetMapping("/{productId}/availability")
    public ResponseEntity<ApiResponse<Boolean>> isProductAvailable(@PathVariable String productId) {
        boolean available = productService.isProductAvailable(productId);
        return ResponseEntity.ok(ApiResponse.ok(available));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @ModelAttribute ProductFormRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @PutMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable String productId,
            @Valid @ModelAttribute ProductFormRequest request) {
        ProductResponse response = productService.updateProduct(productId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Product updated successfully"));
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable String productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Product deleted successfully"));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAllProducts() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiResponse.<Void>failure(
                        "Bulk delete is disabled for safety. Delete products individually using DELETE /api/v1/products/{productId}",
                        HttpStatus.FORBIDDEN.value()
                )
        );
    }
}
