package com.nexoracommerce.redis.service;

public interface StockSyncService {
    void syncProductStock(String productId);

    void syncAllStock();
}
