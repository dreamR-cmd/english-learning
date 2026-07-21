package com.english.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final int maxToolListItems;

    public RagInternalApiClient(ObjectMapper objectMapper,
                                @Value("${rag.agent.user-service-url:http://localhost:8088}") String userServiceUrl,
                                @Value("${rag.agent.learning-service-url:http://localhost:8089}") String learningServiceUrl,
                                @Value("${gateway.internal.secret:${INTERNAL_GATEWAY_SECRET:english-learning-internal-secret}}") String internalGatewaySecret,
                                @Value("${rag.agent.max-tool-output-chars:12000}") int maxToolOutputChars,
                                @Value("${rag.agent.max-tool-list-items:30}") int maxToolListItems) {
        this.restClient = RestClient.builder().build();
        this.objectMapper = objectMapper;
        this.userServiceUrl = trimTrailingSlash(userServiceUrl);
        this.learningServiceUrl = trimTrailingSlash(learningServiceUrl);
        this.internalGatewaySecret = internalGatewaySecret;
        this.maxToolOutputChars = maxToolOutputChars;
        this.maxToolListItems = maxToolListItems;
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

    public List<Map<String, Object>> getWrongRecordItems(Long userId) {
        try {
            Object data = unwrapApiResult(getObject(userServiceUrl, userId, "/api/user/wrong-records"));
            if (data instanceof List<?> list) {
                List<Map<String, Object>> records = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        records.add(toStringKeyMap(map));
                    }
                }
                return records;
            }
            return List.of();
        } catch (RuntimeException error) {
            return List.of();
        }
    }

    public String updateProfile(Long userId, String nickname, Integer dailyWordTarget) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (nickname != null && !nickname.isBlank()) {
            body.put("nickname", nickname.trim());
        }
        if (dailyWordTarget != null) {
            body.put("dailyWordTarget", dailyWordTarget);
        }
        if (body.isEmpty()) {
            return "昵称和每日单词目标不能同时为空。";
        }
        return putUser(userId, "/api/user/profile", body);
    }

    public String submitWrongRecord(Long userId,
                                    String questionType,
                                    Long contentId,
                                    String contentTitle,
                                    String questionText,
                                    String userAnswer,
                                    String correctAnswer,
                                    String moduleCode) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("questionType", questionType == null || questionType.isBlank() ? "manual" : questionType.trim());
        putIfPresent(body, "contentId", contentId);
        putIfPresent(body, "contentTitle", contentTitle);
        putIfPresent(body, "questionText", questionText);
        putIfPresent(body, "userAnswer", userAnswer);
        putIfPresent(body, "correctAnswer", correctAnswer);
        putIfPresent(body, "moduleCode", moduleCode);
        return postUser(userId, "/api/user/wrong-records", body);
    }

    public String removeWrongRecord(Long userId, Long wrongRecordId) {
        if (wrongRecordId == null) {
            return "错题记录 ID 不能为空。";
        }
        return deleteUser(userId, "/api/user/wrong-records/" + wrongRecordId);
    }

    public String markWordKnown(Long userId, Long wordId) {
        if (wordId == null) {
            return "单词 ID 不能为空。";
        }
        return postUser(userId, "/api/user/word-progress/known", Map.of("wordId", wordId));
    }

    public String resetWordProgress(Long userId, Long wordId) {
        if (wordId == null) {
            return "单词 ID 不能为空。";
        }
        return postUser(userId, "/api/user/word-progress/reset", Map.of("wordId", wordId));
    }

    public String addReadingFavorite(Long userId, Long readingId) {
        if (readingId == null) {
            return "阅读文章 ID 不能为空。";
        }
        return postUser(userId, "/api/user/favorites", Map.of("readingId", readingId));
    }

    public String removeReadingFavorite(Long userId, Long readingId) {
        if (readingId == null) {
            return "阅读文章 ID 不能为空。";
        }
        return deleteUser(userId, "/api/user/favorites/" + readingId);
    }

    private String getLearning(Long userId, String path) {
        return getJson(learningServiceUrl, userId, path);
    }

    private String getUser(Long userId, String path) {
        return getJson(userServiceUrl, userId, path);
    }

    private String postUser(Long userId, String path, Object requestBody) {
        return writeJson("POST", userServiceUrl, userId, path, requestBody);
    }

    private String putUser(Long userId, String path, Object requestBody) {
        return writeJson("PUT", userServiceUrl, userId, path, requestBody);
    }

    private String deleteUser(Long userId, String path) {
        return writeJson("DELETE", userServiceUrl, userId, path, null);
    }

    private String getJson(String baseUrl, Long userId, String path) {
        try {
            Object body = getObject(baseUrl, userId, path);
            return compactJson(unwrapApiResult(body));
        } catch (RuntimeException error) {
            return "内部接口调用失败：" + safeMessage(error);
        }
    }

    private Object getObject(String baseUrl, Long userId, String path) {
        return restClient.get()
                .uri(baseUrl + path)
                .header(USER_ID_HEADER, String.valueOf(userId))
                .header(INTERNAL_GATEWAY_SECRET_HEADER, internalGatewaySecret)
                .retrieve()
                .body(Object.class);
    }

    private String writeJson(String method, String baseUrl, Long userId, String path, Object requestBody) {
        if (userId == null) {
            return "未登录，无法执行操作。";
        }
        try {
            Object body = switch (method) {
                case "POST" -> restClient.post()
                        .uri(baseUrl + path)
                        .header(USER_ID_HEADER, String.valueOf(userId))
                        .header(INTERNAL_GATEWAY_SECRET_HEADER, internalGatewaySecret)
                        .body(requestBody)
                        .retrieve()
                        .body(Object.class);
                case "PUT" -> restClient.put()
                        .uri(baseUrl + path)
                        .header(USER_ID_HEADER, String.valueOf(userId))
                        .header(INTERNAL_GATEWAY_SECRET_HEADER, internalGatewaySecret)
                        .body(requestBody)
                        .retrieve()
                        .body(Object.class);
                case "DELETE" -> restClient.delete()
                        .uri(baseUrl + path)
                        .header(USER_ID_HEADER, String.valueOf(userId))
                        .header(INTERNAL_GATEWAY_SECRET_HEADER, internalGatewaySecret)
                        .retrieve()
                        .body(Object.class);
                default -> throw new IllegalArgumentException("不支持的内部写方法：" + method);
            };
            return compactJson(unwrapWriteResult(body));
        } catch (RuntimeException error) {
            return "内部写接口调用失败：" + safeMessage(error);
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

    private Object unwrapWriteResult(Object body) {
        if (body instanceof Map<?, ?> map && (map.containsKey("code") || map.containsKey("message") || map.containsKey("data"))) {
            Object code = map.get("code");
            Object message = map.get("message");
            Object data = map.get("data");
            Map<String, Object> result = new LinkedHashMap<>();
            boolean success = code == null || "200".equals(String.valueOf(code));
            result.put("success", success);
            result.put("message", message == null || String.valueOf(message).isBlank()
                    ? (success ? "操作成功" : "操作失败")
                    : message);
            if (data != null) {
                result.put("data", data);
            }
            return result;
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
            return objectMapper.writeValueAsString(compactForModel(value));
        } catch (JsonProcessingException error) {
            return String.valueOf(value);
        }
    }

    private Object compactForModel(Object value) {
        if (value instanceof List<?> list) {
            int returnedCount = Math.min(list.size(), maxToolListItems);
            List<Object> items = new ArrayList<>(list.subList(0, returnedCount));
            return Map.of(
                    "totalCount", list.size(),
                    "returnedCount", returnedCount,
                    "items", items,
                    "assistantInstruction", "请基于已提供记录回答。若 totalCount 大于 returnedCount，只说“先列出最近/当前可见的 returnedCount 条”，不要提底层接口或传输细节。"
            );
        }
        return Map.of(
                "data", String.valueOf(value),
                "assistantInstruction", "请基于已提供内容回答，不要提底层接口或传输细节。"
        );
    }

    private void putIfPresent(Map<String, Object> body, String key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String text) {
            if (text.isBlank()) {
                return;
            }
            body.put(key, text.trim());
            return;
        }
        body.put(key, value);
    }

    private Map<String, Object> toStringKeyMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() != null) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return result;
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
