package com.english.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class RagInternalApiClient {
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String INTERNAL_GATEWAY_SECRET_HEADER = "X-Internal-Gateway-Secret";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String userServiceUrl;
    private final String learningServiceUrl;
    private final String internalGatewaySecret;
    private final int maxToolOutputChars;

    public RagInternalApiClient(ObjectMapper objectMapper,
                                @Value("${rag.agent.user-service-url:http://localhost:8088}") String userServiceUrl,
                                @Value("${rag.agent.learning-service-url:http://localhost:8089}") String learningServiceUrl,
                                @Value("${gateway.internal.secret:${INTERNAL_GATEWAY_SECRET:english-learning-internal-secret}}") String internalGatewaySecret,
                                @Value("${rag.agent.max-tool-output-chars:5000}") int maxToolOutputChars) {
        this.restClient = RestClient.builder().build();
        this.objectMapper = objectMapper;
        this.userServiceUrl = trimTrailingSlash(userServiceUrl);
        this.learningServiceUrl = trimTrailingSlash(learningServiceUrl);
        this.internalGatewaySecret = internalGatewaySecret;
        this.maxToolOutputChars = maxToolOutputChars;
    }

    public String getDailyWords(Long userId) {
        return getLearning(userId, "/api/practice/words/daily");
    }

    public String getModuleWords(Long userId, String moduleCode) {
        String safeModuleCode = UriUtils.encodePathSegment(moduleCode == null ? "" : moduleCode.trim(), StandardCharsets.UTF_8);
        return getLearning(userId, "/api/practice/words/" + safeModuleCode);
    }

    public String getReviewWords(Long userId) {
        return getUser(userId, "/api/user/word-progress/review");
    }

    public String getWrongRecords(Long userId) {
        return getUser(userId, "/api/user/wrong-records");
    }

    private String getLearning(Long userId, String path) {
        return getJson(learningServiceUrl, userId, path);
    }

    private String getUser(Long userId, String path) {
        return getJson(userServiceUrl, userId, path);
    }

    private String getJson(String baseUrl, Long userId, String path) {
        try {
            Object body = restClient.get()
                    .uri(baseUrl + path)
                    .header(USER_ID_HEADER, String.valueOf(userId))
                    .header(INTERNAL_GATEWAY_SECRET_HEADER, internalGatewaySecret)
                    .retrieve()
                    .body(Object.class);
            return compactJson(unwrapApiResult(body));
        } catch (RuntimeException error) {
            return "内部接口调用失败：" + safeMessage(error);
        }
    }

    @SuppressWarnings("unchecked")
    private Object unwrapApiResult(Object body) {
        if (body instanceof Map<?, ?> map && map.containsKey("data")) {
            Object code = map.get("code");
            Object message = map.get("message");
            Object data = map.get("data");
            if (code != null && !"200".equals(String.valueOf(code))) {
                return Map.of("code", code, "message", message == null ? "" : message);
            }
            return data;
        }
        return body;
    }

    private String compactJson(Object value) {
        if (value == null) {
            return "没有查询到数据。";
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            if (json.length() <= maxToolOutputChars) {
                return json;
            }
            return json.substring(0, maxToolOutputChars) + "...（结果较长，已截断）";
        } catch (JsonProcessingException error) {
            return String.valueOf(value);
        }
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }
}
