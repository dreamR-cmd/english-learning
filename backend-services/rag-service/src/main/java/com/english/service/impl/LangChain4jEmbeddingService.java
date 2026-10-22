package com.english.service.impl;

import com.english.service.RagEmbeddingService;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class LangChain4jEmbeddingService implements RagEmbeddingService {

    private final EmbeddingModel embeddingModel;

    public LangChain4jEmbeddingService(
            @Value("${rag.embedding.provider:ollama}") String provider,
            @Value("${rag.embedding.api-key:}") String apiKey,
            @Value("${rag.embedding.base-url:}") String openaiBaseUrl,
            @Value("${rag.embedding.model-name:bge-m3}") String modelName,
            @Value("${rag.embedding.ollama.base-url:http://ollama:11434}") String ollamaBaseUrl,
            @Value("${rag.vector.dimension:1024}") Integer dimensions,
            @Value("${rag.embedding.timeout-seconds:60}") long timeoutSeconds) {

        if ("ollama".equalsIgnoreCase(provider)) {
            this.embeddingModel = OllamaEmbeddingModel.builder()
                    .baseUrl(ollamaBaseUrl)
                    .modelName(modelName)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();
        } else {
            this.embeddingModel = OpenAiEmbeddingModel.builder()
                    .apiKey(apiKey)
                    .baseUrl(openaiBaseUrl)
                    .modelName(modelName)
                    .dimensions(dimensions)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();
        }
    }

    @Override
    public float[] embed(String text) {
        return embeddingModel.embed(text).content().vector();
    }
}
