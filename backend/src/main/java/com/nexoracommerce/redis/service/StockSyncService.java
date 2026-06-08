package com.nexoracommerce.redis.service;

public interface IStockSyncService {
    void syncProductStock(String productId);

    void syncAllStock();
}
