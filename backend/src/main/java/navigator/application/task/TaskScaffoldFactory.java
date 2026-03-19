package navigator.application.task;

import navigator.domain.enums.FeedbackStyle;
import navigator.domain.enums.ScaffoldIntensity;
import navigator.domain.model.TaskBlueprint;
import navigator.domain.model.LearnerStrategyProfile;
import navigator.domain.model.TaskScaffold;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 由 TaskBlueprint 生成执行期脚手架（规则生成，不调用 LLM）。
 */
public final class TaskScaffoldFactory {

    private TaskScaffoldFactory() {}

    public static TaskScaffold build(String sessionId, TaskBlueprint bp) {
        return build(sessionId, bp, null);
    }

    public static TaskScaffold build(String sessionId, TaskBlueprint bp, LearnerStrategyProfile strategyProfile) {
        String goal = bp.getGoal() != null ? bp.getGoal() : bp.getTitle();
        String prompt = bp.getRecommendedPromptTemplate() != null
                ? bp.getRecommendedPromptTemplate()
                : (bp.getPromptScaffold() != null ? bp.getPromptScaffold() : "请围绕任务目标向导师提问。");
        List<String> ask = new ArrayList<>();
        ask.add(prompt);
        ask.add("请用最小例子说明：" + truncate(goal, 40));
        List<String> follow = new ArrayList<>();
        follow.add("和上一概念相比，最大的差异是什么？");
        follow.add("如果去掉其中一个条件，结论还成立吗？");
        List<String> selfCheck = new ArrayList<>();
        if (bp.getSelfEvaluationQuestions() != null) {
            selfCheck.addAll(bp.getSelfEvaluationQuestions());
        }
        selfCheck.add("我这样理解对吗：" + truncate(goal, 30) + "……");
        List<String> fallback = new ArrayList<>();
        if (bp.getFallbackAction() != null) {
            fallback.add(bp.getFallbackAction());
        }
        fallback.add("先只问一个更小的子问题，再逐步扩展。");
        fallback.add("用自己的话复述当前卡住的点，再请导师针对该点解释。");
        List<String> completion = new ArrayList<>();
        if (bp.getCompletionCriteria() != null) {
            completion.addAll(bp.getCompletionCriteria());
        }
        if (completion.isEmpty()) {
            completion.add("能独立复述本任务的核心要点");
        }
        List<String> anti = new ArrayList<>(List.of(
                "不要让导师一次性讲完整章",
                "不要只复制定义而不举例",
                "不要跳过自我复述直接要答案"
        ));
        if (strategyProfile != null && strategyProfile.getFeedbackStyle() == FeedbackStyle.DIRECT) {
            anti.add("不要期待导师反复追问，可主动精简提问");
        }

        int suggestedExploreTurns = 2;
        int suggestedCheckpointCount = 1;
        if (strategyProfile != null && strategyProfile.getScaffoldIntensity() != null) {
            ScaffoldIntensity intensity = strategyProfile.getScaffoldIntensity();
            if (intensity == ScaffoldIntensity.LIGHT) {
                suggestedExploreTurns = 1;
                suggestedCheckpointCount = 1;
            } else if (intensity == ScaffoldIntensity.STRICT) {
                suggestedExploreTurns = 3;
                suggestedCheckpointCount = 2;
            }
        }

        return TaskScaffold.builder()
                .scaffoldId(TaskExecutionRuntime.newScaffoldId())
                .taskId(bp.getTaskId())
                .sessionId(sessionId)
                .taskType(bp.getTaskType() != null ? bp.getTaskType().name() : "CONCEPT_EXPLAIN")
                .learningObjective(goal)
                .whyThisTask(bp.getTaskMethod() != null ? bp.getTaskMethod() : "本任务是计划中的一环，完成后便于进入下一步。")
                .recommendedAskTemplates(ask)
                .recommendedFollowupTemplates(follow)
                .selfCheckTemplates(selfCheck)
                .fallbackHints(fallback)
                .completionSignals(completion)
                .antiPatterns(anti)
                .currentExecutionState("ORIENT")
                .suggestedExploreTurns(suggestedExploreTurns)
                .suggestedCheckpointCount(suggestedCheckpointCount)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
