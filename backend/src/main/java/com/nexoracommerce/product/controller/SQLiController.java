package com.nexoracommerce.product.controller;

import com.nexoracommerce.common.response.ApiResponse;
import com.nexoracommerce.product.dto.response.ProductResponse;
import com.nexoracommerce.product.service.ISqliService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/security-demos/sql-injections")
@RequiredArgsConstructor
public class SQLiController {

    private final ISqliService sqliService;

    @GetMapping("/vulnerable-products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchVulnerable(
            @NotBlank(message = "Product name is required") @RequestParam String name) {
        List<ProductResponse> response = sqliService.searchVulnerable(name);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/secure-products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchSecure(
            @NotBlank(message = "Product name is required") @RequestParam String name) {
        List<ProductResponse> response = sqliService.searchSecure(name);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
