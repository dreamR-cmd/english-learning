package com.english.service.impl;

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

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 基于 Ollama 多模态模型的图片文字识别（OCR）。
 * 复用 rag.chat.ollama.base-url 指向的 Ollama 实例，
 * 视觉模型通过 rag.ocr.model 配置（默认 qwen2.5vl:7b，需预先 ollama pull）。
 */
@Component
public class RagOcrService {
    private static final Logger log = LoggerFactory.getLogger(RagOcrService.class);

    private static final String OCR_PROMPT = """
            请提取图片中的所有文字内容（包括中英文），按阅读顺序输出纯文本。
            表格内容用文本形式按行列描述。图片中没有文字时只输出：[无文字]
            """;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;
    private final String model;
    private final long timeoutSeconds;

    public RagOcrService(
            @Value("${rag.chat.ollama.base-url:http://host.docker.internal:11434}") String baseUrl,
            @Value("${rag.ocr.model:qwen2.5vl:7b}") String model,
            @Value("${rag.ocr.timeout-seconds:120}") long timeoutSeconds) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
        this.timeoutSeconds = Math.max(timeoutSeconds, 10);
    }

    /**
     * 识别单张图片中的文字；无文字或识别失败返回空字符串（失败仅告警，不阻断整份文档入库）。
     */
    public String recognize(byte[] imageBytes) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = Map.of(
                    "model", model,
                    "prompt", OCR_PROMPT,
                    "images", List.of(Base64.getEncoder().encodeToString(imageBytes)),
                    "stream", false
            );
            JsonNode response = rest.postForObject(
                    baseUrl + "/api/generate",
                    new HttpEntity<>(mapper.writeValueAsString(body), headers),
                    JsonNode.class);
            if (response == null || !response.hasNonNull("response")) {
                log.warn("[OCR] empty response from ollama, model={}", model);
                return "";
            }
            String text = response.get("response").asText("").trim();
            if (text.isBlank() || text.contains("[无文字]")) {
                return "";
            }
            return text;
        } catch (RuntimeException error) {
            log.warn("[OCR] recognition failed (model={}): {}", model, safeMessage(error));
            return "";
        }
    }

    public Duration timeout() {
        return Duration.ofSeconds(timeoutSeconds);
    }

    private String safeMessage(Throwable error) {
        String message = error.getMessage();
        return message == null ? error.getClass().getSimpleName() : message;
    }
}
