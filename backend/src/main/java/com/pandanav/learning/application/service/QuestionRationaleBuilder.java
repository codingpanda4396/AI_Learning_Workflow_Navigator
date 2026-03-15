package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.diagnosis.QuestionRationaleDto;
import com.pandanav.learning.domain.model.DiagnosisLearnerProfileSnapshot;
import com.pandanav.learning.domain.model.DiagnosisQuestionDraft;
import com.pandanav.learning.domain.model.DiagnosisStrategyDecision;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builds per-question rationales for the create response.
 */
@Component
public class QuestionRationaleBuilder {

    public List<QuestionRationaleDto> build(
        List<DiagnosisQuestionDraft> selectedDrafts,
        DiagnosisLearnerProfileSnapshot profileSnapshot,
        DiagnosisStrategyDecision strategyDecision
    ) {
        if (selectedDrafts == null || selectedDrafts.isEmpty()) {
            return List.of();
        }
        String topicContext = profileSnapshot.evidence() != null && !profileSnapshot.evidence().isEmpty()
            ? "根据当前目标与主题"
            : "为确认你的起点与目标";
        return selectedDrafts.stream()
            .map(d -> new QuestionRationaleDto(
                d.question().questionId(),
                d.selectionReason(),
                buildPersonalizedWhy(d, topicContext, strategyDecision)
            ))
            .toList();
    }

    private String buildPersonalizedWhy(
        DiagnosisQuestionDraft d,
        String topicContext,
        DiagnosisStrategyDecision strategy
    ) {
        if (strategy != null && strategy.personalizationReasons() != null && !strategy.personalizationReasons().isEmpty()) {
            return topicContext + "，本次优先确认" + dimensionLabel(d.dimension().name()) + "，" + strategy.personalizationReasons().get(0);
        }
        return topicContext + "，本题用于确认" + dimensionLabel(d.dimension().name()) + "。";
    }

    private static String dimensionLabel(String dim) {
        return switch (dim) {
            case "FOUNDATION" -> "前置基础";
            case "TIME_BUDGET" -> "时间投入";
            case "GOAL_STYLE" -> "学习目标";
            case "LEARNING_PREFERENCE" -> "学习偏好";
            case "EXPERIENCE" -> "过往经验";
            case "DIFFICULTY_PAIN_POINT" -> "难点与支持";
            default -> dim;
        };
    }
}
