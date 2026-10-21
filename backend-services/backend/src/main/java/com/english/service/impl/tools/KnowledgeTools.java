package com.english.service.impl.tools;

import com.english.dto.RagSearchItem;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.ArrayList;
import java.util.List;

public final class KnowledgeTools {
    private final String sessionId;
    private final Integer topK;
    private final KnowledgeBaseTool knowledgeBaseTool;
    private final List<RagSearchItem> references = new ArrayList<>();

    public KnowledgeTools(String sessionId, Integer topK, KnowledgeBaseTool knowledgeBaseTool) {
        this.sessionId = sessionId;
        this.topK = topK;
        this.knowledgeBaseTool = knowledgeBaseTool;
    }

    @Tool("检索 RAG 知识库。适合解释单词、语法、英语学习资料、上传文档内容、以及承接上一轮的追问。")
    public String searchKnowledgeBase(@P("用户要检索或追问的问题") String question) {
        String query = question == null || question.isBlank() ? "" : question.trim();
        if (query.isBlank()) {
            return "问题不能为空，无法检索知识库。";
        }
        try {
            KnowledgeBaseToolResult result = knowledgeBaseTool.search(query, topK, sessionId);
            updateReferences(result.references());
            return result.content();
        } catch (RuntimeException error) {
            updateReferences(List.of());
            return "知识库检索失败：" + safeMessage(error);
        }
    }

    public synchronized List<RagSearchItem> references() {
        return List.copyOf(references);
    }

    private synchronized void updateReferences(List<RagSearchItem> nextReferences) {
        references.clear();
        references.addAll(nextReferences == null ? List.of() : nextReferences);
    }

    private String safeMessage(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }
}
