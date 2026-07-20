package com.english.service.impl.tools;

@FunctionalInterface
public interface KnowledgeBaseTool {
    KnowledgeBaseToolResult search(String question, Integer topK, String sessionId);
}
