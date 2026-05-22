package com.nexoracommerce.inventory.controller;

import com.nexoracommerce.inventory.dto.request.UpdateStockRequest;
import com.nexoracommerce.inventory.dto.response.InventoryResponse;
import com.nexoracommerce.inventory.service.IInventoryService;
import com.nexoracommerce.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Inventory operations
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final IInventoryService inventoryService;

    /**
     * Get available stock for a product
     * @param productId the product ID
     * @return ApiResponse with available stock quantity
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<Integer>> getStock(@PathVariable String productId) {
        Integer stock = inventoryService.getStock(productId);
        return ResponseEntity.ok(ApiResponse.ok(stock));
    }

    /**
     * Get detailed inventory information for a product
     * @param productId the product ID
     * @return ApiResponse with InventoryResponse
     */
    @GetMapping("/{productId}/details")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventoryDetails(@PathVariable String productId) {
        InventoryResponse details = inventoryService.getInventoryDetails(productId);
        return ResponseEntity.ok(ApiResponse.ok(details));
    }

    /**
     * Update stock for a product (ADMIN only)
     * @param productId the product ID
     * @param request UpdateStockRequest with new quantity
     * @return ApiResponse with success message
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> updateStock(
            @PathVariable String productId,
            @Valid @RequestBody UpdateStockRequest request) {
        inventoryService.updateStock(productId, request.getQuantity());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * Reserve stock for an order (ADMIN only)
     * @param productId the product ID
     * @param quantity the quantity to reserve
     * @return ApiResponse with success message
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{productId}/reserve")
    public ResponseEntity<ApiResponse<Void>> reserveStock(
            @PathVariable String productId,
            @RequestParam Integer quantity) {
        inventoryService.reserveStock(productId, quantity);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * Release reserved stock (ADMIN only)
     * @param productId the product ID
     * @param quantity the quantity to release
     * @return ApiResponse with success message
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{productId}/release")
    public ResponseEntity<ApiResponse<Void>> releaseStock(
            @PathVariable String productId,
            @RequestParam Integer quantity) {
        inventoryService.releaseStock(productId, quantity);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * Check if product has enough stock
     * @param productId the product ID
     * @param quantity the required quantity
     * @return ApiResponse with availability status
     */
    @GetMapping("/{productId}/check")
    public ResponseEntity<ApiResponse<Boolean>> hasEnoughStock(
            @PathVariable String productId,
            @RequestParam Integer quantity) {
        boolean hasStock = inventoryService.hasEnoughStock(productId, quantity);
        return ResponseEntity.ok(ApiResponse.ok(hasStock));
    }
}
