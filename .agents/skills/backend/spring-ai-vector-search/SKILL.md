---
name: spring-ai-vector-search
description: Spring AI integration for Phase 4 - RAG chatbot, semantic search, embeddings with pgvector, and Gemini LLM.
---

## Scope & Activation Rules

Activate when:
- Implementing AI chatbot with context from product database
- Creating semantic search using embeddings + pgvector
- Generating or analyzing product content with Gemini
- Building RAG (Retrieval-Augmented Generation) features

## System Directives

### Spring AI Setup
- **Use Spring AI ChatClient**: Single abstraction for LLM calls (Gemini/OpenAI)
- **Vector Storage in pgvector**: Store product embeddings in `product_variants.embedding` (768-dim)
- **Cache Embeddings**: Generate once, store permanently; no regeneration on each search
- **Streaming Responses**: Use `stream()` for chat to show real-time responses to users
- **Temperature Control**: Lower (0.3) for deterministic tasks, higher (0.8) for creative content
- **Token Limits**: Set max output tokens to prevent runaway responses
- **System Prompts**: Always define context and guidelines for AI behavior

### Anti-Patterns
- No regenerating embeddings on every request (performance killer)
- No storing full LLM responses in logs without truncation
- No prompts without context (always provide product/domain info)
- No skipping rate limiting on Gemini API

## Configuration

### Dependencies (pom.xml)
```xml
<!-- Spring AI for Gemini -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-google-ai-gemini-spring-boot-starter</artifactId>
    <version>1.0.0-M1</version>
</dependency>

<!-- pgvector support -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-pgvector-store-spring-boot-starter</artifactId>
</dependency>
```

### Config
```java
@Configuration
@EnableCaching
public class AiConfig {
    
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.defaultSystem("""
            Bạn là trợ lý mua sắm trực tuyến của Nexora Commerce. 
            Giúp khách hàng tìm sản phẩm phù hợp dựa trên nhu cầu của họ.
            Luôn trả lời bằng tiếng Việt. Ngắn gọn và hữu ích.
            """)
            .build();
    }
    
    @Bean
    public EmbeddingClient embeddingClient(GoogleAiEmbeddingClient client) {
        return new ResilienceEmbeddingClientWrapper(client);
    }
    
    @Bean
    public VectorStore vectorStore(JdbcOperationsVectorStoreImpl store) {
        return store;  // Auto-configured by Spring AI with pgvector
    }
}
```

### Application Configuration
```yaml
spring:
  ai:
    google:
      ai:
        gemini:
          api-key: ${GEMINI_API_KEY}
          chat:
            model: gemini-pro
            temperature: 0.7
            max-output-tokens: 500
    vectorstore:
      pgvector:
        index-type: hnsw
        distance-type: cosine_distance
```

## Phase 4.1: RAG Chatbot with Product Context

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {
    
    private final ChatClient chatClient;
    private final AiChatMessageRepository chatRepository;
    private final ProductVariantRepository productRepository;
    private final CacheManager cacheManager;
    
    public ChatResponse chat(String message, String sessionId, String userId) {
        // Load chat history (last 10 messages for context)
        List<AiChatMessage> history = chatRepository
            .findBySessionIdOrderByCreatedAtDesc(sessionId)
            .stream()
            .sorted(Comparator.comparing(AiChatMessage::getCreatedAt))
            .limit(10)
            .toList();
        
        // Build context string from history
        String context = history.stream()
            .map(m -> (m.isUserMessage() ? "User: " : "Assistant: ") + m.getContent())
            .collect(Collectors.joining("\n"));
        
        // Search similar products based on user query
        List<ProductVariant> relatedProducts = findRelatedProducts(message, 5);
        
        // Build RAG prompt with product context
        String ragPrompt = buildRagPrompt(message, context, relatedProducts);
        
        // Get response from Gemini
        String reply = chatClient
            .prompt(ragPrompt)
            .call()
            .getResult()
            .getOutput()
            .getContent();
        
        // Save chat messages
        AiChatMessage userMsg = AiChatMessage.builder()
            .sessionId(sessionId)
            .userId(userId)
            .content(message)
            .isUserMessage(true)
            .createdAt(LocalDateTime.now())
            .build();
        
        AiChatMessage assistantMsg = AiChatMessage.builder()
            .sessionId(sessionId)
            .userId(userId)
            .content(reply)
            .isUserMessage(false)
            .createdAt(LocalDateTime.now())
            .build();
        
        chatRepository.saveAll(List.of(userMsg, assistantMsg));
        log.info("Chat saved: session={}, user={}", sessionId, userId);
        
        // Extract product suggestions from reply (parse JSON block if present)
        List<ProductVariantDto> suggestedProducts = relatedProducts.stream()
            .map(v -> new ProductVariantDto(v.getId(), v.getProduct().getName(), v.getPrice()))
            .toList();
        
        return new ChatResponse(reply, sessionId, suggestedProducts);
    }
    
    private List<ProductVariant> findRelatedProducts(String query, int limit) {
        try {
            // Embed user query using Gemini embeddings
            // Search pgvector for similar product embeddings
            // This would use VectorStore.search() with similarity threshold
            return productRepository.findBySemanticSimilarity(query, limit);
        } catch (Exception e) {
            log.warn("Failed to find related products: {}", e.getMessage());
            return List.of();
        }
    }
    
    private String buildRagPrompt(String userMessage, String history, List<ProductVariant> products) {
        String productInfo = products.stream()
            .map(p -> String.format(
                "- %s (%.0f₫): %s",
                p.getProduct().getName(),
                p.getPrice(),
                p.getProduct().getDescription()
            ))
            .collect(Collectors.joining("\n"));
        
        return String.format("""
            Lịch sử trò chuyện:
            %s
            
            Sản phẩm liên quan có thể gợi ý:
            %s
            
            Câu hỏi của khách hàng: %s
            
            Hãy trả lời hữu ích, gợi ý sản phẩm nếu phù hợp.
            """, history, productInfo, userMessage);
    }
}

public record ChatResponse(
    String reply,
    String sessionId,
    List<ProductVariantDto> suggestedProducts
) {}

public record ProductVariantDto(
    String id,
    String name,
    BigDecimal price
) {}

@Entity
@Table(name = "ai_chat_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String sessionId;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(nullable = false)
    private Boolean isUserMessage;
    
    @CreatedDate
    private LocalDateTime createdAt;
}
```

## Phase 4.2 & 4.3: Semantic Search + Embeddings

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class SemanticSearchService {
    
    private final EmbeddingClient embeddingClient;
    private final ProductVariantRepository productRepository;
    private final CacheManager cacheManager;
    
    // Generate and cache embedding for product variant
    public void generateEmbedding(ProductVariant variant) {
        try {
            String text = variant.getProduct().getName() + " " + 
                         variant.getProduct().getDescription();
            
            List<Double> embedding = embeddingClient.embed(text);
            
            // Convert to pgvector format: "[0.1, 0.2, ...]"
            String vectorStr = "[" + embedding.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", ")) + "]";
            
            variant.setEmbedding(vectorStr);
            productRepository.save(variant);
            
            log.info("Embedding generated for variant: {}", variant.getId());
        } catch (Exception e) {
            log.error("Failed to generate embedding", e);
        }
    }
    
    // Search by semantic similarity
    @Cacheable(value = "semanticSearch", key = "#query")
    public List<ProductVariantResponse> search(String query, int limit) {
        try {
            List<Double> queryVector = embeddingClient.embed(query);
            
            // pgvector similarity search
            List<ProductVariant> results = productRepository
                .findBySemanticSimilarity(queryVector, limit);
            
            return results.stream()
                .map(v -> new ProductVariantResponse(v.getId(), v.getProduct().getName(), v.getPrice()))
                .toList();
        } catch (Exception e) {
            log.warn("Semantic search failed, falling back to keyword search");
            return productRepository.findByNameContaining(query)
                .stream()
                .limit(limit)
                .map(v -> new ProductVariantResponse(v.getId(), v.getProduct().getName(), v.getPrice()))
                .toList();
        }
    }
}

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, String> {
    
    @Query(value = """
        SELECT * FROM product_variants 
        WHERE embedding <-> ?1 < 0.5
        ORDER BY embedding <-> ?1
        LIMIT ?2
        """, nativeQuery = true)
    List<ProductVariant> findBySemanticSimilarity(List<Double> embedding, int limit);
    
    @Query("SELECT v FROM ProductVariant v WHERE v.product.name LIKE %?1%")
    List<ProductVariant> findByNameContaining(String name);
}
```

## Phase 4.4 & 4.5: Content Generation

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentGenerationService {
    
    private final ChatClient chatClient;
    
    public ProductContentResponse generateContent(ProductContentRequest request) {
        String prompt = String.format("""
            Hãy tạo nội dung marketing cho sản phẩm:
            - Tên: %s
            - Danh mục: %s
            - Đặc tính: %s
            - Khách hàng mục tiêu: %s
            - Từ khóa SEO: %s
            
            Trả về JSON với các field:
            {
              "title": "Tiêu đề sản phẩm (50 ký tự)",
              "description": "Mô tả chi tiết (300 ký tự)",
              "seoMetaDescription": "SEO description (160 ký tự)",
              "bulletPoints": ["Điểm nổi bật 1", "Điểm nổi bật 2", ...]
            }
            """,
            request.productName(),
            request.category(),
            String.join(", ", request.specs()),
            request.targetAudience(),
            String.join(", ", request.keywords())
        );
        
        String response = chatClient
            .prompt(prompt)
            .call()
            .getResult()
            .getOutput()
            .getContent();
        
        // Parse JSON from response
        return parseContentResponse(response);
    }
    
    private ProductContentResponse parseContentResponse(String jsonStr) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonStr, ProductContentResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse content response", e);
            throw new RuntimeException("Content generation failed");
        }
    }
}

public record ProductContentRequest(
    String productName,
    String category,
    List<String> specs,
    String targetAudience,
    List<String> keywords
) {}

public record ProductContentResponse(
    String title,
    String description,
    String seoMetaDescription,
    List<String> bulletPoints
) {}
```

## Controller

```java
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {
    
    private final AiChatService chatService;
    private final SemanticSearchService searchService;
    private final ContentGenerationService contentService;
    
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
        @RequestBody ChatRequest request,
        @AuthenticationPrincipal String userId) {
        
        ChatResponse response = chatService.chat(request.message(), request.sessionId(), userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<ProductVariantResponse>> search(
        @RequestParam String q,
        @RequestParam(defaultValue = "10") int limit) {
        
        List<ProductVariantResponse> results = searchService.search(q, limit);
        return ResponseEntity.ok(results);
    }
    
    @PostMapping("/admin/generate-content")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductContentResponse> generateContent(
        @RequestBody ProductContentRequest request) {
        
        ProductContentResponse content = contentService.generateContent(request);
        return ResponseEntity.ok(content);
    }
}

public record ChatRequest(String message, String sessionId) {}
```

## Verification Commands

```bash
# Test chat endpoint
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message":"Tìm laptop gaming dưới 20 triệu","sessionId":"sess-123"}'

# Test semantic search
curl -X GET "http://localhost:8080/api/ai/search?q=laptop%20mỏng%20nhẹ"

# Check embeddings saved in DB
psql nexora_db -c "SELECT id, embedding FROM product_variants LIMIT 1;"
```
