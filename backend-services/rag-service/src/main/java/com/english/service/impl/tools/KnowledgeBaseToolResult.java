package com.english.service.impl.tools;

import com.english.dto.RagSearchItem;

import java.util.List;

public record KnowledgeBaseToolResult(String content, List<RagSearchItem> references) {
    public KnowledgeBaseToolResult {
        references = references == null ? List.of() : List.copyOf(references);
    }
}
