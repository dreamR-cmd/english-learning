package com.english.service.impl;

import com.english.entity.WrongRecord;
import com.english.service.PracticeService;
import com.english.service.UserService;
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
    private final UserService userService;
    private final PracticeService practiceService;
    private final int maxToolOutputChars;
    private final int maxToolListItems;

    public RagInternalApiClient(ObjectMapper objectMapper,
                                UserService userService,
                                PracticeService practiceService,
                                @Value("${rag.agent.max-tool-output-chars:12000}") int maxToolOutputChars,
                                @Value("${rag.agent.max-tool-list-items:30}") int maxToolListItems) {
        this.objectMapper = objectMapper;
        this.userService = userService;
        this.practiceService = practiceService;
        this.maxToolOutputChars = maxToolOutputChars;
        this.maxToolListItems = maxToolListItems;
    }

    public String getDailyWords(Long userId) {
        return getJson(() -> practiceService.getDailyWords(userId));
    }

    public String getModuleWords(Long userId, String moduleCode) {
        String safeModuleCode = moduleCode == null ? "" : moduleCode.trim();
        return getJson(() -> practiceService.getWordsByModule(safeModuleCode, userId));
    }

    public String getReviewWords(Long userId) {
        return getJson(() -> userService.getReviewWords(userId));
    }

    public String getWrongRecords(Long userId) {
        return getJson(() -> userService.getWrongRecords(userId));
    }

    public List<Map<String, Object>> getWrongRecordItems(Long userId) {
        try {
            List<WrongRecord> records = userService.getWrongRecords(userId);
            List<Map<String, Object>> items = new ArrayList<>();
            for (WrongRecord record : records) {
                items.add(objectMapper.convertValue(record, objectMapper.getTypeFactory()
                        .constructMapType(LinkedHashMap.class, String.class, Object.class)));
            }
            return items;
        } catch (RuntimeException error) {
            return List.of();
        }
    }

    public String updateProfile(Long userId, String nickname, Integer dailyWordTarget) {
        if ((nickname == null || nickname.isBlank()) && dailyWordTarget == null) {
            return "Nickname and daily word target cannot both be empty.";
        }
        return writeJson(userId, () -> userService.updateProfile(userId, nickname, dailyWordTarget));
    }

    public String submitWrongRecord(Long userId,
                                    String questionType,
                                    Long contentId,
                                    String contentTitle,
                                    String questionText,
                                    String userAnswer,
                                    String correctAnswer,
                                    String moduleCode) {
        WrongRecord record = new WrongRecord();
        record.setUserId(userId);
        record.setQuestionType(questionType == null || questionType.isBlank() ? "manual" : questionType.trim());
        record.setContentId(contentId);
        record.setContentTitle(trimToNull(contentTitle));
        record.setQuestionText(trimToNull(questionText));
        record.setUserAnswer(trimToNull(userAnswer));
        record.setCorrectAnswer(trimToNull(correctAnswer));
        record.setModuleCode(trimToNull(moduleCode));
        return writeJson(userId, () -> userService.saveWrongRecord(record));
    }

    public String removeWrongRecord(Long userId, Long wrongRecordId) {
        if (wrongRecordId == null) {
            return "Wrong record ID cannot be empty.";
        }
        return writeJson(userId, () -> {
            userService.removeWrongRecord(userId, wrongRecordId);
            return Map.of("success", true, "message", "Operation succeeded");
        });
    }

    public String markWordKnown(Long userId, Long wordId) {
        if (wordId == null) {
            return "Word ID cannot be empty.";
        }
        return writeJson(userId, () -> userService.markWordKnown(userId, wordId));
    }

    public String resetWordProgress(Long userId, Long wordId) {
        if (wordId == null) {
            return "Word ID cannot be empty.";
        }
        return writeJson(userId, () -> userService.resetWordProgress(userId, wordId));
    }

    public String addReadingFavorite(Long userId, Long readingId) {
        if (readingId == null) {
            return "Reading ID cannot be empty.";
        }
        return writeJson(userId, () -> userService.addFavorite(userId, readingId));
    }

    public String removeReadingFavorite(Long userId, Long readingId) {
        if (readingId == null) {
            return "Reading ID cannot be empty.";
        }
        return writeJson(userId, () -> {
            userService.removeFavorite(userId, readingId);
            return Map.of("success", true, "message", "Operation succeeded");
        });
    }

    private String getJson(LocalCall call) {
        try {
            return compactJson(call.execute());
        } catch (RuntimeException error) {
            return "Internal API call failed: " + safeMessage(error);
        }
    }

    private String writeJson(Long userId, LocalCall call) {
        if (userId == null) {
            return "Login is required.";
        }
        try {
            Object result = call.execute();
            if (result instanceof Map<?, ?>) {
                return compactJson(result);
            }
            Map<String, Object> wrapped = new LinkedHashMap<>();
            wrapped.put("success", true);
            wrapped.put("message", "Operation succeeded");
            if (result != null) {
                wrapped.put("data", result);
            }
            return compactJson(wrapped);
        } catch (RuntimeException error) {
            return "Internal write API call failed: " + safeMessage(error);
        }
    }

    @FunctionalInterface
    private interface LocalCall {
        Object execute();
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

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }
}
