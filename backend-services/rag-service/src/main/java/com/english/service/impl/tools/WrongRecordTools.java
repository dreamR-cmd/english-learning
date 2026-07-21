package com.english.service.impl.tools;

import com.english.service.impl.RagInternalApiClient;
import dev.langchain4j.agent.tool.Tool;

public final class WrongRecordTools {
    private final Long userId;
    private final RagInternalApiClient internalApiClient;

    public WrongRecordTools(Long userId, RagInternalApiClient internalApiClient) {
        this.userId = userId;
        this.internalApiClient = internalApiClient;
    }

    @Tool("查询当前用户的错题记录。适合回答：我的错题、最近做错了什么、错题本内容、错因分析。")
    public String getWrongRecords() {
        return internalApiClient.getWrongRecords(userId);
    }
}
