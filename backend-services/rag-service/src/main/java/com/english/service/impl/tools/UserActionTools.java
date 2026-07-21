package com.english.service.impl.tools;

import com.english.service.impl.RagInternalApiClient;
import com.english.service.impl.agent.PendingUserAction;
import com.english.service.impl.agent.PendingUserActionStore;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UserActionTools {
    private final Long userId;
    private final String sessionId;
    private final RagInternalApiClient internalApiClient;
    private final PendingUserActionStore pendingActionStore;
    private boolean createdPendingActionInThisTurn;

    public UserActionTools(Long userId,
                           String sessionId,
                           RagInternalApiClient internalApiClient,
                           PendingUserActionStore pendingActionStore) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.internalApiClient = internalApiClient;
        this.pendingActionStore = pendingActionStore;
    }

    @Tool("查看当前对话是否存在等待用户确认的写操作。")
    public String getPendingAction() {
        return pendingActionStore.find(userId, sessionId)
                .map(action -> "当前待确认操作：" + action.description() + "。请用户回复“确认”执行，或回复“取消”放弃。")
                .orElse("当前没有待确认的写操作。");
    }

    @Tool("为更新用户资料创建待确认操作。可以更新昵称、每日单词目标，必须等待用户下一轮确认后才能执行。")
    public String requestUpdateProfile(@P("新昵称。用户没有要求修改昵称时传空。") String nickname,
                                       @P("每日单词目标。用户没有要求修改每日目标时传空。") Integer dailyWordTarget) {
        Map<String, Object> payload = new LinkedHashMap<>();
        putIfPresent(payload, "nickname", nickname);
        putIfPresent(payload, "dailyWordTarget", dailyWordTarget);
        if (payload.isEmpty()) {
            return "需要提供新昵称或每日单词目标，才能更新用户资料。";
        }
        return savePending(PendingUserAction.ActionType.UPDATE_PROFILE, describeProfileUpdate(nickname, dailyWordTarget), payload);
    }

    @Tool("为提交错题记录创建待确认操作。适合用户明确要求把某道题记录到错题本，必须等待用户下一轮确认后才能执行。")
    public String requestSubmitWrongRecord(@P("题型，例如 word、reading、listening、manual。没有明确题型时传 manual。") String questionType,
                                           @P("关联内容 ID，没有则传空。") Long contentId,
                                           @P("关联内容标题，没有则传空。") String contentTitle,
                                           @P("错题题干或题目内容。") String questionText,
                                           @P("用户答案，没有则传空。") String userAnswer,
                                           @P("正确答案，没有则传空。") String correctAnswer,
                                           @P("模块编码，例如 cet4、cet6、kaoyan，没有则传空。") String moduleCode) {
        if (questionText == null || questionText.isBlank()) {
            return "需要提供错题题干或题目内容，才能记录到错题本。";
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        putIfPresent(payload, "questionType", questionType == null || questionType.isBlank() ? "manual" : questionType);
        putIfPresent(payload, "contentId", contentId);
        putIfPresent(payload, "contentTitle", contentTitle);
        putIfPresent(payload, "questionText", questionText);
        putIfPresent(payload, "userAnswer", userAnswer);
        putIfPresent(payload, "correctAnswer", correctAnswer);
        putIfPresent(payload, "moduleCode", moduleCode);
        return savePending(PendingUserAction.ActionType.SUBMIT_WRONG_RECORD, "记录一道错题到错题本", payload);
    }

    @Tool("列出当前用户的错题，方便用户选择要删除第几条。不要展示数据库 ID。")
    public String listWrongRecordsForDeletion() {
        List<Map<String, Object>> records = internalApiClient.getWrongRecordItems(userId);
        if (records.isEmpty()) {
            return "当前没有查到错题记录。";
        }

        StringBuilder result = new StringBuilder("当前错题如下，请用户回复“删除第几条错题”：\n");
        int limit = Math.min(records.size(), 10);
        for (int i = 0; i < limit; i++) {
            result.append(i + 1)
                    .append(". ")
                    .append(summarizeWrongRecord(records.get(i)))
                    .append("\n");
        }
        if (records.size() > limit) {
            result.append("还有 ").append(records.size() - limit).append(" 条未展示，可让用户补充关键词。");
        }
        return result.toString().trim();
    }

    @Tool("按错题本当前展示顺序为删除错题创建待确认操作。适合用户说：删除第 1 条错题、删除刚才列表第 2 条。必须等待用户下一轮确认后才能执行。")
    public String requestRemoveWrongRecordByPosition(@P("错题本当前顺序中的第几条，从 1 开始") Integer position) {
        if (position == null || position <= 0) {
            return "请说明要删除第几条错题，例如“删除第 2 条错题”。";
        }

        List<Map<String, Object>> records = internalApiClient.getWrongRecordItems(userId);
        if (records.isEmpty()) {
            return "当前没有查到错题记录，无法删除。";
        }
        if (position > records.size()) {
            return "当前只查到 " + records.size() + " 条错题，请选择 1 到 " + records.size() + " 之间的序号。";
        }

        Map<String, Object> record = records.get(position - 1);
        return requestRemoveWrongRecordRecord(record, "删除第 " + position + " 条错题");
    }

    @Tool("按关键词为删除错题创建待确认操作。适合用户说：删除 alternative 那道错题。若匹配多条，只列候选并要求用户改用第几条。")
    public String requestRemoveWrongRecordByKeyword(@P("错题中的关键词，例如单词、题干片段、答案片段") String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "请提供错题关键词，或先让系统列出错题后再说“删除第几条错题”。";
        }

        List<Map<String, Object>> records = internalApiClient.getWrongRecordItems(userId);
        if (records.isEmpty()) {
            return "当前没有查到错题记录，无法删除。";
        }

        String normalizedKeyword = keyword.toLowerCase().trim();
        Map<String, Object> matchedRecord = null;
        int matchedPosition = -1;
        int matchedCount = 0;
        StringBuilder candidates = new StringBuilder();
        for (int i = 0; i < records.size(); i++) {
            Map<String, Object> record = records.get(i);
            if (!matchesWrongRecord(record, normalizedKeyword)) {
                continue;
            }

            matchedCount++;
            if (matchedRecord == null) {
                matchedRecord = record;
                matchedPosition = i + 1;
            }
            if (matchedCount <= 5) {
                candidates.append("第 ")
                        .append(i + 1)
                        .append(" 条：")
                        .append(summarizeWrongRecord(record))
                        .append("\n");
            }
        }

        if (matchedCount == 0) {
            return "没有找到包含“" + keyword.trim() + "”的错题。可以先说“我的错题”，再说“删除第几条错题”。";
        }
        if (matchedCount > 1) {
            return "匹配到 " + matchedCount + " 条错题，暂不创建删除操作。请用户明确回复要删除哪一条，例如“删除第 "
                    + matchedPosition + " 条错题”。\n" + candidates.toString().trim();
        }

        return requestRemoveWrongRecordRecord(matchedRecord, "删除包含“" + keyword.trim() + "”的错题");
    }

    @Tool("为删除错题记录创建待确认操作。仅当用户明确提供错题记录 ID 时使用；普通用户更适合使用按第几条或关键词删除。必须等待用户下一轮确认后才能执行。")
    public String requestRemoveWrongRecord(@P("要删除的错题记录 ID") Long wrongRecordId) {
        String error = validatePositiveId(wrongRecordId, "错题记录 ID");
        if (error != null) {
            return error;
        }
        return savePending(
                PendingUserAction.ActionType.DELETE_WRONG_RECORD,
                "删除错题记录 " + wrongRecordId,
                Map.of("wrongRecordId", wrongRecordId)
        );
    }

    @Tool("为把单词标记为已认识创建待确认操作。必须等待用户下一轮确认后才能执行。")
    public String requestMarkWordKnown(@P("要标记认识的单词 ID") Long wordId) {
        String error = validatePositiveId(wordId, "单词 ID");
        if (error != null) {
            return error;
        }
        return savePending(
                PendingUserAction.ActionType.MARK_WORD_KNOWN,
                "标记单词 " + wordId + " 为已认识",
                Map.of("wordId", wordId)
        );
    }

    @Tool("为重置单词掌握进度创建待确认操作。必须等待用户下一轮确认后才能执行。")
    public String requestResetWordProgress(@P("要重置进度的单词 ID") Long wordId) {
        String error = validatePositiveId(wordId, "单词 ID");
        if (error != null) {
            return error;
        }
        return savePending(
                PendingUserAction.ActionType.RESET_WORD_PROGRESS,
                "重置单词 " + wordId + " 的掌握进度",
                Map.of("wordId", wordId)
        );
    }

    @Tool("为收藏阅读文章创建待确认操作。必须等待用户下一轮确认后才能执行。")
    public String requestAddReadingFavorite(@P("要收藏的阅读文章 ID") Long readingId) {
        String error = validatePositiveId(readingId, "阅读文章 ID");
        if (error != null) {
            return error;
        }
        return savePending(
                PendingUserAction.ActionType.ADD_READING_FAVORITE,
                "收藏阅读文章 " + readingId,
                Map.of("readingId", readingId)
        );
    }

    @Tool("为取消收藏阅读文章创建待确认操作。必须等待用户下一轮确认后才能执行。")
    public String requestRemoveReadingFavorite(@P("要取消收藏的阅读文章 ID") Long readingId) {
        String error = validatePositiveId(readingId, "阅读文章 ID");
        if (error != null) {
            return error;
        }
        return savePending(
                PendingUserAction.ActionType.REMOVE_READING_FAVORITE,
                "取消收藏阅读文章 " + readingId,
                Map.of("readingId", readingId)
        );
    }

    @Tool("确认并执行当前对话中上一轮创建的待确认写操作。只有用户本轮明确回复确认、执行、yes 时才能调用。")
    public String confirmPendingAction() {
        if (createdPendingActionInThisTurn) {
            return "本轮刚创建待确认操作，不能在同一轮执行。请等待用户下一条消息明确回复“确认”。";
        }
        return pendingActionStore.consume(userId, sessionId)
                .map(this::execute)
                .orElse("当前没有待确认的写操作，无法执行。");
    }

    @Tool("取消当前对话中上一轮创建的待确认写操作。只有用户本轮明确回复取消、算了、不用了时才能调用。")
    public String cancelPendingAction() {
        if (createdPendingActionInThisTurn) {
            return "本轮刚创建待确认操作。如果用户想放弃，请让用户下一条消息回复“取消”。";
        }
        boolean removed = pendingActionStore.clear(userId, sessionId);
        return removed ? "已取消当前待确认的写操作。" : "当前没有待确认的写操作。";
    }

    @Tool("说明商城下单、购买、支付类请求当前不能由 AI 直接执行，必须到页面确认。")
    public String rejectShopWriteAction(@P("用户想执行的商城操作") String actionDescription) {
        String action = actionDescription == null || actionDescription.isBlank() ? "下单、购买或支付" : actionDescription.trim();
        return action + "涉及订单或资金流程，当前不能由 AI 对话直接执行。请引导用户到商城页面手动确认。";
    }

    private String savePending(PendingUserAction.ActionType type, String description, Map<String, Object> payload) {
        if (userId == null) {
            return "未登录，无法创建待确认操作。";
        }
        PendingUserAction action = pendingActionStore.save(userId, sessionId, type, description, payload);
        createdPendingActionInThisTurn = true;
        return "已创建待确认操作：" + action.description() + "。请用户回复“确认”执行，或回复“取消”放弃。";
    }

    private String requestRemoveWrongRecordRecord(Map<String, Object> record, String actionText) {
        Long wrongRecordId = longField(record, "id");
        if (wrongRecordId == null) {
            return "找到了对应错题，但记录缺少 ID，无法删除。";
        }
        return savePending(
                PendingUserAction.ActionType.DELETE_WRONG_RECORD,
                actionText + "：" + summarizeWrongRecord(record),
                Map.of("wrongRecordId", wrongRecordId)
        );
    }

    private String execute(PendingUserAction action) {
        String result = switch (action.type()) {
            case UPDATE_PROFILE -> internalApiClient.updateProfile(
                    action.userId(),
                    action.stringValue("nickname"),
                    action.integerValue("dailyWordTarget")
            );
            case SUBMIT_WRONG_RECORD -> internalApiClient.submitWrongRecord(
                    action.userId(),
                    action.stringValue("questionType"),
                    action.longValue("contentId"),
                    action.stringValue("contentTitle"),
                    action.stringValue("questionText"),
                    action.stringValue("userAnswer"),
                    action.stringValue("correctAnswer"),
                    action.stringValue("moduleCode")
            );
            case DELETE_WRONG_RECORD -> internalApiClient.removeWrongRecord(action.userId(), action.longValue("wrongRecordId"));
            case MARK_WORD_KNOWN -> internalApiClient.markWordKnown(action.userId(), action.longValue("wordId"));
            case RESET_WORD_PROGRESS -> internalApiClient.resetWordProgress(action.userId(), action.longValue("wordId"));
            case ADD_READING_FAVORITE -> internalApiClient.addReadingFavorite(action.userId(), action.longValue("readingId"));
            case REMOVE_READING_FAVORITE -> internalApiClient.removeReadingFavorite(action.userId(), action.longValue("readingId"));
        };
        return "已执行待确认操作：" + action.description() + "\n执行结果：" + result;
    }

    private String describeProfileUpdate(String nickname, Integer dailyWordTarget) {
        if (nickname != null && !nickname.isBlank() && dailyWordTarget != null) {
            return "更新昵称为“" + nickname.trim() + "”，每日单词目标为 " + dailyWordTarget;
        }
        if (nickname != null && !nickname.isBlank()) {
            return "更新昵称为“" + nickname.trim() + "”";
        }
        return "更新每日单词目标为 " + dailyWordTarget;
    }

    private String validatePositiveId(Long value, String name) {
        if (value == null) {
            return name + "不能为空。";
        }
        if (value <= 0) {
            return name + "必须是正数。";
        }
        return null;
    }

    private boolean matchesWrongRecord(Map<String, Object> record, String normalizedKeyword) {
        return containsIgnoreCase(record, "contentTitle", normalizedKeyword)
                || containsIgnoreCase(record, "questionText", normalizedKeyword)
                || containsIgnoreCase(record, "userAnswer", normalizedKeyword)
                || containsIgnoreCase(record, "correctAnswer", normalizedKeyword)
                || containsIgnoreCase(record, "moduleCode", normalizedKeyword)
                || containsIgnoreCase(record, "questionType", normalizedKeyword);
    }

    private boolean containsIgnoreCase(Map<String, Object> record, String key, String normalizedKeyword) {
        String value = textField(record, key);
        return value != null && value.toLowerCase().contains(normalizedKeyword);
    }

    private Long longField(Map<String, Object> record, String key) {
        Object value = record.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.valueOf(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String summarizeWrongRecord(Map<String, Object> record) {
        String title = textField(record, "contentTitle");
        String question = textField(record, "questionText");
        String userAnswer = textField(record, "userAnswer");
        String correctAnswer = textField(record, "correctAnswer");

        StringBuilder summary = new StringBuilder();
        if (title != null) {
            summary.append(title);
        }
        if (question != null) {
            appendPart(summary, question);
        }
        if (userAnswer != null || correctAnswer != null) {
            appendPart(summary, "你的答案：" + valueOrDash(userAnswer) + "，正确答案：" + valueOrDash(correctAnswer));
        }
        if (summary.isEmpty()) {
            summary.append("未命名错题");
        }
        return clip(summary.toString(), 160);
    }

    private String textField(Map<String, Object> record, String key) {
        Object value = record.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).replaceAll("\\s+", " ").trim();
        return text.isBlank() ? null : text;
    }

    private void appendPart(StringBuilder builder, String value) {
        if (builder.length() > 0) {
            builder.append("；");
        }
        builder.append(value);
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String clip(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private void putIfPresent(Map<String, Object> payload, String key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String text) {
            if (text.isBlank()) {
                return;
            }
            payload.put(key, text.trim());
            return;
        }
        payload.put(key, value);
    }
}
