package com.english.service.impl.agent;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class RagIntentRouter {
    private static final Pattern ENGLISH_TERM_PATTERN = Pattern.compile("[a-zA-Z]{2,}");
    private static final Set<String> CONFIRMATION_MESSAGES = Set.of("确认", "确认执行", "执行", "可以", "可以执行", "好的", "好", "yes", "y");
    private static final Set<String> CANCELLATION_MESSAGES = Set.of("取消", "取消操作", "算了", "不用了", "先不用", "先不");

    public AgentIntent route(String question) {
        if (question == null || question.isBlank()) {
            return AgentIntent.GENERAL_CHAT;
        }
        String normalized = question.toLowerCase(Locale.ROOT).trim();

        if (CONFIRMATION_MESSAGES.contains(normalized) || CANCELLATION_MESSAGES.contains(normalized)) {
            return AgentIntent.USER_ACTION;
        }
        if (containsAny(normalized, "删除", "移除", "清空", "修改", "更新", "设置", "标记", "提交", "保存", "重置", "购买", "下单", "支付")
                || containsAny(normalized, "记录到错题", "加入错题", "记到错题", "认识这个单词", "这个单词认识")
                || containsAny(normalized, "加入收藏", "帮我收藏", "收藏阅读", "收藏文章", "收藏第", "取消收藏")) {
            return AgentIntent.USER_ACTION;
        }
        if (containsAny(normalized, "错题", "做错", "错因", "错误记录", "错题本")) {
            return AgentIntent.WRONG_RECORD;
        }
        if (containsAny(normalized, "今天", "每日", "今日", "复习", "学什么", "学习计划", "任务", "掌握", "进度")
                || containsAny(normalized, "cet4", "cet6", "kaoyan", "ielts", "toefl", "gre")) {
            return AgentIntent.LEARNING_PLAN;
        }
        if (containsAny(normalized, "作文", "翻译", "批改", "润色", "改写")) {
            return AgentIntent.WRITING_CORRECTION;
        }
        if (containsAny(normalized, "出题", "练习题", "测试", "测验", "专项练习")) {
            return AgentIntent.PRACTICE_GENERATE;
        }
        if (containsAny(normalized, "是什么意思", "什么意思", "语法", "文档", "资料", "pdf", "word", "解释", "造句", "例句", "怎么用", "发音", "读音")
                || ENGLISH_TERM_PATTERN.matcher(normalized).find()) {
            return AgentIntent.KNOWLEDGE_QA;
        }
        return AgentIntent.KNOWLEDGE_QA;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
