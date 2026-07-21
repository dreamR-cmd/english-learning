package com.english.service.impl.agent;

import com.english.service.impl.tools.KnowledgeBaseTool;
import dev.langchain4j.memory.chat.ChatMemoryProvider;

public record AgentRequestContext(String question,
                                  Integer topK,
                                  String sessionId,
                                  Long userId,
                                  ChatMemoryProvider chatMemoryProvider,
                                  KnowledgeBaseTool knowledgeBaseTool) {
}
