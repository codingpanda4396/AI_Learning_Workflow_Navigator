package navigator.application.planning;

import navigator.domain.enums.FoundationLevel;
import navigator.domain.enums.GoalType;
import navigator.domain.enums.UrgencyLevel;
import navigator.domain.model.GoalContextSnapshot;
import navigator.domain.model.LearnerProfileSnapshot;
import navigator.domain.model.StructuredLearningGoal;
import org.springframework.stereotype.Component;

/**
 * Sprint 2.5: 规则矩阵选择策略，输出 RecommendedStrategyCode 对应的字符串。
 */
@Component
public class PlanStrategySelector {

    public static final String FOUNDATION_PATCH = "FOUNDATION_PATCH";
    public static final String FRAMEWORK_BUILD = "FRAMEWORK_BUILD";
    public static final String DRILL_STRENGTHEN = "DRILL_STRENGTHEN";
    public static final String SPRINT_CORRECTION = "SPRINT_CORRECTION";
    public static final String LOCAL_REPAIR = "LOCAL_REPAIR";
    public static final String CONCEPT_CLARIFICATION = "CONCEPT_CLARIFICATION";

    public String select(PlanningContext ctx) {
        if (ctx == null) return CONCEPT_CLARIFICATION;
        StructuredLearningGoal goal = ctx.getGoal();
        GoalContextSnapshot goalContext = ctx.getGoalContextSnapshot();
        LearnerProfileSnapshot profile = ctx.getLearnerProfileSnapshot();
        if (goal == null) return CONCEPT_CLARIFICATION;

        String scope = goal.getTopicScopeType();
        boolean chapterOrCourse = "CHAPTER".equals(scope) || "COURSE".equals(scope);
        boolean systematicGoal = goal.getGoalType() == GoalType.BUILD_SYSTEMATIC_UNDERSTANDING;
        boolean prerequisiteGap = profile != null && profile.getRiskTags() != null && profile.getRiskTags().contains("PREREQUISITE_GAP");
        boolean beginner = profile != null && profile.getFoundationLevel() == FoundationLevel.BEGINNER;
        String primaryGap = profile != null && profile.getBlockerTags() != null && !profile.getBlockerTags().isEmpty() ? profile.getBlockerTags().get(0) : null;
        boolean procedureOrQuestionGap = "QUESTION_TYPE_RECOGNITION_GAP".equals(primaryGap) || "PROCEDURE_GAP".equals(primaryGap);
        boolean highUrgency = UrgencyLevel.HIGH == goal.getUrgencyLevel();

        if (chapterOrCourse || systematicGoal) return FRAMEWORK_BUILD;
        if (beginner || prerequisiteGap) return FOUNDATION_PATCH;
        if (goal.getGoalType() == GoalType.REVIEW_FOR_EXAM && highUrgency && !beginner) return SPRINT_CORRECTION;
        if (procedureOrQuestionGap || goal.getGoalType() == GoalType.PRACTICE_ENHANCEMENT) return DRILL_STRENGTHEN;
        if (goal.getGoalType() == GoalType.FIX_SPECIFIC_BLOCKER && "SINGLE_TOPIC".equals(scope)) return LOCAL_REPAIR;
        return CONCEPT_CLARIFICATION;
    }
}
