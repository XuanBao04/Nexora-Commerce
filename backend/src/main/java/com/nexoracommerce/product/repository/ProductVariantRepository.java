package com.nexoracommerce.product.repository;

import com.nexoracommerce.common.repository.BaseRepository;
import com.nexoracommerce.product.entity.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends BaseRepository<ProductVariant, String> {
    Optional<ProductVariant> findBySku(String sku);

    List<ProductVariant> findBySkuIn(Collection<String> skus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT pv 
        FROM ProductVariant pv 
        WHERE pv.sku = :sku
    """)
    Optional<ProductVariant> findBySkuWithLock(@Param("sku") String sku);

    @Modifying
    @Query("""
        UPDATE ProductVariant pv
        SET pv.quantity = pv.quantity - :qty,
            pv.soldQuantity = COALESCE(pv.soldQuantity, 0) + :qty,
            pv.reservedQuantity = COALESCE(pv.reservedQuantity, 0) - :qty
        WHERE pv.sku = :sku
          AND COALESCE(pv.reservedQuantity, 0) >= :qty
    """)
    int deductStockOnCompletion(@Param("sku") String sku, @Param("qty") int qty);

    @Modifying
    @Query("""
        UPDATE ProductVariant pv
        SET pv.reservedQuantity = COALESCE(pv.reservedQuantity, 0) + :qty
        WHERE pv.sku = :sku
          AND (pv.quantity - COALESCE(pv.reservedQuantity, 0)) >= :qty
    """)
    int reserveStock(@Param("sku") String sku, @Param("qty") int qty);

    @Modifying
    @Query("""
        UPDATE ProductVariant pv
        SET pv.reservedQuantity = COALESCE(pv.reservedQuantity, 0) - :qty
        WHERE pv.sku = :sku
          AND COALESCE(pv.reservedQuantity, 0) >= :qty
    """)
    int releaseStock(@Param("sku") String sku, @Param("qty") int qty);

    @Query(value = """
        -- 1. Full-Text Search (FTS) ranking using Postgres ts_rank
        WITH fts_search AS (
            SELECT 
                sku, 
                ts_rank(search_vector, websearch_to_tsquery('simple', :query)) AS rank,
                ROW_NUMBER() OVER (
                    ORDER BY ts_rank(search_vector, websearch_to_tsquery('simple', :query)) DESC
                ) AS fts_rank
            FROM product_variants
            WHERE search_vector @@ websearch_to_tsquery('simple', :query)
            ORDER BY rank DESC
            LIMIT :limit
        ),
        
        -- 2. Vector Search ranking using pgvector cosine similarity (<=> operator)
        vector_search AS (
            SELECT 
                sku,
                (1.0 - (embedding <=> CAST(:embedding AS vector))) AS similarity,
                ROW_NUMBER() OVER (
                    ORDER BY embedding <=> CAST(:embedding AS vector) ASC
                ) AS vec_rank
            FROM product_variants
            WHERE embedding IS NOT NULL
            ORDER BY similarity DESC
            LIMIT :limit
        )
        
        -- 3. Combine results using Reciprocal Rank Fusion (RRF) algorithm
        SELECT pv.*
        FROM product_variants pv
        INNER JOIN (
            SELECT 
                COALESCE(f.sku, v.sku) AS sku,
                COALESCE(1.0 / (60.0 + f.fts_rank), 0.0) + COALESCE(1.0 / (60.0 + v.vec_rank), 0.0) AS rrf_score
            FROM fts_search f
            FULL OUTER JOIN vector_search v ON f.sku = v.sku
        ) combined ON pv.sku = combined.sku
        ORDER BY combined.rrf_score DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<ProductVariant> hybridSearch(@Param("query") String query, 
                                      @Param("embedding") String embeddingString, 
                                      @Param("limit") int limit);

    @Query(value = """
        SELECT pv.*
        FROM product_variants pv
        WHERE pv.search_vector @@ websearch_to_tsquery('simple', :query)
        ORDER BY ts_rank(pv.search_vector, websearch_to_tsquery('simple', :query)) DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<ProductVariant> ftsOnlySearch(@Param("query") String query,
                                       @Param("limit") int limit);

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE product_variants 
        SET search_vector = to_tsvector('simple', :textChunk),
            embedding = CAST(:embedding AS vector)
        WHERE sku = :sku
    """, nativeQuery = true)
    void updateSearchData(@Param("sku") String sku, 
                          @Param("textChunk") String textChunk, 
                          @Param("embedding") String embeddingString);

    @Query("""
        SELECT DISTINCT pv 
        FROM ProductVariant pv 
        JOIN FETCH pv.product p 
        LEFT JOIN FETCH p.brand 
        LEFT JOIN FETCH p.category 
        LEFT JOIN FETCH pv.attributeValues av 
        LEFT JOIN FETCH av.attribute
    """)
    List<ProductVariant> findAllWithAssociations();
}

