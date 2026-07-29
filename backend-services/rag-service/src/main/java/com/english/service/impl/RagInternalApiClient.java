package com.english.service.impl;

import com.english.client.LearningServiceClient;
import com.english.client.UserServiceClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RagInternalApiClient {
    private final ObjectMapper objectMapper;
    private final UserServiceClient userServiceClient;
    private final LearningServiceClient learningServiceClient;
    private final String internalGatewaySecret;
    private final int maxToolOutputChars;
    private final int maxToolListItems;

    public RagInternalApiClient(ObjectMapper objectMapper,
                                UserServiceClient userServiceClient,
                                LearningServiceClient learningServiceClient,
                                @Value("${gateway.internal.secret:${INTERNAL_GATEWAY_SECRET:english-learning-internal-secret}}") String internalGatewaySecret,
                                @Value("${rag.agent.max-tool-output-chars:12000}") int maxToolOutputChars,
                                @Value("${rag.agent.max-tool-list-items:30}") int maxToolListItems) {
        this.objectMapper = objectMapper;
        this.userServiceClient = userServiceClient;
        this.learningServiceClient = learningServiceClient;
        this.internalGatewaySecret = internalGatewaySecret;
        this.maxToolOutputChars = maxToolOutputChars;
        this.maxToolListItems = maxToolListItems;
    }

    public String getDailyWords(Long userId) {
        return getJson(() -> learningServiceClient.getDailyWords(userId, internalGatewaySecret));
    }

    public String getModuleWords(Long userId, String moduleCode) {
        String safeModuleCode = moduleCode == null ? "" : moduleCode.trim();
        return getJson(() -> learningServiceClient.getModuleWords(userId, internalGatewaySecret, safeModuleCode));
    }

    public String getReviewWords(Long userId) {
        return getJson(() -> userServiceClient.getReviewWords(userId, internalGatewaySecret));
    }

    public String getWrongRecords(Long userId) {
        return getJson(() -> userServiceClient.getWrongRecords(userId, internalGatewaySecret));
    }

    public List<Map<String, Object>> getWrongRecordItems(Long userId) {
        try {
            Object data = unwrapApiResult(userServiceClient.getWrongRecords(userId, internalGatewaySecret));
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
            return "Nickname and daily word target cannot both be empty.";
        }
        return writeJson(userId, () -> userServiceClient.updateProfile(userId, internalGatewaySecret, body));
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
        return writeJson(userId, () -> userServiceClient.submitWrongRecord(userId, internalGatewaySecret, body));
    }

    public String removeWrongRecord(Long userId, Long wrongRecordId) {
        if (wrongRecordId == null) {
            return "Wrong record ID cannot be empty.";
        }
        return writeJson(userId, () -> userServiceClient.removeWrongRecord(userId, internalGatewaySecret, wrongRecordId));
    }

    public String markWordKnown(Long userId, Long wordId) {
        if (wordId == null) {
            return "Word ID cannot be empty.";
        }
        return writeJson(userId, () -> userServiceClient.markWordKnown(userId, internalGatewaySecret, Map.of("wordId", wordId)));
    }

    public String resetWordProgress(Long userId, Long wordId) {
        if (wordId == null) {
            return "Word ID cannot be empty.";
        }
        return writeJson(userId, () -> userServiceClient.resetWordProgress(userId, internalGatewaySecret, Map.of("wordId", wordId)));
    }

    public String addReadingFavorite(Long userId, Long readingId) {
        if (readingId == null) {
            return "Reading ID cannot be empty.";
        }
        return writeJson(userId, () -> userServiceClient.addReadingFavorite(userId, internalGatewaySecret, Map.of("readingId", readingId)));
    }

    public String removeReadingFavorite(Long userId, Long readingId) {
        if (readingId == null) {
            return "Reading ID cannot be empty.";
        }
        return writeJson(userId, () -> userServiceClient.removeReadingFavorite(userId, internalGatewaySecret, readingId));
    }

    private String getJson(FeignCall call) {
        try {
            return compactJson(unwrapApiResult(call.execute()));
        } catch (RuntimeException error) {
            return "Internal API call failed: " + safeMessage(error);
        }
    }

    private String writeJson(Long userId, FeignCall call) {
        if (userId == null) {
            return "Login is required.";
        }
        try {
            return compactJson(unwrapWriteResult(call.execute()));
        } catch (RuntimeException error) {
            return "Internal write API call failed: " + safeMessage(error);
        }
    }

    @FunctionalInterface
    private interface FeignCall {
        Object execute();
    }

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
                    ? (success ? "Operation succeeded" : "Operation failed")
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
            return "No data found.";
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
                    "assistantInstruction", "Answer using the provided records. If totalCount is greater than returnedCount, say only that the latest visible records are shown. Do not mention transport details."
            );
        }
        return Map.of(
                "data", String.valueOf(value),
                "assistantInstruction", "Answer using the provided content. Do not mention transport details."
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

    private String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }
}
