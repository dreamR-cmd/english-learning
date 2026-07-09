package com.english.gateway.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class GatewayTokenService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String secret;

    public GatewayTokenService(@Value("${admin.token.secret:english-learning-admin-secret}") String secret) {
        this.secret = secret;
    }

    public TokenPayload validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("未登录");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            throw new RuntimeException("无效 token");
        }
        String payload = decodePayload(parts[0]);
        if (!sign(payload).equals(parts[1])) {
            throw new RuntimeException("token 签名无效");
        }
        String[] fields = payload.split(":");
        if (fields.length != 2) {
            throw new RuntimeException("token 内容无效");
        }
        long expiresAt = parseLong(fields[1], "token 过期时间无效");
        if (Instant.now().getEpochSecond() > expiresAt) {
            throw new RuntimeException("登录已过期");
        }
        Long userId = parseLong(fields[0], "token 用户无效");
        return new TokenPayload(userId, expiresAt);
    }

    private String decodePayload(String encodedPayload) {
        try {
            return new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException error) {
            throw new RuntimeException("token 内容无效");
        }
    }

    private long parseLong(String value, String message) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException error) {
            throw new RuntimeException(message);
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception error) {
            throw new RuntimeException("token 签名失败", error);
        }
    }

    public record TokenPayload(Long userId, long expiresAt) {}
}
