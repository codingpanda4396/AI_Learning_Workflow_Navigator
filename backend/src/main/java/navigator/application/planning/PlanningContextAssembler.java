package navigator.application.planning;

import navigator.domain.enums.EntryGranularity;
import navigator.domain.enums.TimeBudget;
import navigator.domain.model.DiagnosisEvidenceSummary;
import navigator.domain.model.GoalContextSnapshot;
import navigator.domain.model.LearnerProfileSnapshot;
import navigator.domain.model.LearnerStrategyProfile;
import navigator.domain.model.StructuredLearningGoal;
import navigator.domain.model.TimeBudgetConstraint;
import navigator.infrastructure.memory.InMemoryStore;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Sprint 1: 从 store 按 goalId、diagnosisId 组装 PlanningContext。
 */
@Component
public class PlanningContextAssembler {

    private final InMemoryStore store;

    public PlanningContextAssembler(InMemoryStore store) {
        this.store = store;
    }

    public PlanningContext assemble(String goalId, String diagnosisId) {
        StructuredLearningGoal goal = store.getGoals().get(goalId);
        GoalContextSnapshot goalContextSnapshot = store.getGoalContextSnapshots().get(goalId);
        LearnerProfileSnapshot learnerProfileSnapshot = store.getLearnerProfiles().get(diagnosisId);
        LearnerStrategyProfile learnerStrategyProfile = store.getLearnerStrategyProfiles().get(diagnosisId);
        DiagnosisEvidenceSummary diagnosisEvidenceSummary = store.getDiagnosisEvidenceSummaries().get(diagnosisId);
        TimeBudgetConstraint timeBudgetConstraint = deriveTimeBudgetConstraint(goal, goalContextSnapshot);
        return PlanningContext.builder()
                .goal(goal)
                .goalContextSnapshot(goalContextSnapshot)
                .learnerProfileSnapshot(learnerProfileSnapshot)
                .learnerStrategyProfile(learnerStrategyProfile)
                .diagnosisEvidenceSummary(diagnosisEvidenceSummary)
                .timeBudgetConstraint(timeBudgetConstraint)
                .build();
    }

    private TimeBudgetConstraint deriveTimeBudgetConstraint(StructuredLearningGoal goal, GoalContextSnapshot goalContextSnapshot) {
        TimeBudget budget = goal != null && goal.getTimeBudget() != null ? goal.getTimeBudget() : TimeBudget.WITHIN_60_MIN;
        int totalCap = totalMinutesCapFor(budget);
        EntryGranularity granularity = goalContextSnapshot != null && goalContextSnapshot.getEntryGranularity() != null
                ? goalContextSnapshot.getEntryGranularity() : EntryGranularity.SMALL;
        int minTasks = 1;
        int maxTasks = maxTasksFor(budget, granularity);
        if (matchesArrayVsLinkedListShowcaseGoal(goal)
                || matchesBinaryTreeBasicShowcaseGoal(goal)
                || matchesDfsVsBfsShowcaseGoal(goal)) {
            maxTasks = Math.max(maxTasks, 4);
        }
        return TimeBudgetConstraint.builder()
                .timeBudget(budget)
                .totalMinutesCap(totalCap)
                .minTasks(minTasks)
                .maxTasks(maxTasks)
                .build();
    }

    private int totalMinutesCapFor(TimeBudget budget) {
        return switch (budget) {
            case WITHIN_15_MIN -> 15;
            case WITHIN_30_MIN -> 30;
            case WITHIN_60_MIN -> 60;
            case MULTI_DAY, LONG_TERM -> -1;
            default -> 60;
        };
    }

    private int maxTasksFor(TimeBudget budget, EntryGranularity granularity) {
        if (budget == TimeBudget.WITHIN_15_MIN) {
            return granularity == EntryGranularity.MICRO ? 2 : 2;
        }
        if (budget == TimeBudget.WITHIN_30_MIN) {
            return granularity == EntryGranularity.MICRO ? 2 : 3;
        }
        if (budget == TimeBudget.WITHIN_60_MIN) {
            return 5;
        }
        return 8;
    }

    /**
     * 与前端演示知识点「顺序表 vs 链表」判定一致：保证该场景下任务不被压到 3 个以下。
     */
    private static boolean matchesArrayVsLinkedListShowcaseGoal(StructuredLearningGoal goal) {
        if (goal == null) {
            return false;
        }
        StringBuilder sb = new StringBuilder();
        append(sb, goal.getRawGoalText());
        append(sb, goal.getNormalizedGoalText());
        append(sb, goal.getIntentDescription());
        append(sb, goal.getSubject());
        append(sb, goal.getSourceContext());
        append(sb, goal.getPriorityModule());
        List<String> topics = goal.getTopics();
        if (topics != null) {
            for (String t : topics) {
                append(sb, t);
            }
        }
        String corpus = sb.toString();
        if (!StringUtils.hasText(corpus)) {
            return false;
        }
        boolean hasList = corpus.contains("链表");
        boolean hasSeqTable = corpus.contains("顺序表");
        boolean hasArrayWord = corpus.contains("数组");
        return hasList && (hasSeqTable || hasArrayWord);
    }

    /**
     * 与前端演示知识点「二叉树 · 基本结构理解」判定一致：保证短时长预算下任务不被压到 4 个以下。
     */
    private static boolean matchesBinaryTreeBasicShowcaseGoal(StructuredLearningGoal goal) {
        if (goal == null) {
            return false;
        }
        StringBuilder sb = new StringBuilder();
        append(sb, goal.getRawGoalText());
        append(sb, goal.getNormalizedGoalText());
        append(sb, goal.getIntentDescription());
        append(sb, goal.getSubject());
        append(sb, goal.getSourceContext());
        append(sb, goal.getPriorityModule());
        List<String> topics = goal.getTopics();
        if (topics != null) {
            for (String t : topics) {
                append(sb, t);
            }
        }
        String corpus = sb.toString();
        return StringUtils.hasText(corpus) && corpus.contains("二叉树");
    }

    /**
     * 与前端演示知识点「DFS vs BFS」判定一致：短时长预算下保证 4 任务以启用 showcase。
     */
    private static boolean matchesDfsVsBfsShowcaseGoal(StructuredLearningGoal goal) {
        if (goal == null) {
            return false;
        }
        StringBuilder sb = new StringBuilder();
        append(sb, goal.getRawGoalText());
        append(sb, goal.getNormalizedGoalText());
        append(sb, goal.getIntentDescription());
        append(sb, goal.getSubject());
        append(sb, goal.getSourceContext());
        append(sb, goal.getPriorityModule());
        List<String> topics = goal.getTopics();
        if (topics != null) {
            for (String t : topics) {
                append(sb, t);
            }
        }
        String corpus = sb.toString();
        if (!StringUtils.hasText(corpus)) {
            return false;
        }
        String lower = corpus.toLowerCase();
        boolean hasDfs = lower.contains("dfs") || corpus.contains("深度优先");
        boolean hasBfs = lower.contains("bfs") || corpus.contains("广度优先");
        return hasDfs && hasBfs;
    }

    private static void append(StringBuilder sb, String part) {
        if (StringUtils.hasText(part)) {
            sb.append(part);
        }
    }
}
