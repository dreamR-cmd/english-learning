package com.english.service.impl.agent;

import com.english.dto.RagAnswerResponse;
import com.english.service.RagService;
import com.english.service.impl.RagInternalApiClient;
import com.english.service.impl.tools.KnowledgeBaseTool;
import com.english.service.impl.tools.RagAgentTools;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class RagAgentService {
    private static final Logger log = LoggerFactory.getLogger(RagAgentService.class);
    private static final String AGENT_SYSTEM_MESSAGE = """
            你是英语学习平台内的 AI 学习助手，可以根据用户意图调用后端工具获取平台数据。
            工具规则：
            1. 解释单词、语法、作文、上传文档或知识库资料时，先调用 searchKnowledgeBase。
            2. 查询今日单词、复习单词、错题记录、模块单词时，调用对应的只读工具。
            3. 工具返回的是内部数据，只能提炼总结，不要原样展示 JSON、标题、来源或资料片段。
            4. 不要编造平台数据；工具没有查到时，直接说明没有查到。
            5. 不要执行删除、修改、购买、提交答案等写操作；遇到这类需求，只说明需要用户确认后在页面操作。
            6. 回答以中文为主，必要时给英文例句，尽量控制在 300 字以内。
            """;

    private final RagInternalApiClient internalApiClient;
    private final RagChatModelFactory chatModelFactory;

    public RagAgentService(RagInternalApiClient internalApiClient,
                           RagChatModelFactory chatModelFactory) {
        this.internalApiClient = internalApiClient;
        this.chatModelFactory = chatModelFactory;
    }

    public RagAnswerResponse ask(String question,
                                 Integer topK,
                                 String sessionId,
                                 Long userId,
                                 ChatMemoryProvider chatMemoryProvider,
                                 KnowledgeBaseTool knowledgeBaseTool) {
        RagAgentTools tools = createTools(userId, sessionId, topK, knowledgeBaseTool);
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModelFactory.chatModel())
                .chatMemoryProvider(chatMemoryProvider)
                .systemMessage(AGENT_SYSTEM_MESSAGE)
                .tools(tools)
                .maxToolCallingRoundTrips(4)
                .maxSequentialToolsInvocations(6)
                .compensateOnToolErrors(true)
                .build();
        String answer = assistant.chat(sessionId, buildAgentUserMessage(question));
        return new RagAnswerResponse(answer, tools.references());
    }

    public void askStream(String question,
                          Integer topK,
                          String sessionId,
                          Long userId,
                          ChatMemoryProvider chatMemoryProvider,
                          KnowledgeBaseTool knowledgeBaseTool,
                          RagService.RagStreamHandler handler,
                          Runnable fallback) {
        RagAgentTools tools = createTools(userId, sessionId, topK, knowledgeBaseTool);
        AtomicBoolean emittedToken = new AtomicBoolean(false);
        StreamingAssistant assistant = AiServices.builder(StreamingAssistant.class)
                .streamingChatModel(chatModelFactory.streamingChatModel())
                .chatMemoryProvider(chatMemoryProvider)
                .systemMessage(AGENT_SYSTEM_MESSAGE)
                .tools(tools)
                .maxToolCallingRoundTrips(4)
                .maxSequentialToolsInvocations(6)
                .compensateOnToolErrors(true)
                .build();

        assistant.chat(sessionId, buildAgentUserMessage(question))
                .onPartialResponse(token -> {
                    emittedToken.set(true);
                    handler.onToken(token);
                })
                .onToolExecuted(toolExecution -> handler.onReferences(tools.references()))
                .onCompleteResponse(response -> {
                    handler.onReferences(tools.references());
                    handler.onComplete();
                })
                .onError(error -> {
                    if (emittedToken.get()) {
                        handler.onError(error);
                        return;
                    }
                    log.warn("RAG streaming agent failed, falling back to retrieved-context stream.", error);
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

    private RagAgentTools createTools(Long userId, String sessionId, Integer topK, KnowledgeBaseTool knowledgeBaseTool) {
        return new RagAgentTools(userId, sessionId, topK, internalApiClient, knowledgeBaseTool);
    }

    private String buildAgentUserMessage(String question) {
        return "用户问题：\n" + (question == null ? "" : question.trim()) + "\n\n请判断用户意图，必要时先调用工具获取平台数据，再给出回答。";
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
