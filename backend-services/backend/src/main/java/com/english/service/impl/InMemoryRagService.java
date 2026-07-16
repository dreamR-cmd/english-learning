package com.english.service.impl;

import com.english.dto.RagAnswerResponse;
import com.english.dto.RagDocumentRequest;
import com.english.dto.RagDocumentResponse;
import com.english.dto.RagSearchItem;
import com.english.service.RagService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class InMemoryRagService implements RagService {
    private final List<DocumentEntry> documents = new CopyOnWriteArrayList<>();
    private final int defaultMaxResults;

    public InMemoryRagService(@Value("${rag.max-results:5}") int defaultMaxResults) {
        this.defaultMaxResults = defaultMaxResults;
    }

    @Override
    public RagDocumentResponse addDocument(RagDocumentRequest request) {
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("content cannot be empty");
        }
        String title = request.getTitle() == null || request.getTitle().isBlank()
                ? "Untitled Document"
                : request.getTitle().trim();
        DocumentEntry entry = new DocumentEntry(
                UUID.randomUUID().toString(),
                title,
                request.getContent().trim(),
                request.getSource(),
                LocalDateTime.now()
        );
        documents.add(entry);
        return toResponse(entry);
    }

    @Override
    public List<RagDocumentResponse> listDocuments() {
        return documents.stream()
                .sorted(Comparator.comparing(DocumentEntry::createdAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<RagSearchItem> search(String question, Integer topK) {
        if (question == null || question.isBlank()) {
            return List.of();
        }
        int limit = topK == null || topK <= 0 ? defaultMaxResults : Math.min(topK, 20);
        List<String> terms = tokenize(question);
        return documents.stream()
                .map(document -> toSearchItem(document, terms))
                .filter(item -> item.getScore() > 0)
                .sorted(Comparator.comparingInt(RagSearchItem::getScore).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public RagAnswerResponse ask(String question, Integer topK) {
        List<RagSearchItem> references = search(question, topK);
        if (references.isEmpty()) {
            return new RagAnswerResponse("暂时没有检索到相关资料，请先上传文档或换一个问题。", references);
        }
        StringBuilder answer = new StringBuilder("已根据知识库检索到相关片段：");
        for (int i = 0; i < references.size(); i++) {
            RagSearchItem item = references.get(i);
            answer.append("\n").append(i + 1).append(". ")
                    .append(item.getTitle()).append("：")
                    .append(item.getSnippet());
        }
        answer.append("\n\n当前单体 RAG 已完成检索骨架，后续可以在这里接入向量库和大模型生成最终答案。");
        return new RagAnswerResponse(answer.toString(), references);
    }

    private RagDocumentResponse toResponse(DocumentEntry entry) {
        return new RagDocumentResponse(entry.id(), entry.title(), entry.source(), entry.createdAt());
    }

    private RagSearchItem toSearchItem(DocumentEntry document, List<String> terms) {
        String content = document.content().toLowerCase(Locale.ROOT);
        String title = document.title().toLowerCase(Locale.ROOT);
        int score = 0;
        for (String term : terms) {
            if (title.contains(term)) {
                score += 3;
            }
            if (content.contains(term)) {
                score += 1;
            }
        }
        return new RagSearchItem(document.id(), document.title(), document.source(), buildSnippet(document.content(), terms), score);
    }

    private String buildSnippet(String content, List<String> terms) {
        String lower = content.toLowerCase(Locale.ROOT);
        int index = -1;
        for (String term : terms) {
            index = lower.indexOf(term);
            if (index >= 0) {
                break;
            }
        }
        if (index < 0) {
            index = 0;
        }
        int start = Math.max(0, index - 80);
        int end = Math.min(content.length(), index + 220);
        return content.substring(start, end).replaceAll("\\s+", " ").trim();
    }

    private List<String> tokenize(String text) {
        String[] parts = text.toLowerCase(Locale.ROOT).split("[^\\p{IsAlphabetic}\\p{IsDigit}\\u4e00-\\u9fa5]+");
        List<String> terms = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                terms.add(part);
            }
        }
        return terms;
    }

    private record DocumentEntry(String id, String title, String content, String source, LocalDateTime createdAt) {}
}
