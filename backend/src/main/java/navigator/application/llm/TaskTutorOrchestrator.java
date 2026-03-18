package navigator.application.llm;

import navigator.application.task.TaskExecutionRuntime;
import navigator.domain.enums.LearningActionType;
import navigator.domain.model.TaskBlueprint;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 受控导师编排：按状态裁剪回复与推荐，不代替状态机做最终迁移。
 */
@Component
public class TaskTutorOrchestrator {

    private final LlmGateway llmGateway;

    public TaskTutorOrchestrator(LlmGateway llmGateway) {
        this.llmGateway = llmGateway;
    }

    public TutorTurnResult exploreTurn(TaskExecutionRuntime rt, TaskBlueprint bp, String userInput, LearningActionType action) {
        String goal = bp.getGoal() != null ? bp.getGoal() : "";
        String system = "EXPLORE 态：可解释、可举最小例、可引导追问；禁止展开整章；任务目标：" + goal;
        String reply = llmGateway.generateReply(system, userInput);
        List<String> prompts = new ArrayList<>();
        if (rt.getScaffold() != null && rt.getScaffold().getRecommendedFollowupTemplates() != null) {
            prompts.addAll(rt.getScaffold().getRecommendedFollowupTemplates());
        }
        prompts.add("我这样理解对吗：……");
        String recommend = rt.getExploreTurnCount() >= (rt.getScaffold() != null && rt.getScaffold().getSuggestedExploreTurns() != null
                ? rt.getScaffold().getSuggestedExploreTurns() - 1 : 1)
                ? "MOVE_TO_SELF_EXPLAIN" : "KEEP";
        if (action == LearningActionType.SEEK_DIRECT_ANSWER) {
            reply = "直接给答案不利于形成自己的理解。请先试着用一句话说出你卡住的点，我们再从最小例子切入。";
            recommend = "KEEP";
        }
        if (action == LearningActionType.OFF_TOPIC) {
            reply = "我们先回到当前任务：" + truncate(goal, 60) + "。你可以从上面的推荐问法里选一句开始。";
        }
        return new TutorTurnResult(reply, prompts, recommend, "NONE");
    }

    public TutorTurnResult remedialTurn(TaskBlueprint bp, String userInput) {
        String system = "REMEDIAL：只做最小纠偏，给一个小台阶，不要求长文";
        return new TutorTurnResult(
                llmGateway.generateReply(system, userInput),
                List.of("请再用自己的话复述刚才的纠偏点"),
                "KEEP",
                "NONE");
    }

    private static String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }

    public record TutorTurnResult(
            String assistantReply,
            List<String> suggestedNextPrompts,
            String stateRecommendation,
            String fallbackMode
    ) {}
}
