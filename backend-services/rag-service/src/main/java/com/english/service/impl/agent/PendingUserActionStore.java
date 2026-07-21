package com.english.service.impl.agent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PendingUserActionStore {
    private final Map<String, PendingUserAction> actions = new ConcurrentHashMap<>();
    private final long ttlSeconds;

    public PendingUserActionStore(@Value("${rag.agent.pending-action-ttl-seconds:600}") long ttlSeconds) {
        this.ttlSeconds = ttlSeconds <= 0 ? 600 : ttlSeconds;
    }

    public PendingUserAction save(Long userId,
                                  String sessionId,
                                  PendingUserAction.ActionType type,
                                  String description,
                                  Map<String, Object> payload) {
        Instant now = Instant.now();
        PendingUserAction action = new PendingUserAction(
                type,
                userId,
                normalizeSessionId(userId, sessionId),
                description,
                payload == null ? Map.of() : Map.copyOf(payload),
                now,
                now.plusSeconds(ttlSeconds)
        );
        actions.put(key(userId, sessionId), action);
        return action;
    }

    public Optional<PendingUserAction> find(Long userId, String sessionId) {
        String key = key(userId, sessionId);
        PendingUserAction action = actions.get(key);
        if (action == null) {
            return Optional.empty();
        }
        if (action.expired(Instant.now())) {
            actions.remove(key, action);
            return Optional.empty();
        }
        return Optional.of(action);
    }

    public Optional<PendingUserAction> consume(Long userId, String sessionId) {
        PendingUserAction action = actions.remove(key(userId, sessionId));
        if (action == null || action.expired(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(action);
    }

    public boolean clear(Long userId, String sessionId) {
        return actions.remove(key(userId, sessionId)) != null;
    }

    private String key(Long userId, String sessionId) {
        return userId + ":" + normalizeSessionId(userId, sessionId);
    }

    private String normalizeSessionId(Long userId, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return String.valueOf(userId);
        }
        return sessionId.trim();
    }
}
