-- V4__add_vector_index_and_fts.sql

-- 1. Ensure vector extension is created
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Configure maintenance_work_mem to be large enough for HNSW index creation
-- Note: Setting it locally for the current session/transaction
SET maintenance_work_mem = '512MB';

-- 3. Create HNSW index for the embedding column in product_variants
-- using vector_cosine_ops since we will search by cosine similarity
CREATE INDEX IF NOT EXISTS idx_variants_embedding ON product_variants USING hnsw (embedding vector_cosine_ops);

-- 4. Add search_vector column for Full-Text Search
ALTER TABLE product_variants ADD COLUMN IF NOT EXISTS search_vector tsvector;

-- 5. Create GIN index for Full-Text Search
CREATE INDEX IF NOT EXISTS idx_variants_search_vector ON product_variants USING gin(search_vector);
