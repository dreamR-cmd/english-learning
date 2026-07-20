package com.english.service.impl.tools;

import com.english.dto.RagSearchItem;
import com.english.service.impl.RagInternalApiClient;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.ArrayList;
import java.util.List;

public final class RagAgentTools {
    private final Long userId;
    private final String sessionId;
    private final Integer topK;
    private final RagInternalApiClient internalApiClient;
    private final KnowledgeBaseTool knowledgeBaseTool;
    private final List<RagSearchItem> references = new ArrayList<>();

    //构造函数，初始化 RagAgentTools 实例
    public RagAgentTools(Long userId,
                         String sessionId,
                         Integer topK,
                         RagInternalApiClient internalApiClient,
                         KnowledgeBaseTool knowledgeBaseTool) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.topK = topK;
        this.internalApiClient = internalApiClient;
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

    @Tool("查询当前用户今天需要练习的每日单词列表。适合回答：今天学什么、今日单词、每日任务。")
    public String getTodayWords() {
        return internalApiClient.getDailyWords(userId);
    }

    @Tool("查询当前用户已经达到复习条件的单词。适合回答：我有哪些复习词、需要复习什么。")
    public String getReviewWords() {
        return internalApiClient.getReviewWords(userId);
    }

    @Tool("查询当前用户的错题记录。适合回答：我的错题、最近做错了什么、错题本内容。")
    public String getWrongRecords() {
        return internalApiClient.getWrongRecords(userId);
    }

    @Tool("按考试模块查询单词列表。模块编码通常是 cet4、cet6、kaoyan、ielts、toefl、gre。")
    public String getModuleWords(@P("模块编码，例如 cet4、cet6、kaoyan、ielts、toefl、gre") String moduleCode) {
        if (moduleCode == null || moduleCode.isBlank()) {
            return "模块编码不能为空。可用示例：cet4、cet6、kaoyan、ielts、toefl、gre。";
        }
        return internalApiClient.getModuleWords(userId, moduleCode);
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
