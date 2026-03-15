package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.diagnosis.QuestionRationaleDto;
import com.pandanav.learning.domain.model.DiagnosisLearnerProfileSnapshot;
import com.pandanav.learning.domain.model.DiagnosisQuestionDraft;
import com.pandanav.learning.domain.model.DiagnosisStrategyDecision;
import com.pandanav.learning.domain.model.PlanningContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builds per-question rationales for the create response.
 * personalizedWhy is based on riskTags, topic and evidence only (no unconfirmed claims).
 */
@Component
public class QuestionRationaleBuilder {

    public List<QuestionRationaleDto> build(
        List<DiagnosisQuestionDraft> selectedDrafts,
        DiagnosisLearnerProfileSnapshot profileSnapshot,
        DiagnosisStrategyDecision strategyDecision,
        PlanningContext planningContext
    ) {
        if (selectedDrafts == null || selectedDrafts.isEmpty()) {
            return List.of();
        }
        String topic = resolveTopic(planningContext);
        String evidencePrefix = buildEvidencePrefix(profileSnapshot, topic);
        return selectedDrafts.stream()
            .map(d -> new QuestionRationaleDto(
                d.question().questionId(),
                d.selectionReason(),
                buildPersonalizedWhy(d, evidencePrefix, topic, profileSnapshot, strategyDecision)
            ))
            .toList();
    }

    private String buildEvidencePrefix(DiagnosisLearnerProfileSnapshot profile, String topic) {
        if (profile.evidence() == null || profile.evidence().isEmpty()) {
            return "为确认你的起点与目标";
        }
        if (topic != null && !topic.isBlank()) {
            return "你当前目标与主题涉及「" + topic + "」，系统还缺少部分维度的证据";
        }
        return "根据当前目标与主题";
    }

    private String buildPersonalizedWhy(
        DiagnosisQuestionDraft d,
        String evidencePrefix,
        String topic,
        DiagnosisLearnerProfileSnapshot profile,
        DiagnosisStrategyDecision strategy
    ) {
        String dimLabel = dimensionLabel(d.dimension().name());
        if (profile.riskTags() != null && !profile.riskTags().isEmpty()) {
            String riskHint = riskTagsToHint(profile.riskTags());
            if (riskHint != null) {
                return evidencePrefix + "，" + riskHint + "，因此先确认" + dimLabel + "。";
            }
        }
        if (strategy != null && strategy.personalizationReasons() != null && !strategy.personalizationReasons().isEmpty()) {
            String reason = strategy.personalizationReasons().get(0);
            if (reason != null && !reason.isBlank() && !reason.contains("时间") && !reason.contains("预算")) {
                return evidencePrefix + "，本次优先确认" + dimLabel + "，" + reason;
            }
        }
        return evidencePrefix + "，本题用于确认" + dimLabel + "。";
    }

    private static String riskTagsToHint(List<String> riskTags) {
        if (riskTags == null || riskTags.isEmpty()) return null;
        if (riskTags.contains("FOUNDATION_GAP_RISK") || riskTags.contains("ALGORITHM_JUMP_RISK")) {
            return "存在基础或算法跳跃风险";
        }
        if (riskTags.contains("GOAL_SCOPE_RISK")) {
            return "目标与章节范围需对齐";
        }
        if (riskTags.contains("TIME_CONSTRAINT_RISK")) {
            return "时间约束需确认";
        }
        return null;
    }

    private static String resolveTopic(PlanningContext context) {
        if (context == null) return "";
        if (context.topicName() != null && !context.topicName().isBlank()) return context.topicName().trim();
        if (context.chapterName() != null && !context.chapterName().isBlank()) return context.chapterName().trim();
        if (context.learningGoal() != null && !context.learningGoal().isBlank()) return context.learningGoal().trim();
        return "";
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
