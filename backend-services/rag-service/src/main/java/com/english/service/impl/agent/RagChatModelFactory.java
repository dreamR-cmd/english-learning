package com.english.service.impl.agent;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RagChatModelFactory {
    private final String apiKey;
    private final String modelName;
    private final String baseUrl;

    public RagChatModelFactory(
            @Value("${rag.chat.api-key:}") String apiKey,
            @Value("${rag.chat.model-name:gpt-4o-mini}") String modelName,
            @Value("${rag.chat.base-url:https://api.openai.com/v1}") String baseUrl) {
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.baseUrl = baseUrl;
    }

    public OpenAiChatModel chatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .build();
    }

    public OpenAiStreamingChatModel streamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .build();
    }
}
