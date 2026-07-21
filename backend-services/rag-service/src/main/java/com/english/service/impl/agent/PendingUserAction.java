package com.english.service.impl.agent;

import java.time.Instant;
import java.util.Map;

public record PendingUserAction(ActionType type,
                                Long userId,
                                String sessionId,
                                String description,
                                Map<String, Object> payload,
                                Instant createdAt,
                                Instant expiresAt) {

    public enum ActionType {
        UPDATE_PROFILE,
        SUBMIT_WRONG_RECORD,
        DELETE_WRONG_RECORD,
        MARK_WORD_KNOWN,
        RESET_WORD_PROGRESS,
        ADD_READING_FAVORITE,
        REMOVE_READING_FAVORITE
    }

    public boolean expired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    public Long longValue(String key) {
        Object value = payload == null ? null : payload.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.valueOf(text.trim());
        }
        return null;
    }

    public Integer integerValue(String key) {
        Object value = payload == null ? null : payload.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.valueOf(text.trim());
        }
        return null;
    }

    public String stringValue(String key) {
        Object value = payload == null ? null : payload.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }
}
