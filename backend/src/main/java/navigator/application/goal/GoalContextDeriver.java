package navigator.application.goal;

import navigator.domain.enums.EntryGranularity;
import navigator.domain.enums.PlanningMode;
import navigator.domain.enums.PreferenceTag;
import navigator.domain.enums.SelfReportedLevel;
import navigator.domain.model.GoalContextSnapshot;
import navigator.domain.model.StructuredLearningGoal;
import navigator.domain.enums.TimeBudget;
import navigator.domain.enums.GoalType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sprint 1: 从 StructuredLearningGoal 推导 GoalContextSnapshot（规则 6～9）。
 */
@Component
public class GoalContextDeriver {

    public GoalContextSnapshot derive(StructuredLearningGoal goal) {
        if (goal == null) {
            return GoalContextSnapshot.builder()
                    .structuredGoal(null)
                    .requiresDiagnosis(true)
                    .planningMode(PlanningMode.CONCEPT_CLARIFICATION)
                    .entryGranularity(EntryGranularity.SMALL)
                    .strategyHints(List.of())
                    .riskTags(List.of())
                    .explanationFocus(List.of())
                    .createdFrom("USER_INPUT_V1")
                    .version(1)
                    .build();
        }
        PlanningMode planningMode = derivePlanningMode(goal);
        EntryGranularity entryGranularity = deriveEntryGranularity(goal);
        List<String> riskTags = deriveRiskTags(goal);
        List<String> strategyHints = deriveStrategyHints(goal);

        return GoalContextSnapshot.builder()
                .structuredGoal(null)
                .requiresDiagnosis(true)
                .planningMode(planningMode)
                .entryGranularity(entryGranularity)
                .strategyHints(strategyHints)
                .riskTags(riskTags)
                .explanationFocus(List.of("根据目标与时间预算生成"))
                .createdFrom("USER_INPUT_V1")
                .version(1)
                .build();
    }

    private PlanningMode derivePlanningMode(StructuredLearningGoal goal) {
        boolean highUrgency = "HIGH".equals(goal.getUrgencyLevel());
        String scope = goal.getTopicScopeType();
        boolean chapterOrCourse = "CHAPTER".equals(scope) || "COURSE".equals(scope);
        boolean systematicGoal = goal.getGoalType() == GoalType.BUILD_SYSTEMATIC_UNDERSTANDING;
        boolean practiceGoal = goal.getGoalType() == GoalType.PRACTICE_ENHANCEMENT;
        var prefs = goal.getPreferenceTags() != null ? goal.getPreferenceTags() : List.<PreferenceTag>of();
        boolean practiceFirst = prefs.contains(PreferenceTag.PRACTICE_FIRST);
        boolean beginner = goal.getSelfReportedLevel() == SelfReportedLevel.BEGINNER;

        if (goal.getGoalType() == GoalType.REVIEW_FOR_EXAM && highUrgency) {
            return PlanningMode.EXAM_CRASH;
        }
        if (systematicGoal || chapterOrCourse) {
            return PlanningMode.SYSTEMATIC_BUILD;
        }
        if (practiceGoal || practiceFirst) {
            return PlanningMode.PRACTICE_DRIVEN;
        }
        if (beginner && !systematicGoal) {
            return PlanningMode.STEADY_FOUNDATION;
        }
        return PlanningMode.CONCEPT_CLARIFICATION;
    }

    private EntryGranularity deriveEntryGranularity(StructuredLearningGoal goal) {
        TimeBudget budget = goal.getTimeBudget();
        if (budget == TimeBudget.WITHIN_15_MIN || budget == TimeBudget.WITHIN_30_MIN) {
            return EntryGranularity.MICRO;
        }
        if (goal.getSelfReportedLevel() == SelfReportedLevel.BEGINNER) {
            return EntryGranularity.SMALL;
        }
        if ("MULTI_TOPIC".equals(goal.getTopicScopeType())) {
            return EntryGranularity.SMALL;
        }
        if (("CHAPTER".equals(goal.getTopicScopeType()) || "COURSE".equals(goal.getTopicScopeType()))
                && budget == TimeBudget.LONG_TERM) {
            return EntryGranularity.MEDIUM;
        }
        return EntryGranularity.SMALL;
    }

    private List<String> deriveRiskTags(StructuredLearningGoal goal) {
        List<String> tags = new ArrayList<>();
        TimeBudget budget = goal.getTimeBudget();
        boolean shortTime = budget == TimeBudget.WITHIN_15_MIN || budget == TimeBudget.WITHIN_30_MIN;
        String raw = goal.getRawGoalText() != null ? goal.getRawGoalText() : "";
        if (shortTime || raw.contains("明天") || raw.contains("今晚")) {
            tags.add("TIME_PRESSURE");
        }
        if (("CHAPTER".equals(goal.getTopicScopeType()) || "COURSE".equals(goal.getTopicScopeType())) && shortTime) {
            tags.add("GOAL_TOO_BROAD");
        }
        List<String> topics = goal.getTopics();
        if (raw.trim().length() < 3 || (topics != null && topics.contains("未指定主题"))) {
            tags.add("GOAL_TOO_VAGUE");
        }
        if (goal.getSelfReportedLevel() == SelfReportedLevel.SOLID_BUT_WANT_IMPROVE
                && (raw.contains("不会") || raw.contains("一直错"))) {
            tags.add("OVERCONFIDENCE_RISK");
        }
        return tags.isEmpty() ? List.of() : tags;
    }

    private List<String> deriveStrategyHints(StructuredLearningGoal goal) {
        List<String> hints = new ArrayList<>();
        if (goal.getSelfReportedLevel() == SelfReportedLevel.BEGINNER) {
            hints.add("PREREQUISITE_FIRST");
        }
        if (goal.getGoalType() == GoalType.REVIEW_FOR_EXAM) {
            hints.add("CORE_CONTRAST_FIRST");
        }
        var prefs = goal.getPreferenceTags() != null ? goal.getPreferenceTags() : List.<PreferenceTag>of();
        if (prefs.contains(PreferenceTag.EXAMPLE_FIRST)) {
            hints.add("EXAMPLE_BEFORE_ABSTRACTION");
        }
        if (prefs.contains(PreferenceTag.PRACTICE_FIRST)) {
            hints.add("ONE_EXAMPLE_ONE_PRACTICE");
        }
        if (prefs.contains(PreferenceTag.STEP_BY_STEP)) {
            hints.add("STEP_BY_STEP_PROGRESS");
        }
        if (goal.getGoalType() == GoalType.BUILD_SYSTEMATIC_UNDERSTANDING) {
            hints.add("FRAMEWORK_FIRST");
        }
        return hints.isEmpty() ? List.of("CONCEPT_FIRST") : hints;
    }
}
