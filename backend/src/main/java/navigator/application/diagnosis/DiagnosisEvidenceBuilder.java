package navigator.application.diagnosis;

import navigator.domain.model.DiagnosisEvidenceSummary;
import navigator.domain.model.GoalContextSnapshot;
import navigator.domain.model.LearnerProfileSnapshot;
import navigator.domain.model.StructuredLearningGoal;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sprint 1: 从 goal + profile 生成 DiagnosisEvidenceSummary（模板化，不走 LLM）。
 */
@Component
public class DiagnosisEvidenceBuilder {

    public DiagnosisEvidenceSummary build(
            LearnerProfileSnapshot profile,
            StructuredLearningGoal goal,
            GoalContextSnapshot goalContext,
            String primaryGapType) {
        String currentState = buildCurrentState(goal, profile);
        List<String> keyEvidence = buildKeyEvidence(goal, profile, goalContext);
        List<String> primaryRiskTags = profile.getRiskTags() != null ? profile.getRiskTags() : List.of();
        List<String> explanationPoints = buildExplanationPoints(goalContext, profile, primaryGapType);
        String summary = currentState + " " + String.join("；", explanationPoints);

        return DiagnosisEvidenceSummary.builder()
                .summary(summary)
                .keyEvidence(keyEvidence)
                .primaryGapType(primaryGapType != null ? primaryGapType : "GENERAL")
                .primaryRiskTags(primaryRiskTags)
                .explanationPoints(explanationPoints)
                .build();
    }

    private String buildCurrentState(StructuredLearningGoal goal, LearnerProfileSnapshot profile) {
        String goalDesc = goal != null && goal.getGoalType() != null
                ? "当前目标为" + goalTypeLabel(goal.getGoalType().name()) : "当前目标已设定";
        String foundationDesc = profile != null && profile.getFoundationLevel() != null
                ? "用户自述基础为" + profile.getFoundationLevel().name() : "";
        String gapDesc = profile != null && profile.getBlockerTags() != null && !profile.getBlockerTags().isEmpty()
                ? "主要阻塞点在" + String.join("、", profile.getBlockerTags()) : "";
        return String.join("。", List.of(goalDesc, foundationDesc, gapDesc)).replaceAll("。+", "。").trim();
    }

    private static String goalTypeLabel(String type) {
        switch (type) {
            case "REVIEW_FOR_EXAM": return "考前复习";
            case "BUILD_SYSTEMATIC_UNDERSTANDING": return "系统学习";
            case "FIX_SPECIFIC_BLOCKER": return "局部修补";
            case "PRACTICE_ENHANCEMENT": return "练习增强";
            default: return "新概念学习";
        }
    }

    private List<String> buildKeyEvidence(StructuredLearningGoal goal, LearnerProfileSnapshot profile, GoalContextSnapshot goalContext) {
        List<String> items = new ArrayList<>();
        if (goal != null) {
            items.add("当前目标为" + goalTypeLabel(goal.getGoalType().name()) + "，主题范围为" + (goal.getTopicScopeType() != null ? goal.getTopicScopeType() : "未指定"));
        }
        if (profile != null && profile.getFoundationLevel() != null) {
            items.add("当前基础判断为" + profile.getFoundationLevel().name());
        }
        if (profile != null && profile.getBlockerTags() != null && !profile.getBlockerTags().isEmpty()) {
            items.add("当前主要断层为" + String.join("、", profile.getBlockerTags()));
        }
        if (goalContext != null && goalContext.getRiskTags() != null && !goalContext.getRiskTags().isEmpty()) {
            items.add("风险标签：" + String.join("、", goalContext.getRiskTags()));
        }
        return items.isEmpty() ? List.of("诊断证据已生成") : items;
    }

    private List<String> buildExplanationPoints(GoalContextSnapshot goalContext, LearnerProfileSnapshot profile, String primaryGapType) {
        List<String> points = new ArrayList<>();
        if (goalContext != null && goalContext.getRiskTags() != null && goalContext.getRiskTags().contains("TIME_PRESSURE")) {
            points.add("时间有限，系统优先压缩范围");
        }
        if (profile != null && (profile.getFoundationLevel() != null && profile.getFoundationLevel().name().equals("BEGINNER") || (profile.getRiskTags() != null && profile.getRiskTags().contains("PREREQUISITE_GAP")))) {
            points.add("存在前置基础缺口，需先补关键前提");
        }
        if (primaryGapType != null) {
            points.add("主要缺口类型：" + primaryGapType);
        }
        return points.isEmpty() ? List.of("根据诊断结果生成规划建议") : points;
    }
}
