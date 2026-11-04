package com.english.service.impl.agent;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 溢出时压缩上下文的对话记忆：
 * 非 system 消息数超过 maxMessages 时，把最早的（maxMessages - keepMessages）条
 * 交给 LLM 摘要，摘要合并进此前摘要并固定在上下文最前部；
 * 最近 keepMessages 条原文始终保留，保证承接追问的细节不丢。
 * 摘要调用失败时降级为直接丢弃，等价于普通滑动窗口。
 */
public class SummarizingChatMemory implements ChatMemory {
    private static final Logger log = LoggerFactory.getLogger(SummarizingChatMemory.class);

    private static final String SUMMARY_PREFIX = "【此前对话摘要】\n";

    private final Object id;
    private final ChatModel chatModel;
    private final int maxMessages;
    private final int keepMessages;
    private final int maxSummaryChars;
    private final List<ChatMessage> messages = new ArrayList<>();
    private String summary;

    public SummarizingChatMemory(Object id, ChatModel chatModel, int maxMessages, int keepMessages) {
        this(id, chatModel, maxMessages, keepMessages, 2000);
    }

    public SummarizingChatMemory(Object id,
                                 ChatModel chatModel,
                                 int maxMessages,
                                 int keepMessages,
                                 int maxSummaryChars) {
        this.id = id;
        this.chatModel = chatModel;
        this.maxMessages = Math.max(maxMessages, 2);
        this.keepMessages = Math.min(Math.max(keepMessages, 1), this.maxMessages - 1);
        this.maxSummaryChars = Math.max(maxSummaryChars, 200);
    }

    @Override
    public Object id() {
        return id;
    }

    @Override
    public synchronized void add(ChatMessage message) {
        if (message instanceof SystemMessage systemMessage) {
            // AiServices 每次请求都会重设 system message，只保留最新一条且固定在最前
            messages.removeIf(m -> m instanceof SystemMessage);
            messages.add(0, systemMessage);
            return;
        }
        messages.add(message);
        evictIfNeeded();
    }

    @Override
    public synchronized List<ChatMessage> messages() {
        List<ChatMessage> result = new ArrayList<>();
        if (!messages.isEmpty()) {
            result.add(messages.get(0)); // system message
        }
        if (summary != null && !summary.isBlank()) {
            result.add(SystemMessage.from(SUMMARY_PREFIX + summary));
        }
        for (int i = 1; i < messages.size(); i++) {
            result.add(messages.get(i));
        }
        return result;
    }

    @Override
    public synchronized void clear() {
        messages.clear();
        summary = null;
    }

    private void evictIfNeeded() {
        int nonSystemCount = countNonSystem();
        if (nonSystemCount <= maxMessages) {
            return;
        }
        int evictCount = nonSystemCount - keepMessages;
        List<ChatMessage> evicted = new ArrayList<>(evictCount);
        int removed = 0;
        for (int i = 1; i < messages.size() && removed < evictCount; ) {
            ChatMessage m = messages.get(i);
            if (!(m instanceof SystemMessage)) {
                evicted.add(m);
                messages.remove(i);
                removed++;
            } else {
                i++;
            }
        }
        String newSummary = summarize(evicted);
        if (newSummary != null) {
            summary = newSummary;
        }
    }

    private int countNonSystem() {
        return (int) messages.stream().filter(m -> !(m instanceof SystemMessage)).count();
    }

    private String summarize(List<ChatMessage> evicted) {
        try {
            StringBuilder conversation = new StringBuilder();
            if (summary != null && !summary.isBlank()) {
                conversation.append("已有摘要：\n").append(summary).append("\n\n新增对话：\n");
            }
            for (ChatMessage message : evicted) {
                conversation.append(describe(message)).append('\n');
            }
            String prompt = """
                    请把以下对话内容压缩成一段简洁的中文摘要，用于替代原始对话作为后续回答的上下文。
                    保留：用户的明确目标、提到的单词/知识点、已执行的操作及其结果、未完成的待办。
                    丢弃：寒暄、重复内容。直接输出摘要正文，不要任何解释。

                    """ + conversation;
            String result = chatModel.chat(prompt);
            if (result == null || result.isBlank()) {
                return null;
            }
            result = result.trim();
            // 摘要超长时对摘要本身再压缩一轮，保证摘要有界，不会随会话无限增长
            if (result.length() > maxSummaryChars) {
                result = chatModel.chat("""
                        请把以下对话摘要压缩到 %d 字以内，保留最重要的目标和结论，直接输出压缩后的摘要。

                        %s""".formatted(maxSummaryChars, result)).trim();
                if (result.length() > maxSummaryChars) {
                    // 模型没压缩到位则硬截断兜底
                    result = result.substring(0, maxSummaryChars);
                }
            }
            return result;
        } catch (RuntimeException error) {
            log.warn("Chat memory summarization failed, evicted messages will be dropped. memoryId={}", id, error);
            return null;
        }
    }

    private String describe(ChatMessage message) {
        String type = message.type().name();
        String text = message.toString();
        return type + ": " + text;
    }
}
