package com.english.service.impl;

import com.english.service.RagChunker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SimpleRagChunker implements RagChunker {
    private final int maxChars;
    private final int overlapChars;

    public SimpleRagChunker(
            @Value("${rag.chunk.max-chars:800}") int maxChars,
            @Value("${rag.chunk.overlap-chars:120}") int overlapChars) {
        this.maxChars = Math.max(maxChars, 100);
        this.overlapChars = Math.max(0, Math.min(overlapChars, this.maxChars / 2));
    }

    @Override
    public List<String> split(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + maxChars, normalized.length());
            chunks.add(normalized.substring(start, end).trim());
            if (end == normalized.length()) {
                break;
            }
            start = Math.max(end - overlapChars, start + 1);
        }
        return chunks;
    }
}
