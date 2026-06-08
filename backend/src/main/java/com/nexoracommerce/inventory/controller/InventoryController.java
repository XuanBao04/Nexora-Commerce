package com.nexoracommerce.inventory.controller;

import com.nexoracommerce.common.response.ApiResponse;
import com.nexoracommerce.inventory.dto.request.UpdateStockRequest;
import com.nexoracommerce.inventory.dto.response.InventoryResponse;
import com.nexoracommerce.inventory.service.InventoryService;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller for inventory and stock management
 * Handles stock level queries, availability checks, and admin stock operations
 */
@Validated
@RestController
@RequestMapping("/api/v1/inventories")
@RequiredArgsConstructor
@Tag(name = "Inventory Module", description = "Endpoints for stock management, availability checking, reservations, and admin inventory operations")
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * GET /api/v1/inventories/{productId} - Get full inventory details
     */
    @GetMapping("/{productId}")
    @Operation(
        summary = "Get detailed inventory information for a product",
        description = "Retrieves complete inventory details including available stock, reserved quantity, sold count, and pricing."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inventory details retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventoryDetails(
            @Parameter(description = "The unique product code", example = "P001")
            @PathVariable String productId) {
        InventoryResponse details = inventoryService.getInventoryDetails(productId);
        return ResponseEntity.ok(ApiResponse.ok(details));
    }

    /**
     * GET /api/v1/inventories/{productId}/stock - Get current stock count
     */
    @GetMapping("/{productId}/stock")
    @Operation(
        summary = "Get current stock count for a product",
        description = "Returns the current available stock quantity (total stock minus reserved and sold items)."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock count retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<Integer>> getStock(
            @Parameter(description = "The unique product code", example = "P001")
            @PathVariable String productId) {
        Integer stock = inventoryService.getStock(productId);
        return ResponseEntity.ok(ApiResponse.ok(stock));
    }

    /**
     * GET /api/v1/inventories/{productId}/availability - Check product availability
     */
    @GetMapping("/{productId}/availability")
    @Operation(
        summary = "Check product availability for a specific quantity",
        description = "Verifies whether a product has sufficient stock available to fulfill a requested quantity."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Availability status checked successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid quantity parameter"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<Boolean>> hasEnoughStock(
            @Parameter(description = "The unique product code", example = "P001")
            @PathVariable String productId,
            @Parameter(description = "Quantity to check availability for", example = "5")
            @Positive(message = "Quantity must be greater than 0") @RequestParam Integer quantity) {
        boolean hasStock = inventoryService.hasEnoughStock(productId, quantity);
        return ResponseEntity.ok(ApiResponse.ok(hasStock));
    }

    /**
     * PATCH /api/v1/inventories/{productId}/stock - Update product stock (Admin only)
     */
    @PatchMapping("/{productId}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Update product stock quantity (Admin only)",
        description = "Requires ADMIN role. Manually adjusts total stock quantity for inventory correction or restocking."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid update request or quantity invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions (must be ADMIN)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<Void>> updateStock(
            @Parameter(description = "The unique product code", example = "P001")
            @PathVariable String productId,
            @Valid @RequestBody UpdateStockRequest request) {
        inventoryService.updateStock(productId, request.quantity());
        return ResponseEntity.ok(ApiResponse.ok(null, "Stock updated successfully"));
    }

    /**
     * POST /api/v1/inventories/{productId}/stock-reservations - Reserve stock for orders
     */
    @PostMapping("/{productId}/stock-reservations")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Reserve stock for pending orders (Admin only)",
        description = "Requires ADMIN role. Reserves specific quantity of stock from available inventory for a pending order. " +
                "Reserved stock cannot be sold to other customers."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock reserved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Insufficient stock or invalid request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions (must be ADMIN)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<Void>> reserveStock(
            @Parameter(description = "The unique product code", example = "P001")
            @PathVariable String productId,
            @Valid @RequestBody UpdateStockRequest request) {
        inventoryService.reserveStock(productId, request.quantity());
        return ResponseEntity.ok(ApiResponse.ok(null, "Stock reserved successfully"));
    }

    /**
     * POST /api/v1/inventories/{productId}/stock-releases - Release reserved stock
     */
    @PostMapping("/{productId}/stock-releases")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Release reserved stock back to available inventory (Admin only)",
        description = "Requires ADMIN role. Releases previously reserved stock back into available inventory. " +
                "Used when orders are cancelled or payment fails."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reserved stock released successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid release request or insufficient reserved stock"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions (must be ADMIN)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<Void>> releaseStock(
            @Parameter(description = "The unique product code", example = "P001")
            @PathVariable String productId,
            @Valid @RequestBody UpdateStockRequest request) {
        inventoryService.releaseStock(productId, request.quantity());
        return ResponseEntity.ok(ApiResponse.ok(null, "Reserved stock released successfully"));
    }
}
