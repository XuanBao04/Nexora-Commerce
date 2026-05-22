package com.nexoracommerce.redis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for stock status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockStatus {
    
    /**
     * Product ID
     */
    private String productId;
    
    /**
     * Current stock in Redis
     */
    private long redisStock;
    
    /**
     * Whether this product has an active lock
     */
    private boolean locked;
    
    /**
     * Last sync timestamp (optional)
     */
    private Long lastSyncTimestamp;
}
