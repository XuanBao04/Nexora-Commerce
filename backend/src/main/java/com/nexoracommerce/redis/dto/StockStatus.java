package com.nexoracommerce.redis.dto;

import lombok.Builder;

/**
 * Response DTO for stock status
 */
@Builder
public record StockStatus(
    /**
     * Product ID
     */
    String productId,
    
    /**
     * Current stock in Redis
     */
    long redisStock,
    
    /**
     * Whether this product has an active lock
     */
    boolean locked,
    
    /**
     * Last sync timestamp (optional)
     */
    Long lastSyncTimestamp
) {}
