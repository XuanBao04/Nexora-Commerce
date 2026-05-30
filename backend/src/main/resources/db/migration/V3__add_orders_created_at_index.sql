-- V3: Add physical index on orders(created_at) column to optimize date-range queries
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);
