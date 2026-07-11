package com.english.gateway.config;

import com.english.gateway.filter.GatewayAuthFilter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class RateLimitConfig {
    private static final String UNKNOWN_IP = "unknown";

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just("ip:" + resolveClientIp(exchange.getRequest().getHeaders(),
                exchange.getRequest().getRemoteAddress()));
    }

    @Bean
    @Primary
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst(GatewayAuthFilter.USER_ID_HEADER);
            if (StringUtils.hasText(userId)) {
                return Mono.just("user:" + userId);
            }
            return Mono.just("ip:" + resolveClientIp(exchange.getRequest().getHeaders(),
                    exchange.getRequest().getRemoteAddress()));
        };
    }

    private String resolveClientIp(HttpHeaders headers, InetSocketAddress remoteAddress) {
        String forwardedFor = headers.getFirst("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = headers.getFirst("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return UNKNOWN_IP;
    }
}
