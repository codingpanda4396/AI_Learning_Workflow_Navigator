package navigator.application.planning;

import navigator.domain.enums.FoundationLevel;
import navigator.domain.enums.GoalType;
import navigator.domain.model.GoalContextSnapshot;
import navigator.domain.model.LearnerProfileSnapshot;
import navigator.domain.model.StructuredLearningGoal;
import org.springframework.stereotype.Component;

/**
 * Sprint 1: 按文档优先级选择策略（字符串常量），调用方再映射为 RecommendedStrategyCode。
 */
@Component
public class PlanStrategySelector {

    public static final String SYSTEMATIC_PROGRESSIVE = "SYSTEMATIC_PROGRESSIVE";
    public static final String FOUNDATION_REBUILD = "FOUNDATION_REBUILD";
    public static final String COMPRESSED_REVIEW = "COMPRESSED_REVIEW";
    public static final String PRACTICE_DRIVEN = "PRACTICE_DRIVEN";
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
        boolean highUrgency = "HIGH".equals(goal.getUrgencyLevel());

        if (chapterOrCourse || systematicGoal) return SYSTEMATIC_PROGRESSIVE;
        if (beginner || prerequisiteGap) return FOUNDATION_REBUILD;
        if (goal.getGoalType() == GoalType.REVIEW_FOR_EXAM && highUrgency && !beginner) return COMPRESSED_REVIEW;
        if (procedureOrQuestionGap || goal.getGoalType() == GoalType.PRACTICE_ENHANCEMENT) return PRACTICE_DRIVEN;
        if (goal.getGoalType() == GoalType.FIX_SPECIFIC_BLOCKER && "SINGLE_TOPIC".equals(scope)) return LOCAL_REPAIR;
        return CONCEPT_CLARIFICATION;
    }
}
