package com.nexoracommerce.redis.controller;

import com.nexoracommerce.common.response.ApiResponse;
import com.nexoracommerce.redis.dto.StockStatus;
import com.nexoracommerce.redis.dto.request.StockQuantityRequest;
import com.nexoracommerce.redis.service.IRedisStockService;
import com.nexoracommerce.redis.service.IStockSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/v1/admin/stocks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStockController {

    private final IRedisStockService redisStockService;
    private final IStockSyncService stockSyncService;

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<StockStatus>> getProductStock(@PathVariable String productId) {
        StockStatus status = StockStatus.builder()
                .productId(productId)
                .redisStock(redisStockService.getStock(productId))
                .locked(redisStockService.isLocked(productId))
                .build();
        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> setProductStock(
            @PathVariable String productId,
            @Valid @RequestBody StockQuantityRequest request) {
        redisStockService.setStock(productId, request.quantity());
        return ResponseEntity.ok(ApiResponse.ok(null, "Stock set successfully"));
    }

    @PostMapping("/{productId}/synchronizations")
    public ResponseEntity<ApiResponse<Void>> syncProductStock(@PathVariable String productId) {
        stockSyncService.syncProductStock(productId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Product stock synchronized successfully"));
    }

    @PostMapping("/synchronizations")
    public ResponseEntity<ApiResponse<Void>> syncAllStock() {
        stockSyncService.syncAllStock();
        return ResponseEntity.ok(ApiResponse.ok(null, "All stock synchronized successfully"));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProductStock(@PathVariable String productId) {
        redisStockService.deleteStock(productId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Product stock deleted successfully"));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearAllStock() {
        redisStockService.clearAllStock();
        return ResponseEntity.ok(ApiResponse.ok(null, "All Redis stock data cleared successfully"));
    }
}
