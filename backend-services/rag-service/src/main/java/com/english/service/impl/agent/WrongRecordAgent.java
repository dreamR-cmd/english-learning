package com.english.service.impl.agent;

import com.english.dto.RagAnswerResponse;
import com.english.service.RagService;
import com.english.service.impl.RagInternalApiClient;
import com.english.service.impl.tools.WrongRecordTools;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WrongRecordAgent implements SpecialistAgent {
    private static final String SYSTEM_MESSAGE = """
            你是错题分析 Agent，负责查询错题、归纳错因、总结薄弱点和给出复习建议。
            规则：
            1. 处理错题相关问题时先调用 getWrongRecords。
            2. 不要原样展示 JSON，整理成清晰的中文列表或总结。
            3. 如果用户要求“根据错题出题”，可以先基于错题内容生成练习建议，但不要写入数据库。
            4. 如果没有错题记录，直接说明当前没有查到错题。
            """;

    private final SpecialistAgentRunner runner;
    private final RagInternalApiClient internalApiClient;

    public WrongRecordAgent(SpecialistAgentRunner runner, RagInternalApiClient internalApiClient) {
        this.runner = runner;
        this.internalApiClient = internalApiClient;
    }

    @Override
    public RagAnswerResponse ask(AgentRequestContext context) {
        WrongRecordTools tools = new WrongRecordTools(context.userId(), internalApiClient);
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
        WrongRecordTools tools = new WrongRecordTools(context.userId(), internalApiClient);
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
        return "用户问题：\n" + (question == null ? "" : question.trim()) + "\n\n请先查询错题记录，再回答。";
    }
}
