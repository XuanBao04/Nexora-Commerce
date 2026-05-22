package com.nexoracommerce.inventory.controller;

import com.nexoracommerce.common.response.ApiResponse;
import com.nexoracommerce.inventory.dto.request.UpdateStockRequest;
import com.nexoracommerce.inventory.dto.response.InventoryResponse;
import com.nexoracommerce.inventory.service.IInventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final IInventoryService inventoryService;

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventoryDetails(@PathVariable String productId) {
        InventoryResponse details = inventoryService.getInventoryDetails(productId);
        return ResponseEntity.ok(ApiResponse.ok(details));
    }

    @GetMapping("/{productId}/stock")
    public ResponseEntity<ApiResponse<Integer>> getStock(@PathVariable String productId) {
        Integer stock = inventoryService.getStock(productId);
        return ResponseEntity.ok(ApiResponse.ok(stock));
    }

    @GetMapping("/{productId}/availability")
    public ResponseEntity<ApiResponse<Boolean>> hasEnoughStock(
            @PathVariable String productId,
            @Positive(message = "Quantity must be greater than 0") @RequestParam Integer quantity) {
        boolean hasStock = inventoryService.hasEnoughStock(productId, quantity);
        return ResponseEntity.ok(ApiResponse.ok(hasStock));
    }

    @PatchMapping("/{productId}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateStock(
            @PathVariable String productId,
            @Valid @RequestBody UpdateStockRequest request) {
        inventoryService.updateStock(productId, request.quantity());
        return ResponseEntity.ok(ApiResponse.ok(null, "Stock updated successfully"));
    }

    @PostMapping("/{productId}/stock-reservations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reserveStock(
            @PathVariable String productId,
            @Valid @RequestBody UpdateStockRequest request) {
        inventoryService.reserveStock(productId, request.quantity());
        return ResponseEntity.ok(ApiResponse.ok(null, "Stock reserved successfully"));
    }

    @PostMapping("/{productId}/stock-releases")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> releaseStock(
            @PathVariable String productId,
            @Valid @RequestBody UpdateStockRequest request) {
        inventoryService.releaseStock(productId, request.quantity());
        return ResponseEntity.ok(ApiResponse.ok(null, "Reserved stock released successfully"));
    }
}
