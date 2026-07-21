package com.english.service.impl.agent;

import com.english.dto.RagAnswerResponse;
import com.english.service.RagService;
import com.english.service.impl.tools.KnowledgeBaseTool;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RagAgentService {
    private static final Logger log = LoggerFactory.getLogger(RagAgentService.class);

    private final RagIntentRouter intentRouter;
    private final KnowledgeAgent knowledgeAgent;
    private final LearningAgent learningAgent;
    private final WrongRecordAgent wrongRecordAgent;
    private final SafetyAgent safetyAgent;

    public RagAgentService(RagIntentRouter intentRouter,
                           KnowledgeAgent knowledgeAgent,
                           LearningAgent learningAgent,
                           WrongRecordAgent wrongRecordAgent,
                           SafetyAgent safetyAgent) {
        this.intentRouter = intentRouter;
        this.knowledgeAgent = knowledgeAgent;
        this.learningAgent = learningAgent;
        this.wrongRecordAgent = wrongRecordAgent;
        this.safetyAgent = safetyAgent;
    }

    public RagAnswerResponse ask(String question,
                                 Integer topK,
                                 String sessionId,
                                 Long userId,
                                 ChatMemoryProvider chatMemoryProvider,
                                 KnowledgeBaseTool knowledgeBaseTool) {
        AgentRequestContext context = new AgentRequestContext(question, topK, sessionId, userId, chatMemoryProvider, knowledgeBaseTool);
        AgentIntent intent = intentRouter.route(question);
        log.debug("RAG agent routed question to {}", intent);
        return selectAgent(intent).ask(context);
    }

    public void askStream(String question,
                          Integer topK,
                          String sessionId,
                          Long userId,
                          ChatMemoryProvider chatMemoryProvider,
                          KnowledgeBaseTool knowledgeBaseTool,
                          RagService.RagStreamHandler handler,
                          Runnable fallback) {
        AgentRequestContext context = new AgentRequestContext(question, topK, sessionId, userId, chatMemoryProvider, knowledgeBaseTool);
        AgentIntent intent = intentRouter.route(question);
        log.debug("RAG streaming agent routed question to {}", intent);
        selectAgent(intent).askStream(context, handler, fallback);
    }

    private SpecialistAgent selectAgent(AgentIntent intent) {
        return switch (intent) {
            case LEARNING_PLAN -> learningAgent;
            case WRONG_RECORD -> wrongRecordAgent;
            case USER_ACTION -> safetyAgent;
            case PRACTICE_GENERATE, WRITING_CORRECTION, GENERAL_CHAT, KNOWLEDGE_QA -> knowledgeAgent;
        };
    }
}
