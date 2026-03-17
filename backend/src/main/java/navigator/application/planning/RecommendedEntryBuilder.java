package navigator.application.planning;

import navigator.domain.enums.EntryGranularity;
import navigator.domain.model.GoalContextSnapshot;
import navigator.domain.model.RecommendedEntry;
import navigator.domain.model.StructuredLearningGoal;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Sprint 1: 按 strategy + entryGranularity 生成 RecommendedEntry 模板。
 */
@Component
public class RecommendedEntryBuilder {

    public RecommendedEntry build(String strategy, PlanningContext ctx) {
        if (ctx == null || ctx.getGoal() == null) {
            return RecommendedEntry.builder()
                    .title("先澄清核心概念")
                    .estimatedMinutes(10)
                    .reason("根据当前目标生成")
                    .build();
        }
        StructuredLearningGoal goal = ctx.getGoal();
        GoalContextSnapshot goalContext = ctx.getGoalContextSnapshot();
        List<String> topics = goal.getTopics() != null ? goal.getTopics() : List.of();
        String topicStr = topics.isEmpty() ? "相关主题" : String.join("、", topics);
        String subjectOrTopic = goal.getSubject() != null ? goal.getSubject() : topicStr;
        EntryGranularity granularity = goalContext != null && goalContext.getEntryGranularity() != null
                ? goalContext.getEntryGranularity() : EntryGranularity.SMALL;
        int minMin = minutesMin(granularity);
        int maxMin = minutesMax(granularity);

        String title;
        String reason;
        switch (strategy) {
            case PlanStrategySelector.FOUNDATION_PATCH:
                title = "先理解 " + topicStr + " 的定义、组成与最小示例";
                reason = "当前主要问题是前置基础不稳，直接进入复杂应用容易断层";
                break;
            case PlanStrategySelector.SPRINT_CORRECTION:
                title = topics.size() >= 2 ? "先对比 " + topics.get(0) + "/" + topics.get(1) + " 的核心差异与典型题入口" : "先对比核心差异与典型题入口";
                reason = "时间紧，优先解决最容易影响做题判断的关键区别";
                break;
            case PlanStrategySelector.DRILL_STRENGTHEN:
                title = "先做 1 个带讲解的典型例题：" + topicStr;
                reason = "当前阻塞更偏题型识别/步骤执行，例题驱动更高效";
                break;
            case PlanStrategySelector.FRAMEWORK_BUILD:
                title = "先建立 " + subjectOrTopic + " 的知识框架与主题关系图";
                reason = "你的目标范围较大，先有全局框架再推进局部内容更稳";
                break;
            case PlanStrategySelector.LOCAL_REPAIR:
                title = "先定点修补当前卡点：" + topicStr;
                reason = "当前问题是局部阻塞，不需要重新铺开整条路径";
                break;
            case PlanStrategySelector.CONCEPT_CLARIFICATION:
            default:
                title = "先澄清 " + topicStr + " 的核心概念和相邻概念边界";
                reason = "当前最需要的是把核心概念讲清，而不是直接堆练习";
                break;
        }
        int estimated = (minMin + maxMin) / 2;
        return RecommendedEntry.builder()
                .conceptId(null)
                .title(title)
                .estimatedMinutes(estimated)
                .reason(reason)
                .build();
    }

    private static int minutesMin(EntryGranularity g) {
        switch (g) {
            case MICRO: return 6;
            case MEDIUM: return 12;
            default: return 8;
        }
    }

    private static int minutesMax(EntryGranularity g) {
        switch (g) {
            case MICRO: return 10;
            case MEDIUM: return 20;
            default: return 12;
        }
    }
}
