package com.english.service.impl.agent;

import com.english.dto.RagAnswerResponse;
import com.english.service.RagService;
import com.english.service.impl.tools.KnowledgeTools;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KnowledgeAgent implements SpecialistAgent {
    private static final String SYSTEM_MESSAGE = """
            你是英语学习知识库 Agent，负责单词、语法、作文、文档资料和承接追问。
            规则：
            1. 回答前先调用 searchKnowledgeBase 检索知识库。
            2. 只根据工具返回的知识库资料回答，不要编造资料外事实。
            3. 不要展示资料标题、来源或原始片段。
            4. 如果没有资料，直接说明知识库没有查到相关内容。
            5. 用中文解释，必要时给英文例句。
            """;

    private final SpecialistAgentRunner runner;

    public KnowledgeAgent(SpecialistAgentRunner runner) {
        this.runner = runner;
    }

    @Override
    public RagAnswerResponse ask(AgentRequestContext context) {
        KnowledgeTools tools = new KnowledgeTools(context.sessionId(), context.topK(), context.knowledgeBaseTool());
        return runner.ask(
                SYSTEM_MESSAGE,
                buildUserMessage(context.question()),
                context.sessionId(),
                context.chatMemoryProvider(),
                List.of(tools),
                tools::references
        );
    }

    @Override
    public void askStream(AgentRequestContext context, RagService.RagStreamHandler handler, Runnable fallback) {
        KnowledgeTools tools = new KnowledgeTools(context.sessionId(), context.topK(), context.knowledgeBaseTool());
        runner.askStream(
                SYSTEM_MESSAGE,
                buildUserMessage(context.question()),
                context.sessionId(),
                context.chatMemoryProvider(),
                List.of(tools),
                tools::references,
                handler,
                fallback
        );
    }

    private String buildUserMessage(String question) {
        return "用户问题：\n" + (question == null ? "" : question.trim()) + "\n\n请先检索知识库，再回答。";
    }
}
