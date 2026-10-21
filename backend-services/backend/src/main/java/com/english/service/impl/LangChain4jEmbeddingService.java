package com.english.service.impl;

import com.english.service.RagEmbeddingService;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class LangChain4jEmbeddingService implements RagEmbeddingService {
    private final String apiKey;
    private final String baseUrl;
    private final String modelName;
    private final Integer dimensions;
    private final long timeoutSeconds;
    private volatile OpenAiEmbeddingModel embeddingModel;

    public LangChain4jEmbeddingService(
            @Value("${rag.embedding.api-key:}") String apiKey,
            @Value("${rag.embedding.base-url:}") String baseUrl,
            @Value("${rag.embedding.model-name:}") String modelName,
            @Value("${rag.vector.dimension:1536}") Integer dimensions,
            @Value("${rag.embedding.timeout-seconds:30}") long timeoutSeconds) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.dimensions = dimensions;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public float[] embed(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured; cannot generate RAG embedding");
        }
        return model().embed(text).content().vector();
    }

    private OpenAiEmbeddingModel model() {
        OpenAiEmbeddingModel current = embeddingModel;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (embeddingModel == null) {
                embeddingModel = OpenAiEmbeddingModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .dimensions(dimensions)
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .build();
            }
            return embeddingModel;
        }
    }
}
