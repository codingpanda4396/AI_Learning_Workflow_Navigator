package navigator.application.llm;

import navigator.application.task.TaskExecutionContext;
import navigator.application.task.TaskExecutionRuntime;
import navigator.domain.enums.FeedbackStyle;
import navigator.domain.enums.GuidanceIntent;
import navigator.domain.enums.LearningActionType;
import navigator.domain.enums.TimeBudget;
import navigator.domain.model.GuidanceDecision;
import jakarta.annotation.PostConstruct;
import navigator.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 受控导师编排：按状态裁剪回复与推荐，不代替状态机做最终迁移。
 */
@Component
public class TaskTutorOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TaskTutorOrchestrator.class);

    private final MockLlmGateway mockLlmGateway;
    private final OpenAiCompatibleLlmGateway openAiCompatibleLlmGateway;
    private final LlmProperties llmProperties;
    private final TutorPromptBuilder tutorPromptBuilder;

    public TaskTutorOrchestrator(MockLlmGateway mockLlmGateway,
                                 OpenAiCompatibleLlmGateway openAiCompatibleLlmGateway,
                                 LlmProperties llmProperties,
                                 TutorPromptBuilder tutorPromptBuilder) {
        this.mockLlmGateway = mockLlmGateway;
        this.openAiCompatibleLlmGateway = openAiCompatibleLlmGateway;
        this.llmProperties = llmProperties;
        this.tutorPromptBuilder = tutorPromptBuilder;
    }

    @PostConstruct
    void logLlmBindingSummary() {
        if (llmProperties == null) {
            return;
        }
        boolean keyOk = llmProperties.getApiKey() != null && !llmProperties.getApiKey().isBlank();
        log.info("navigator.llm: enabled={} baseUrl={} model={} apiKeyConfigured={} timeoutMs={}",
                llmProperties.isEnabled(),
                llmProperties.getBaseUrl(),
                llmProperties.getModel(),
                keyOk,
                llmProperties.getTimeoutMs());
    }

    public TutorTurnResult exploreTurn(TaskExecutionContext ctx, TaskExecutionRuntime rt, String userInput,
                                       LearningActionType action, GuidanceDecision guidanceDecision) {
        String goal = resolveGoal(ctx);
        String completion = resolveCompletionCriteria(ctx);
        String boundary = resolveBoundary(ctx);
        String focus = resolveExplanationFocus(ctx);
        String topics = resolveTopics(ctx);
        String timeConstraint = resolveTimeConstraint(ctx);

        GuidanceDecision gd = guidanceDecision != null ? guidanceDecision : GuidanceDecision.builder().build();
        if (gd.getPhase() == null && rt.getGuidancePhase() != null) {
            gd.setPhase(rt.getGuidancePhase());
        }
        if (gd.getIntent() == null) {
            gd.setIntent(GuidanceIntent.ASK_CLARIFYING_QUESTION);
        }
        String baseSystem = tutorPromptBuilder.buildExploreSystemPrompt(ctx, rt, gd, goal, completion, boundary);
        String system = baseSystem
                + (focus.isBlank() ? "" : "EXPLANATION_FOCUS=" + focus + "\n")
                + (topics.isBlank() ? "" : "TOPICS=" + topics + "\n")
                + (timeConstraint.isBlank() ? "" : "TIME_CONSTRAINT=" + timeConstraint + "\n");

        String forced = gd.getPromptSlots() != null ? gd.getPromptSlots().get("forced_reply") : null;
        LlmCall call;
        if (forced != null && !forced.isBlank()) {
            call = new LlmCall(forced, "TEMPLATE");
        } else if (gd.getIntent() == GuidanceIntent.REDIRECT_OFF_TASK && action == LearningActionType.OFF_TOPIC) {
            call = new LlmCall("我们先回到当前任务：" + truncate(goal, 60) + "。你可以从上面的推荐问法里选一句开始。", "TEMPLATE");
        } else {
            call = callLlm(system, userInput);
        }

        String reply = call.reply;
        String fallbackMode = call.fallbackMode;
        List<String> prompts = new ArrayList<>();
        if (rt.getScaffold() != null && rt.getScaffold().getRecommendedFollowupTemplates() != null) {
            prompts.addAll(rt.getScaffold().getRecommendedFollowupTemplates());
        }
        prompts.add("我这样理解对吗：……");
        if (!gd.isAllowSubstantiveAnswer() && looksLikeCompleteSolutionLeak(reply)) {
            reply = "为避免代替你完成整题，我只给方向：先说出已知条件与目标，再尝试一步推理。你现在卡在哪一小步？";
            fallbackMode = "TEMPLATE";
        }
        return TutorTurnResult.builder()
                .assistantReply(reply)
                .detectedAction(action)
                .semanticSummary(userInput != null ? truncate(userInput.trim(), 120) : null)
                .suggestedFollowups(prompts)
                .explanationDraft(null)
                .evidenceExtracts(List.of())
                .fallbackMode(fallbackMode)
                .build();
    }

    private String resolveGoal(TaskExecutionContext ctx) {
        if (ctx == null) return "";
        if (ctx.getExecutableTaskSpec() != null && ctx.getExecutableTaskSpec().getTitle() != null) {
            return ctx.getExecutableTaskSpec().getTitle();
        }
        if (ctx.getStructuredGoal() != null && ctx.getStructuredGoal().getTopics() != null && !ctx.getStructuredGoal().getTopics().isEmpty()) {
            return String.join("、", ctx.getStructuredGoal().getTopics());
        }
        return "";
    }

    private String resolveCompletionCriteria(TaskExecutionContext ctx) {
        if (ctx != null && ctx.getExecutableTaskSpec() != null && ctx.getExecutableTaskSpec().getCompletionCriteria() != null
                && !ctx.getExecutableTaskSpec().getCompletionCriteria().isEmpty()) {
            return String.join(" | ", ctx.getExecutableTaskSpec().getCompletionCriteria().stream().limit(5).toList());
        }
        return "";
    }

    private String resolveBoundary(TaskExecutionContext ctx) {
        FeedbackStyle style = ctx != null && ctx.getLearnerStrategyProfile() != null
                ? ctx.getLearnerStrategyProfile().getFeedbackStyle() : null;
        if (style == FeedbackStyle.DIRECT) {
            return "brief_answer,one_concept,no_long_lecture";
        }
        return "explain_with_min_example,ask_guiding_questions,no_long_lecture";
    }

    private String resolveExplanationFocus(TaskExecutionContext ctx) {
        if (ctx == null || ctx.getGoalContextSnapshot() == null || ctx.getGoalContextSnapshot().getExplanationFocus() == null) {
            return "";
        }
        return String.join("; ", ctx.getGoalContextSnapshot().getExplanationFocus());
    }

    private String resolveTopics(TaskExecutionContext ctx) {
        if (ctx == null || ctx.getStructuredGoal() == null || ctx.getStructuredGoal().getTopics() == null) {
            return "";
        }
        return String.join("、", ctx.getStructuredGoal().getTopics());
    }

    private String resolveTimeConstraint(TaskExecutionContext ctx) {
        if (ctx == null || ctx.getStructuredGoal() == null || ctx.getStructuredGoal().getTimeBudget() == null) {
            return "";
        }
        TimeBudget budget = ctx.getStructuredGoal().getTimeBudget();
        if (budget == TimeBudget.WITHIN_15_MIN || budget == TimeBudget.WITHIN_30_MIN) {
            return "精简解释，优先最小例子";
        }
        return "";
    }

    public TutorTurnResult remedialTurn(TaskBlueprint bp, String userInput) {
        String system = "PHASE=EXECUTION/REMEDIAL\n" +
                "BOUNDARY=minimal_correction,one_small_step,no_long_text\n" +
                "OUTPUT=plain_text";
        LlmCall call = callLlm(system, userInput);
        return TutorTurnResult.builder()
                .assistantReply(call.reply)
                .detectedAction(null)
                .semanticSummary(userInput != null ? truncate(userInput.trim(), 120) : null)
                .suggestedFollowups(List.of("请再用自己的话复述刚才的纠偏点"))
                .explanationDraft(null)
                .evidenceExtracts(List.of())
                .fallbackMode(call.fallbackMode)
                .build();
    }

    private static String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }

    /** 轻量启发式：疑似完整解答泄题时降级 */
    private static boolean looksLikeCompleteSolutionLeak(String reply) {
        if (reply == null || reply.length() < 80) {
            return false;
        }
        String t = reply;
        boolean manySteps = t.split("\\n\\s*\\d+\\.").length >= 4;
        boolean answerPhrase = t.contains("因此答案") || t.contains("最终答案") || t.contains("答案是");
        return manySteps || (answerPhrase && reply.length() > 200);
    }

    private record LlmCall(String reply, String fallbackMode) {
    }

    private LlmCall callLlm(String system, String userInput) {
        if (llmProperties != null && llmProperties.isEnabled()) {
            int sysLen = system != null ? system.length() : 0;
            int userLen = userInput != null ? userInput.length() : 0;
            log.debug("LLM chat.completions: invoking provider baseUrl={} model={} systemChars={} userChars={}",
                    llmProperties.getBaseUrl(), llmProperties.getModel(), sysLen, userLen);
            try {
                String reply = openAiCompatibleLlmGateway.generateReply(system, userInput);
                log.debug("LLM chat.completions: success fallbackMode=NONE replyChars={}",
                        reply != null ? reply.length() : 0);
                return new LlmCall(reply, "NONE");
            } catch (Exception ex) {
                log.warn("LLM chat.completions: provider failed, using MOCK fallback — {}: {}",
                        ex.getClass().getSimpleName(), ex.getMessage());
                log.debug("LLM chat.completions: provider failure detail", ex);
                return new LlmCall(mockLlmGateway.generateReply(system, userInput), "MOCK");
            }
        }
        log.debug("LLM chat.completions: navigator.llm.enabled=false, using MOCK (no HTTP call)");
        return new LlmCall(mockLlmGateway.generateReply(system, userInput), "MOCK");
    }
}
