package com.nexoracommerce.ai.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResultMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class GeminiEmbeddingConfig {

    @Value("${spring.ai.vertex.ai.gemini.api-key:}")
    private String apiKey;

    @Bean
    public EmbeddingModel geminiEmbeddingModel() {
        return new EmbeddingModel() {
            private final RestTemplate restTemplate = new RestTemplate();

            @Override
            public List<Double> embed(Document document) {
                return embed(document.getContent());
            }

            @Override
            public List<Double> embed(String text) {
                if (apiKey == null || apiKey.isEmpty()) {
                    throw new IllegalStateException("Gemini API Key is not configured. Please set spring.ai.vertex.ai.gemini.api-key");
                }
                
                String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key=" + apiKey;
                Map<String, Object> request = Map.of(
                    "model", "models/gemini-embedding-001",
                    "content", Map.of("parts", List.of(Map.of("text", text))),
                    "outputDimensionality", 768
                );
                
                try {
                    Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
                    Map<String, Object> embeddingNode = (Map<String, Object>) response.get("embedding");
                    return (List<Double>) embeddingNode.get("values");
                } catch (Exception e) {
                    String errorMsg = "Failed to generate embedding from Gemini API";
                    if (e instanceof org.springframework.web.client.HttpStatusCodeException) {
                        org.springframework.web.client.HttpStatusCodeException httpException = (org.springframework.web.client.HttpStatusCodeException) e;
                        errorMsg += " - Response: " + httpException.getResponseBodyAsString();
                    }
                    throw new RuntimeException(errorMsg, e);
                }
            }

            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                List<Embedding> embeddings = request.getInstructions().stream()
                        .map(text -> new Embedding(embed(text), 0))
                        .collect(Collectors.toList());
                return new EmbeddingResponse(embeddings);
            }
            
            @Override
            public int dimensions() {
                return 768;
            }
        };
    }
}
