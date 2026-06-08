package com.nexoracommerce.redis.service;

public interface RedisStockService {
    long getStock(String productId);

    void setStock(String productId, long quantity);

    boolean decrementStock(String productId, long quantity);

    void incrementStock(String productId, long quantity);

    boolean decrementIfAvailable(String productId, long quantity);

    String acquireLock(String productId);

    void releaseLock(String productId, String lockToken);

    boolean isLocked(String productId);

    void deleteStock(String productId);

    void clearAllStock();
}
