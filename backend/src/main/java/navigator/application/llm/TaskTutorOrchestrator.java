package navigator.application.llm;

import navigator.application.task.TaskExecutionRuntime;
import navigator.domain.enums.LearningActionType;
import navigator.domain.model.TaskBlueprint;
import navigator.domain.model.TutorTurnResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 受控导师编排：按状态裁剪回复与推荐，不代替状态机做最终迁移。
 */
@Component
public class TaskTutorOrchestrator {

    private final MockLlmGateway mockLlmGateway;
    private final OpenAiCompatibleLlmGateway openAiCompatibleLlmGateway;
    private final LlmProperties llmProperties;

    public TaskTutorOrchestrator(MockLlmGateway mockLlmGateway,
                                 OpenAiCompatibleLlmGateway openAiCompatibleLlmGateway,
                                 LlmProperties llmProperties) {
        this.mockLlmGateway = mockLlmGateway;
        this.openAiCompatibleLlmGateway = openAiCompatibleLlmGateway;
        this.llmProperties = llmProperties;
    }

    public TutorTurnResult exploreTurn(TaskExecutionRuntime rt, TaskBlueprint bp, String userInput, LearningActionType action) {
        String goal = bp.getGoal() != null ? bp.getGoal() : "";
        String completion = bp.getCompletionCriteria() != null && !bp.getCompletionCriteria().isEmpty()
                ? String.join(" | ", bp.getCompletionCriteria().stream().limit(5).toList())
                : "";
        String system = "PHASE=EXECUTION/EXPLORE\n" +
                "GOAL=" + goal + "\n" +
                (completion.isBlank() ? "" : ("COMPLETION_CRITERIA=" + completion + "\n")) +
                "BOUNDARY=explain_with_min_example,ask_guiding_questions,no_long_lecture\n" +
                "OUTPUT=plain_text";
        LlmCall call = callLlm(system, userInput);
        String reply = call.reply;
        String fallbackMode = call.fallbackMode;
        List<String> prompts = new ArrayList<>();
        if (rt.getScaffold() != null && rt.getScaffold().getRecommendedFollowupTemplates() != null) {
            prompts.addAll(rt.getScaffold().getRecommendedFollowupTemplates());
        }
        prompts.add("我这样理解对吗：……");
        if (action == LearningActionType.SEEK_DIRECT_ANSWER) {
            reply = "直接给答案不利于形成自己的理解。请先试着用一句话说出你卡住的点，我们再从最小例子切入。";
            fallbackMode = "TEMPLATE";
        }
        if (action == LearningActionType.OFF_TOPIC) {
            reply = "我们先回到当前任务：" + truncate(goal, 60) + "。你可以从上面的推荐问法里选一句开始。";
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

    private record LlmCall(String reply, String fallbackMode) {
    }

    private LlmCall callLlm(String system, String userInput) {
        if (llmProperties != null && llmProperties.isEnabled()) {
            try {
                return new LlmCall(openAiCompatibleLlmGateway.generateReply(system, userInput), "NONE");
            } catch (Exception ex) {
                return new LlmCall(mockLlmGateway.generateReply(system, userInput), "MOCK");
            }
        }
        return new LlmCall(mockLlmGateway.generateReply(system, userInput), "MOCK");
    }
}
