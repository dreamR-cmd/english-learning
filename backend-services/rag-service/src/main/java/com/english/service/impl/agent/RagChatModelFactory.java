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
            @Value("${rag.chat.api-key:sk-312e8115d128601865a1cb57dbae20a41762d5484ef14f3d18dc048931a42f04}") String apiKey,
            @Value("${rag.chat.model-name:gpt-5.5}") String modelName,
            @Value("${rag.chat.base-url:https://sub2api.sxlx.tech/v1}") String baseUrl) {
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
