package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.enums.DiagnosisDimension;
import com.pandanav.learning.domain.model.DiagnosisLearnerProfileSnapshot;
import com.pandanav.learning.domain.model.DiagnosisStrategyDecision;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.PlanningContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule-based diagnosis strategy decision. No LLM; strategy is stable and testable.
 */
@Service
public class DiagnosisStrategyDecisionService {

    public static final String STRATEGY_FOUNDATION_FIRST = "FOUNDATION_FIRST";
    public static final String STRATEGY_CONFLICT_CHECK = "CONFLICT_CHECK";
    public static final String STRATEGY_FAST_TRIAGE = "FAST_TRIAGE";
    public static final String TONE_GUIDING = "GUIDING";
    public static final String TONE_DIRECT = "DIRECT";
    public static final String TONE_REFLECTIVE = "REFLECTIVE";

    private static final int DEFAULT_QUESTION_COUNT = 5;

    public DiagnosisStrategyDecision decide(
        LearningSession session,
        PlanningContext planningContext,
        DiagnosisLearnerProfileSnapshot profile
    ) {
        boolean hasHistory = profile.hasHistory();
        boolean hasRecentFailures = profile.hasRecentFailures();
        boolean hasContradictionRisk = profile.hasContradictionRisk();
        String goalClarity = profile.goalClarity() != null ? profile.goalClarity() : "MEDIUM";
        String timeConstraint = profile.timeConstraint() != null ? profile.timeConstraint() : "MEDIUM";
        boolean goalClear = "HIGH".equals(goalClarity) || "MEDIUM".equals(goalClarity);
        boolean timeTight = "LOW".equals(timeConstraint);

        String strategyCode;
        List<String> priorityDimensions = new ArrayList<>();
        List<String> suppressedDimensions = new ArrayList<>();
        int targetQuestionCount = DEFAULT_QUESTION_COUNT;
        String toneStyle = TONE_GUIDING;
        List<String> reasons = new ArrayList<>();

        if (hasHistory && hasRecentFailures && goalClear) {
            strategyCode = STRATEGY_CONFLICT_CHECK;
            priorityDimensions.add(DiagnosisDimension.FOUNDATION.name());
            priorityDimensions.add(DiagnosisDimension.GOAL_STYLE.name());
            priorityDimensions.add(DiagnosisDimension.TIME_BUDGET.name());
            suppressedDimensions.add(DiagnosisDimension.LEARNING_PREFERENCE.name());
            reasons.add("有历史记录且存在近期失败，目标较明确，优先确认冲突与投入边界");
        } else if (!hasHistory && "BEGINNER".equals(profile.learnerStage())) {
            strategyCode = STRATEGY_FOUNDATION_FIRST;
            priorityDimensions.add(DiagnosisDimension.FOUNDATION.name());
            priorityDimensions.add(DiagnosisDimension.TIME_BUDGET.name());
            priorityDimensions.add(DiagnosisDimension.GOAL_STYLE.name());
            suppressedDimensions.add(DiagnosisDimension.DIFFICULTY_PAIN_POINT.name());
            reasons.add("新用户且处于入门阶段，优先确认起点与目标");
        } else if (timeTight || "LOW".equals(timeConstraint)) {
            strategyCode = STRATEGY_FAST_TRIAGE;
            targetQuestionCount = Math.min(DEFAULT_QUESTION_COUNT, 4);
            priorityDimensions.add(DiagnosisDimension.TIME_BUDGET.name());
            priorityDimensions.add(DiagnosisDimension.FOUNDATION.name());
            priorityDimensions.add(DiagnosisDimension.GOAL_STYLE.name());
            reasons.add("时间约束较强，采用精简题量快速定档");
        } else {
            strategyCode = STRATEGY_FOUNDATION_FIRST;
            priorityDimensions.add(DiagnosisDimension.FOUNDATION.name());
            priorityDimensions.add(DiagnosisDimension.GOAL_STYLE.name());
            priorityDimensions.add(DiagnosisDimension.TIME_BUDGET.name());
            priorityDimensions.add(DiagnosisDimension.LEARNING_PREFERENCE.name());
            reasons.add("优先确认前置基础与学习目标，再决定路径");
        }

        return new DiagnosisStrategyDecision(
            strategyCode,
            priorityDimensions,
            suppressedDimensions,
            targetQuestionCount,
            toneStyle,
            reasons
        );
    }
}
