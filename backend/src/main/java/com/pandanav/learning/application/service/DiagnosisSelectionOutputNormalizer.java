package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.model.DiagnosisLearnerProfileSnapshot;
import com.pandanav.learning.domain.model.DiagnosisLlmSelectionResult;
import com.pandanav.learning.domain.model.DiagnosisQuestionCandidate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Normalizes LLM selection output: enforce Chinese for selectionReasons and learnerSummary.
 * When non-CJK ratio is too high or summary over-claims, replace with rule-based copy.
 */
@Component
public class DiagnosisSelectionOutputNormalizer {

    private static final double MIN_CJK_RATIO = 0.7;

    /**
     * Normalize selection result so natural language fields are Chinese and summary does not over-claim.
     */
    public DiagnosisLlmSelectionResult normalize(
        DiagnosisLlmSelectionResult result,
        DiagnosisLearnerProfileSnapshot profile,
        String topic,
        List<DiagnosisQuestionCandidate> candidates
    ) {
        if (result == null) return result;
        Map<String, String> questionIdToDimension = new LinkedHashMap<>();
        if (candidates != null) {
            for (DiagnosisQuestionCandidate c : candidates) {
                if (c.questionId() != null && c.dimension() != null) {
                    questionIdToDimension.put(c.questionId(), c.dimension().name());
                }
            }
        }
        Map<String, String> reasons = normalizeReasons(result.selectionReasons(), questionIdToDimension);
        String summary = normalizeSummary(result.learnerSummary(), profile, topic);
        return new DiagnosisLlmSelectionResult(
            result.strategyCode(),
            result.selectedQuestionIds(),
            result.questionOrder(),
            reasons,
            result.suppressedQuestionIds(),
            summary
        );
    }

    private Map<String, String> normalizeReasons(Map<String, String> selectionReasons, Map<String, String> questionIdToDimension) {
        Map<String, String> out = new LinkedHashMap<>();
        if (selectionReasons == null) return out;
        for (Map.Entry<String, String> e : selectionReasons.entrySet()) {
            String id = e.getKey();
            String value = e.getValue();
            if (value == null || value.isBlank() || cjkRatio(value) < MIN_CJK_RATIO) {
                value = dimensionToReasonLabel(questionIdToDimension.getOrDefault(id, "FOUNDATION"));
            }
            out.put(id, value);
        }
        return out;
    }

    private String normalizeSummary(String learnerSummary, DiagnosisLearnerProfileSnapshot profile, String topic) {
        if (learnerSummary != null && !learnerSummary.isBlank()
            && cjkRatio(learnerSummary) >= MIN_CJK_RATIO
            && !overClaimsUnconfirmed(learnerSummary, profile)) {
            return learnerSummary;
        }
        return ruleSummary(profile, topic);
    }

    private boolean overClaimsUnconfirmed(String summary, DiagnosisLearnerProfileSnapshot profile) {
        if (summary == null) return false;
        if (summary.contains("中等时间") || summary.contains("时间预算") || summary.contains("适配中等")) {
            boolean hasTimeEvidence = profile.evidence() != null
                && profile.evidence().stream().anyMatch(s -> s != null && (s.contains("时间") || s.contains("投入")));
            if (!hasTimeEvidence) return true;
        }
        return false;
    }

    private static double cjkRatio(String text) {
        if (text == null || text.isEmpty()) return 1.0;
        int cjk = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            Character.UnicodeScript script = Character.UnicodeScript.of(c);
            if (script == Character.UnicodeScript.HAN
                || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA) {
                cjk++;
            }
        }
        return (double) cjk / text.length();
    }

    private static String dimensionToReasonLabel(String dimension) {
        if (dimension == null) dimension = "FOUNDATION";
        return switch (dimension) {
            case "FOUNDATION" -> "确认图论基础";
            case "TIME_BUDGET" -> "控制学习节奏";
            case "GOAL_STYLE" -> "对齐学习目标";
            case "LEARNING_PREFERENCE" -> "定制内容交付方式";
            case "EXPERIENCE" -> "了解过往经验";
            case "DIFFICULTY_PAIN_POINT" -> "定位难点与支持";
            default -> "确认" + dimension;
        };
    }

    private static String ruleSummary(DiagnosisLearnerProfileSnapshot profile, String topic) {
        if (profile != null && profile.goalClarity() != null && "HIGH".equals(profile.goalClarity())) {
            if (topic != null && !topic.isBlank()) {
                return "当前已知目标与「" + topic + "」，本次将确认前置基础与投入边界是否匹配。";
            }
            return "当前目标较明确，本次将确认前置基础与投入边界是否匹配。";
        }
        if (profile == null || profile.evidence() == null || profile.evidence().isEmpty()) {
            return "当前学习数据较少，本次将先确认你的起点与目标。";
        }
        return "根据当前目标与主题，本次将确认学习起点并据此规划路径。";
    }
}
