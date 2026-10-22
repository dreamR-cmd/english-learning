package com.english.service.impl.agent;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RagChatModelFactory {

    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;

    public RagChatModelFactory(
            @Value("${rag.chat.provider:ollama}") String provider,
            @Value("${rag.chat.api-key:}") String apiKey,
            @Value("${rag.chat.base-url:https://api.openai.com/v1}") String openaiBaseUrl,
            @Value("${rag.chat.model-name:qwen3.5:9b}") String modelName,
            @Value("${rag.chat.ollama.base-url:http://host.docker.internal:11434}") String ollamaBaseUrl,
            @Value("${rag.chat.timeout-seconds:120}") long timeoutSeconds) {

        if ("ollama".equalsIgnoreCase(provider)) {
            this.chatModel = OllamaChatModel.builder()
                    .baseUrl(ollamaBaseUrl)
                    .modelName(modelName)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();
            this.streamingChatModel = OllamaStreamingChatModel.builder()
                    .baseUrl(ollamaBaseUrl)
                    .modelName(modelName)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();
        } else {
            this.chatModel = OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .baseUrl(openaiBaseUrl)
                    .modelName(modelName)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();
            this.streamingChatModel = OpenAiStreamingChatModel.builder()
                    .apiKey(apiKey)
                    .baseUrl(openaiBaseUrl)
                    .modelName(modelName)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();
        }
    }

    public ChatModel chatModel() {
        return chatModel;
    }

    public StreamingChatModel streamingChatModel() {
        return streamingChatModel;
    }
}
