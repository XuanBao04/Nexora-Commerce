package com.nexoracommerce.ai.controller;

import com.nexoracommerce.ai.service.AiSearchService;
import com.nexoracommerce.ai.service.EmbeddingGeneratorJob;
import com.nexoracommerce.common.response.ApiResponse;
import com.nexoracommerce.product.dto.response.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "AI Search Module", description = "Endpoints for hybrid AI search and vector embeddings")
public class AiSearchController {

    private final AiSearchService aiSearchService;
    private final EmbeddingGeneratorJob embeddingGeneratorJob;

    @Operation(summary = "Search products", description = "Perform a hybrid semantic search using AI vector embeddings and Full-Text Search with RRF ranking. Returns full product details.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> search(@RequestParam String q,
                                                                      @RequestParam(defaultValue = "10") int limit) {
        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.failure("Search query must not be empty", 400)
            );
        }
        // Hard limit protection against excessive result sets
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        List<ProductResponse> results = aiSearchService.hybridSearch(q.trim(), safeLimit);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @Operation(summary = "Sync embeddings", description = "Run a background job to generate vector embeddings and TSVector data for all existing product variants.")
    @PostMapping("/sync-embeddings")
    public ResponseEntity<String> syncEmbeddings() {
        // Run asynchronously
        embeddingGeneratorJob.generateEmbeddingsForAllVariants();
        return ResponseEntity.accepted().body("Started generating embeddings in the background.");
    }
}
