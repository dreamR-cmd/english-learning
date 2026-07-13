package com.english.gateway.filter;

import com.english.gateway.auth.GatewayTokenService;
import com.english.gateway.config.GatewayAuthProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class GatewayAuthFilter implements GlobalFilter, Ordered {
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String TOKEN_EXPIRES_AT_HEADER = "X-Token-Expires-At";
    public static final String INTERNAL_GATEWAY_SECRET_HEADER = "X-Internal-Gateway-Secret";

    private final GatewayTokenService tokenService;
    private final GatewayAuthProperties authProperties;
    private final String internalGatewaySecret;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public GatewayAuthFilter(GatewayTokenService tokenService,
                             GatewayAuthProperties authProperties,
                             @Value("${gateway.internal.secret:${INTERNAL_GATEWAY_SECRET:english-learning-internal-secret}}") String internalGatewaySecret) {
        this.tokenService = tokenService;
        this.authProperties = authProperties;
        this.internalGatewaySecret = internalGatewaySecret;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest sanitizedRequest = removeUserContextHeaders(exchange.getRequest());
        ServerWebExchange sanitizedExchange = exchange.mutate().request(sanitizedRequest).build();

        if (!shouldAuthenticate(sanitizedRequest)) {
            return chain.filter(withInternalSecret(sanitizedExchange));
        }

        try {
            String token = resolveBearerToken(sanitizedRequest.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
            GatewayTokenService.TokenPayload payload = tokenService.validateToken(token);
            ServerHttpRequest authenticatedRequest = sanitizedRequest.mutate()
                    .header(USER_ID_HEADER, String.valueOf(payload.userId()))
                    .header(TOKEN_EXPIRES_AT_HEADER, String.valueOf(payload.expiresAt()))
                    .header(INTERNAL_GATEWAY_SECRET_HEADER, internalGatewaySecret)
                    .build();
            return chain.filter(sanitizedExchange.mutate().request(authenticatedRequest).build());
        } catch (RuntimeException error) {
            return writeError(sanitizedExchange, HttpStatus.UNAUTHORIZED, error.getMessage());
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private boolean shouldAuthenticate(ServerHttpRequest request) {
        if (!authProperties.isEnabled() || HttpMethod.OPTIONS.equals(request.getMethod())) {
            return false;
        }
        String path = request.getURI().getPath();
        if (!path.startsWith("/api/")) {
            return false;
        }
        return authProperties.getExcludePaths().stream().noneMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private ServerHttpRequest removeUserContextHeaders(ServerHttpRequest request) {
        return request.mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(TOKEN_EXPIRES_AT_HEADER);
                    headers.remove(INTERNAL_GATEWAY_SECRET_HEADER);
                })
                .build();
    }

    private ServerWebExchange withInternalSecret(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(INTERNAL_GATEWAY_SECRET_HEADER, internalGatewaySecret)
                .build();
        return exchange.mutate().request(request).build();
    }

    private String resolveBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new RuntimeException("未登录");
        }
        return authorization.substring(7);
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        byte[] bytes = ("{\"code\":" + status.value() + ",\"message\":\"" + escape(message) + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
