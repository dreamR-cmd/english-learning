package com.english.service.impl.agent;

import com.english.dto.RagAnswerResponse;
import com.english.dto.RagSearchItem;
import com.english.service.RagService;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class SpecialistAgentRunner {
    private static final Logger log = LoggerFactory.getLogger(SpecialistAgentRunner.class);

    private final RagChatModelFactory chatModelFactory;

    public SpecialistAgentRunner(RagChatModelFactory chatModelFactory) {
        this.chatModelFactory = chatModelFactory;
    }

    public RagAnswerResponse ask(String systemMessage,
                                 String userMessage,
                                 String sessionId,
                                 ChatMemoryProvider chatMemoryProvider,
                                 List<Object> tools,
                                 Supplier<List<RagSearchItem>> referencesSupplier) {
        AiServices<Assistant> builder = AiServices.builder(Assistant.class)
                .chatModel(chatModelFactory.chatModel())
                .chatMemoryProvider(chatMemoryProvider)
                .systemMessage(systemMessage)
                .maxToolCallingRoundTrips(4)
                .maxSequentialToolsInvocations(6)
                .compensateOnToolErrors(true);
        if (tools != null && !tools.isEmpty()) {
            builder.tools(tools);
        }

        Assistant assistant = builder.build();
        String answer = assistant.chat(sessionId, userMessage);
        return new RagAnswerResponse(answer, safeReferences(referencesSupplier));
    }

    public void askStream(String systemMessage,
                          String userMessage,
                          String sessionId,
                          ChatMemoryProvider chatMemoryProvider,
                          List<Object> tools,
                          Supplier<List<RagSearchItem>> referencesSupplier,
                          RagService.RagStreamHandler handler,
                          Runnable fallback) {
        AtomicBoolean emittedToken = new AtomicBoolean(false);
        AiServices<StreamingAssistant> builder = AiServices.builder(StreamingAssistant.class)
                .streamingChatModel(chatModelFactory.streamingChatModel())
                .chatMemoryProvider(chatMemoryProvider)
                .systemMessage(systemMessage)
                .maxToolCallingRoundTrips(4)
                .maxSequentialToolsInvocations(6)
                .compensateOnToolErrors(true);
        if (tools != null && !tools.isEmpty()) {
            builder.tools(tools);
        }

        StreamingAssistant assistant = builder.build();
        assistant.chat(sessionId, userMessage)
                .onPartialResponse(token -> {
                    emittedToken.set(true);
                    handler.onToken(token);
                })
                .onToolExecuted(toolExecution -> handler.onReferences(safeReferences(referencesSupplier)))
                .onCompleteResponse(response -> {
                    handler.onReferences(safeReferences(referencesSupplier));
                    handler.onComplete();
                })
                .onError(error -> {
                    if (emittedToken.get()) {
                        handler.onError(error);
                        return;
                    }
                    log.warn("Specialist agent failed before streaming tokens, falling back.", error);
                    runFallback(fallback, handler, error);
                })
                .start();
    }

    public interface Assistant {
        String chat(@MemoryId String sessionId, @UserMessage String message);
    }

    public interface StreamingAssistant {
        TokenStream chat(@MemoryId String sessionId, @UserMessage String message);
    }

    private List<RagSearchItem> safeReferences(Supplier<List<RagSearchItem>> referencesSupplier) {
        if (referencesSupplier == null) {
            return List.of();
        }
        List<RagSearchItem> references = referencesSupplier.get();
        return references == null ? List.of() : references;
    }

    private void runFallback(Runnable fallback, RagService.RagStreamHandler handler, Throwable originalError) {
        if (fallback == null) {
            handler.onError(originalError);
            return;
        }
        try {
            fallback.run();
        } catch (Throwable fallbackError) {
            handler.onError(fallbackError);
        }
    }
}
