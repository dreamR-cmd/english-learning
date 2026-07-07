package com.english;

import dev.langchain4j.model.openai.OpenAiChatModel;

public class Test {
    public static void main(String[] args) {
        String apiKey = System.getenv("OPENAI_API_KEY");
        String baseUrl = System.getenv().getOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1");

        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName("gpt-5.5")
                .build();

        System.out.println(model.chat("Say 'Hello World'"));
    }
}
