package com.english.service.impl.agent;

import com.english.dto.RagAnswerResponse;
import com.english.service.RagService;
import com.english.service.impl.RagInternalApiClient;
import com.english.service.impl.tools.LearningTools;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LearningAgent implements SpecialistAgent {
    private static final String SYSTEM_MESSAGE = """
            你是英语学习计划 Agent，负责今日单词、复习词、模块单词和学习进度建议。
            规则：
            1. 查询今日任务时调用 getTodayWords。
            2. 查询复习内容时调用 getReviewWords。
            3. 查询 CET4、CET6、考研、雅思、托福、GRE 等模块单词时调用 getModuleWords。
            4. 工具返回的是内部数据，请整理成适合学习者阅读的中文回答，不要原样展示 JSON。
            5. 不要编造用户学习数据。
            """;

    private final SpecialistAgentRunner runner;
    private final RagInternalApiClient internalApiClient;

    public LearningAgent(SpecialistAgentRunner runner, RagInternalApiClient internalApiClient) {
        this.runner = runner;
        this.internalApiClient = internalApiClient;
    }

    @Override
    public RagAnswerResponse ask(AgentRequestContext context) {
        LearningTools tools = new LearningTools(context.userId(), internalApiClient);
        return runner.ask(
                SYSTEM_MESSAGE,
                buildUserMessage(context.question()),
                context.sessionId(),
                context.chatMemoryProvider(),
                List.of(tools),
                () -> List.of()
        );
    }

    @Override
    public void askStream(AgentRequestContext context, RagService.RagStreamHandler handler, Runnable fallback) {
        LearningTools tools = new LearningTools(context.userId(), internalApiClient);
        runner.askStream(
                SYSTEM_MESSAGE,
                buildUserMessage(context.question()),
                context.sessionId(),
                context.chatMemoryProvider(),
                List.of(tools),
                () -> List.of(),
                handler,
                fallback
        );
    }

    private String buildUserMessage(String question) {
        return "用户问题：\n" + (question == null ? "" : question.trim()) + "\n\n请判断需要查询哪类学习数据，并调用对应工具。";
    }
}
