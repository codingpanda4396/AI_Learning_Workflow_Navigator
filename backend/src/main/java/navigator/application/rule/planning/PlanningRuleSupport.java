package navigator.application.rule.planning;

import navigator.application.planning.PlanningContext;
import navigator.domain.enums.FoundationLevel;
import navigator.domain.enums.GoalType;
import navigator.domain.enums.UrgencyLevel;
import navigator.domain.model.LearnerProfileSnapshot;
import navigator.domain.model.StructuredLearningGoal;

import java.util.List;

public final class PlanningRuleSupport {

    private PlanningRuleSupport() {
    }

    public static StructuredLearningGoal goal(PlanningContext context) {
        return context != null ? context.getGoal() : null;
    }

    public static LearnerProfileSnapshot profile(PlanningContext context) {
        return context != null ? context.getLearnerProfileSnapshot() : null;
    }

    public static boolean hasGoal(PlanningContext context) {
        return goal(context) != null;
    }

    public static boolean isScope(PlanningContext context, String expectedScope) {
        StructuredLearningGoal goal = goal(context);
        return goal != null && expectedScope.equals(goal.getTopicScopeType());
    }

    public static boolean isChapterOrCourse(PlanningContext context) {
        StructuredLearningGoal goal = goal(context);
        if (goal == null) {
            return false;
        }
        String scope = goal.getTopicScopeType();
        return "CHAPTER".equals(scope) || "COURSE".equals(scope);
    }

    public static boolean isGoalType(PlanningContext context, GoalType goalType) {
        StructuredLearningGoal goal = goal(context);
        return goal != null && goal.getGoalType() == goalType;
    }

    public static boolean isBeginner(PlanningContext context) {
        LearnerProfileSnapshot profile = profile(context);
        return profile != null && profile.getFoundationLevel() == FoundationLevel.BEGINNER;
    }

    public static boolean hasRisk(PlanningContext context, String riskTag) {
        LearnerProfileSnapshot profile = profile(context);
        List<String> riskTags = profile != null ? profile.getRiskTags() : null;
        return riskTags != null && riskTags.contains(riskTag);
    }

    public static String primaryGap(PlanningContext context) {
        LearnerProfileSnapshot profile = profile(context);
        List<String> blockerTags = profile != null ? profile.getBlockerTags() : null;
        return blockerTags != null && !blockerTags.isEmpty() ? blockerTags.get(0) : null;
    }

    public static boolean hasPrimaryGap(PlanningContext context, String gapTag) {
        return gapTag.equals(primaryGap(context));
    }

    public static boolean hasHighUrgency(PlanningContext context) {
        StructuredLearningGoal goal = goal(context);
        return goal != null && goal.getUrgencyLevel() == UrgencyLevel.HIGH;
    }
}
