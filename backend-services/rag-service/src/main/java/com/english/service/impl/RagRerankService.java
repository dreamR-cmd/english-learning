package com.english.service.impl;

import com.english.dto.RagSearchItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 精排（rerank）：召回阶段（向量 + 关键词两路合并）产出过量的候选片段，
 * 这里用 Ollama 上的 rerank 模型（默认 qwen3-reranker，需 ollama pull）对每个
 * (query, chunk) 打相关性分，取回 topK。
 *
 * 失败降级：rerank 不可用时直接返回原始候选顺序（仅截断到 topK），不阻断问答链路。
 */
@Component
public class RagRerankService {
    private static final Logger log = LoggerFactory.getLogger(RagRerankService.class);

    private static final String RERANK_PROMPT = """
            Judge whether the Document meets the search requirement. Output a single relevance score \
            between 0 and 1, where 1 means fully relevant and 0 means irrelevant. \
            Output ONLY the number, no explanation.
            Query: %s
            Document: %s
            """;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;
    private final String model;
    private final boolean enabled;
    private final int timeoutSeconds;

    public RagRerankService(
            @Value("${rag.chat.ollama.base-url:http://host.docker.internal:11434}") String baseUrl,
            @Value("${rag.rerank.model:qwen3-reranker:0.6b}") String model,
            @Value("${rag.rerank.enabled:true}") boolean enabled,
            @Value("${rag.rerank.timeout-seconds:30}") int timeoutSeconds) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
        this.enabled = enabled;
        this.timeoutSeconds = Math.max(timeoutSeconds, 5);
    }

    /**
     * 对候选片段精排，返回相关性最高的前 topK 条。
     * 候选为空或 rerank 关闭时原样返回；单条打分失败该条按 0 分处理。
     */
    public List<RagSearchItem> rerank(String question, List<RagSearchItem> candidates, int topK) {
        if (!enabled || candidates == null || candidates.size() <= 1) {
            return truncate(candidates, topK);
        }
        try {
            List<Scored> scored = new ArrayList<>(candidates.size());
            for (RagSearchItem candidate : candidates) {
                scored.add(new Scored(candidate, relevanceScore(question, candidate.getSnippet())));
            }
            scored.sort(Comparator.comparingDouble((Scored s) -> s.score).reversed());
            return scored.stream().limit(topK).map(s -> s.item).toList();
        } catch (RuntimeException error) {
            log.warn("[Rerank] rerank failed, falling back to original order (model={}): {}",
                    model, safeMessage(error));
            return truncate(candidates, topK);
        }
    }

    private float relevanceScore(String question, String document) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("prompt", RERANK_PROMPT.formatted(question, document == null ? "" : document));
        body.put("stream", false);
        body.put("options", Map.of("temperature", 0.0, "num_predict", 8));

        String json;
        try {
            json = mapper.writeValueAsString(body);
        } catch (JsonProcessingException error) {
            log.warn("[Rerank] request serialization failed (model={}): {}", model, safeMessage(error));
            return 0f;
        }
        JsonNode response = rest.postForObject(
                baseUrl + "/api/generate",
                new HttpEntity<>(json, headers),
                JsonNode.class);
        if (response == null || !response.hasNonNull("response")) {
            return 0f;
        }
        return parseScore(response.get("response").asText(""));
    }

    private float parseScore(String raw) {
        if (raw == null) {
            return 0f;
        }
        java.util.Scanner scanner = new java.util.Scanner(raw.trim()).useLocale(java.util.Locale.ROOT);
        if (scanner.hasNextFloat()) {
            float score = scanner.nextFloat();
            // 模型可能输出 0-100 区间，统一归一到 0-1
            return score > 1f ? score / 100f : score;
        }
        return 0f;
    }

    private List<RagSearchItem> truncate(List<RagSearchItem> candidates, int topK) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        return candidates.stream().limit(topK).toList();
    }

    private String safeMessage(Throwable error) {
        String message = error.getMessage();
        return message == null ? error.getClass().getSimpleName() : message;
    }

    private record Scored(RagSearchItem item, float score) {
    }
}
