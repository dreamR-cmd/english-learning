package com.english.service.impl.tools;

import com.english.service.impl.RagInternalApiClient;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

public final class LearningTools {
    private final Long userId;
    private final RagInternalApiClient internalApiClient;

    public LearningTools(Long userId, RagInternalApiClient internalApiClient) {
        this.userId = userId;
        this.internalApiClient = internalApiClient;
    }

    @Tool("查询当前用户今天需要练习的每日单词列表。适合回答：今天学什么、今日单词、每日任务。")
    public String getTodayWords() {
        return internalApiClient.getDailyWords(userId);
    }

    @Tool("查询当前用户已经达到复习条件的单词。适合回答：我有哪些复习词、需要复习什么。")
    public String getReviewWords() {
        return internalApiClient.getReviewWords(userId);
    }

    @Tool("按考试模块查询单词列表。模块编码通常是 cet4、cet6、kaoyan、ielts、toefl、gre。")
    public String getModuleWords(@P("模块编码，例如 cet4、cet6、kaoyan、ielts、toefl、gre") String moduleCode) {
        if (moduleCode == null || moduleCode.isBlank()) {
            return "模块编码不能为空。可用示例：cet4、cet6、kaoyan、ielts、toefl、gre。";
        }
        return internalApiClient.getModuleWords(userId, moduleCode);
    }
}
