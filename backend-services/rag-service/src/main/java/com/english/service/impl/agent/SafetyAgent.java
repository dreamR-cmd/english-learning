package com.english.service.impl.agent;

import com.english.dto.RagAnswerResponse;
import com.english.service.RagService;
import com.english.service.impl.RagInternalApiClient;
import com.english.service.impl.tools.UserActionTools;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class SafetyAgent implements SpecialistAgent {
    private static final Set<String> CONFIRM_MESSAGES = Set.of("确认", "确认执行", "执行", "可以", "可以执行", "好的", "好", "yes", "y");
    private static final Set<String> CANCEL_MESSAGES = Set.of("取消", "取消操作", "算了", "不用了", "先不用", "先不");

    private static final String SYSTEM_MESSAGE = """
            你是安全执行 Agent，负责处理删除、修改、标记、提交、收藏、取消收藏等可能改变用户数据的请求。
            规则：
            1. 只允许调用工具执行白名单操作：更新资料、提交错题、删除错题、标记单词认识、重置单词进度、收藏/取消收藏阅读文章。
            2. 用户首次提出白名单写操作时，只调用 request* 工具创建待确认操作，然后让用户回复“确认”或“取消”。
            3. 创建待确认操作后，本轮不要继续调用 confirmPendingAction。
            4. 只有用户当前消息明确是在确认上一轮待确认操作时，才调用 confirmPendingAction。
            5. 只有用户当前消息明确是在取消上一轮待确认操作时，才调用 cancelPendingAction。
            6. 删除错题时不要要求普通用户提供数据库 ID；用户说“删除第几条”就调用 requestRemoveWrongRecordByPosition，用户说“删除包含某关键词的错题”就调用 requestRemoveWrongRecordByKeyword。
            7. 如果用户只说“删除错题”但没有说明哪一条，调用 listWrongRecordsForDeletion，让用户按序号选择；不要展示数据库 ID。
            8. 购买、下单、支付、退款等商城或资金操作不能由 AI 直接执行，调用 rejectShopWriteAction 并引导用户到页面确认。
            9. 参数缺失时不要猜 ID，要求用户补充必要信息。
            10. 回答简短、明确，不要原样展示 JSON。
            """;

    private final SpecialistAgentRunner runner;
    private final RagInternalApiClient internalApiClient;
    private final PendingUserActionStore pendingActionStore;

    public SafetyAgent(SpecialistAgentRunner runner,
                       RagInternalApiClient internalApiClient,
                       PendingUserActionStore pendingActionStore) {
        this.runner = runner;
        this.internalApiClient = internalApiClient;
        this.pendingActionStore = pendingActionStore;
    }

    @Override
    public RagAnswerResponse ask(AgentRequestContext context) {
        UserActionTools tools = buildTools(context);
        String directAnswer = handleDirectConfirmation(context.question(), tools);
        if (directAnswer != null) {
            return new RagAnswerResponse(directAnswer, List.of());
        }

        return runner.ask(
                SYSTEM_MESSAGE,
                buildUserMessage(context.question()),
                context.sessionId(),
                context.chatMemoryProvider(),
                List.of(tools),
                () -> List.of()
        );
    }

    @Override
    public void askStream(AgentRequestContext context, RagService.RagStreamHandler handler, Runnable fallback) {
        UserActionTools tools = buildTools(context);
        String directAnswer = handleDirectConfirmation(context.question(), tools);
        if (directAnswer != null) {
            handler.onToken(directAnswer);
            handler.onReferences(List.of());
            handler.onComplete();
            return;
        }

        runner.askStream(
                SYSTEM_MESSAGE,
                buildUserMessage(context.question()),
                context.sessionId(),
                context.chatMemoryProvider(),
                List.of(tools),
                () -> List.of(),
                handler,
                fallback
        );
    }

    private UserActionTools buildTools(AgentRequestContext context) {
        return new UserActionTools(context.userId(), context.sessionId(), internalApiClient, pendingActionStore);
    }

    private String handleDirectConfirmation(String question, UserActionTools tools) {
        String normalized = normalize(question);
        if (CONFIRM_MESSAGES.contains(normalized)) {
            return tools.confirmPendingAction();
        }
        if (CANCEL_MESSAGES.contains(normalized)) {
            return tools.cancelPendingAction();
        }
        return null;
    }

    private String buildUserMessage(String question) {
        return "用户请求：\n" + (question == null ? "" : question.trim())
                + "\n\n请判断是否为白名单写操作。首次写操作只创建待确认操作；确认/取消上一轮操作时再调用对应工具。";
    }

    private String normalize(String question) {
        return question == null ? "" : question.toLowerCase(Locale.ROOT).trim();
    }
}
