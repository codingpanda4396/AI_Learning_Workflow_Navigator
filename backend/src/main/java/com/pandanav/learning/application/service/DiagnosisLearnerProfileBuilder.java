package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.model.DiagnosisLearnerProfileSnapshot;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.PlanningContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds a pre-diagnosis learner profile snapshot from session and planning context.
 * Can be extended later with history diagnosis / plan / attempts / mastery / weaknesses.
 */
@Component
public class DiagnosisLearnerProfileBuilder {

    private static final String STAGE_BEGINNER = "BEGINNER";
    private static final String STAGE_BASIC = "BASIC";
    private static final String STAGE_ADVANCED_HINT = "ADVANCED_HINT";
    private static final String CLARITY_LOW = "LOW";
    private static final String CLARITY_MEDIUM = "MEDIUM";
    private static final String CLARITY_HIGH = "HIGH";

    public DiagnosisLearnerProfileSnapshot build(LearningSession session, PlanningContext planningContext) {
        String goal = safeTrim(session != null ? session.getGoalText() : null);
        String topic = safeTrim(planningContext != null ? planningContext.topicName() : null);
        if (topic == null || topic.isEmpty()) {
            topic = safeTrim(planningContext != null ? planningContext.chapterName() : null);
        }
        if (topic == null || topic.isEmpty()) {
            topic = goal;
        }

        String learnerStage = inferLearnerStage(goal, topic);
        String goalClarity = inferGoalClarity(goal);
        String timeConstraint = CLARITY_MEDIUM;
        String confidenceLevel = CLARITY_MEDIUM;
        List<String> weaknessTags = new ArrayList<>();
        List<String> behaviorSignals = new ArrayList<>();
        List<String> evidence = new ArrayList<>();

        if (goal != null && !goal.isEmpty()) {
            evidence.add("本轮目标: " + (goal.length() > 50 ? goal.substring(0, 50) + "…" : goal));
        }
        if (topic != null && !topic.isEmpty()) {
            evidence.add("主题/章节: " + topic);
        }

        boolean hasHistory = false;
        boolean hasRecentFailures = false;
        boolean hasContradictionRisk = false;

        return new DiagnosisLearnerProfileSnapshot(
            learnerStage,
            goalClarity,
            timeConstraint,
            confidenceLevel,
            weaknessTags,
            behaviorSignals,
            evidence,
            hasHistory,
            hasRecentFailures,
            hasContradictionRisk
        );
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String inferLearnerStage(String goal, String topic) {
        if (goal == null || goal.isEmpty()) {
            return STAGE_BASIC;
        }
        String lower = goal.toLowerCase(Locale.ROOT);
        if (lower.contains("基础") || lower.contains("入门") || lower.contains("从零")) {
            return STAGE_BEGINNER;
        }
        if (lower.contains("进阶") || lower.contains("深入") || lower.contains("高级")) {
            return STAGE_ADVANCED_HINT;
        }
        return STAGE_BASIC;
    }

    private static String inferGoalClarity(String goal) {
        if (goal == null || goal.isEmpty()) {
            return CLARITY_LOW;
        }
        if (goal.length() >= 20 && (goal.contains("想") || goal.contains("希望") || goal.contains("目标"))) {
            return CLARITY_HIGH;
        }
        if (goal.length() >= 5) {
            return CLARITY_MEDIUM;
        }
        return CLARITY_LOW;
    }
}
