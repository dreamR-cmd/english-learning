package com.english;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.ai.chat.model.StreamingChatModel;

public class Test  {
    public static void main(String[] args) {
        System.setProperty("https.proxyHost", "127.0.0.1");
        System.setProperty("https.proxyPort", "10808");
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "10808");
        String apiKey = "sk-fb839071a7ebe9320af714f68ba73ecb9e63c2823fe37541e1658d79440fc9fe";

        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://reecewong520--sub2api.modal.run/v1")
                .apiKey(apiKey)
                .modelName("gpt-5.5")
                .build();
        StreamingChatModel model1 = OpenAiStreamingChatModel.builder()
               .baseUrl("https://reecewong520--sub2api.modal.run/v1")
                .apiKey(apiKey)
                .modelName("gpt-5.5")
                .build();
        // String answer = model.chat("Say 'Hello World'");
        String userMessage = "Tell me a joke";
        // System.out.println(answer);

        model1.chat(userMessage, new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
                System.out.println("onPartialResponse: " + partialResponse);
            }

            @Override
            public void onPartialThinking(PartialThinking partialThinking) {
                System.out.println("onPartialThinking: " + partialThinking);
            }

            @Override
            public void onPartialToolCall(PartialToolCall partialToolCall) {
                System.out.println("onPartialToolCall: " + partialToolCall);
            }

            @Override
            public void onCompleteToolCall(CompleteToolCall completeToolCall) {
                System.out.println("onCompleteToolCall: " + completeToolCall);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                System.out.println("onCompleteResponse: " + completeResponse);
            }

            @Override
            public void onError(Throwable error) {
                error.printStackTrace();
            }
        });
    }
}