package com.english.config;

import com.english.entity.AdminOperationLog;
import com.english.entity.User;
import com.english.mapper.AdminOperationLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
public class AdminAuditAspect {
    private final AdminOperationLogMapper logMapper;
    private final ObjectMapper objectMapper;
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public AdminAuditAspect(AdminOperationLogMapper logMapper, ObjectMapper objectMapper) {
        this.logMapper = logMapper;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(adminAudit)")
    public Object record(ProceedingJoinPoint joinPoint, AdminAudit adminAudit) throws Throwable {
        try {
            Object result = joinPoint.proceed();
            saveLog(joinPoint, adminAudit, true, null, result);
            return result;
        } catch (Throwable error) {
            saveLog(joinPoint, adminAudit, false, error, null);
            throw error;
        }
    }

    private void saveLog(ProceedingJoinPoint joinPoint, AdminAudit adminAudit, boolean success, Throwable error, Object result) {
        try {
            HttpServletRequest request = currentRequest();
            User admin = AdminAuthContext.get();
            AdminOperationLog log = new AdminOperationLog();
            log.setAdminId(admin == null ? null : admin.getId());
            log.setAdminUsername(admin == null ? null : admin.getUsername());
            log.setModule(adminAudit.module());
            log.setAction(adminAudit.action());
            log.setTargetId(resolveTargetId(joinPoint, adminAudit.targetId()));
            log.setRequestMethod(request == null ? null : request.getMethod());
            log.setRequestPath(request == null ? null : request.getRequestURI());
            log.setIpAddress(resolveIp(request));
            log.setUserAgent(request == null ? null : request.getHeader("User-Agent"));
            log.setSuccess(success);
            log.setErrorMessage(error == null ? null : truncate(error.getMessage(), 1000));
            log.setRequestBody(truncate(toJson(joinPoint.getArgs()), 4000));
            log.setResponseBody(truncate(toJson(result), 4000));
            log.setCreatedAt(LocalDateTime.now());
            logMapper.save(log);
        } catch (Exception ignored) {
            // Audit logging must not break the business request.
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String resolveTargetId(ProceedingJoinPoint joinPoint, String expression) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = parameterNameDiscoverer.getParameterNames(signature.getMethod());
            Object[] args = joinPoint.getArgs();
            EvaluationContext context = new StandardEvaluationContext();
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }
            Object value = parser.parseExpression(expression).getValue(context);
            return value == null ? null : String.valueOf(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception error) {
            return String.valueOf(value);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
