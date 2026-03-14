package com.pandanav.learning.application.service.learningplan;

import com.pandanav.learning.api.dto.plan.LearningPlanPreviewResponse;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.LearningPlanPreview;
import com.pandanav.learning.domain.model.PlanAlternative;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PreviewTemplateExplanationAssembler {

    public PreviewExplanations build(LearningPlanPreview preview, LearningPlanPlanningContext context) {
        String recommendedTitle = preview.summary().recommendedStartNodeName();
        String reason = clamp(
            "先从「" + safe(recommendedTitle, "当前关键知识点") + "」开始，因为它是你当前目标里最容易影响后续推进的一段。",
            72
        );
        String currentState = context != null && context.learnerStateSnapshot() != null
            ? safe(context.learnerStateSnapshot().primaryBlockDescription(), "你当前最需要先补稳一段关键基础。")
            : "你当前最需要先补稳一段关键基础。";
        List<String> evidence = new ArrayList<>();
        if (context != null && context.weakPointLabels() != null) {
            for (String item : context.weakPointLabels()) {
                if (item == null || item.isBlank()) {
                    continue;
                }
                evidence.add(clamp("近期薄弱点集中在「" + item.trim() + "」。", 48));
                if (evidence.size() >= 3) {
                    break;
                }
            }
        }
        if (evidence.isEmpty() && context != null && context.recentErrorTags() != null) {
            for (String item : context.recentErrorTags()) {
                if (item == null || item.isBlank()) {
                    continue;
                }
                evidence.add(clamp("最近错误标签反复出现「" + item.trim() + "」。", 48));
                if (evidence.size() >= 3) {
                    break;
                }
            }
        }
        while (evidence.size() < 3) {
            evidence.add("这一步是后续学习路径的前置环节。");
        }

        String strategyCode = resolveRecommendedStrategyCode(context, preview);
        String strategyLabel = strategyLabel(strategyCode);
        String strategyExplanation = clamp(
            "当前先走「" + strategyLabel + "」，是为了降低后续反复卡住的风险。",
            56
        );

        List<LearningPlanPreviewResponse.AlternativeStrategyResponse> alternatives = new ArrayList<>();
        for (PlanAlternative item : preview.summary().alternatives()) {
            String code = safe(item.strategy(), "UNKNOWN");
            if (code.equals(strategyCode)) {
                continue;
            }
            alternatives.add(new LearningPlanPreviewResponse.AlternativeStrategyResponse(
                code,
                strategyLabel(code),
                clamp("这条路这次不优先，因为当前证据更支持先稳住关键薄弱点。", 56)
            ));
            if (alternatives.size() >= 2) {
                break;
            }
        }

        return new PreviewExplanations(
            reason,
            currentState,
            evidence,
            strategyCode,
            strategyLabel,
            strategyExplanation,
            alternatives
        );
    }

    private String resolveRecommendedStrategyCode(LearningPlanPlanningContext context, LearningPlanPreview preview) {
        if (context != null && context.requestedStrategy() != null && !context.requestedStrategy().isBlank()) {
            return context.requestedStrategy().trim();
        }
        if (preview != null && preview.summary() != null && preview.summary().recommendedPace() != null) {
            String pace = preview.summary().recommendedPace().trim().toUpperCase();
            if ("LIGHT".equals(pace)) {
                return "COMPRESSED_10_MIN";
            }
            if ("INTENSIVE".equals(pace)) {
                return "FAST_TRACK";
            }
        }
        return "FOUNDATION_FIRST";
    }

    public String strategyLabel(String code) {
        return switch (safe(code, "").toUpperCase()) {
            case "FAST_TRACK" -> "快速推进";
            case "PRACTICE_FIRST" -> "先练后学";
            case "COMPRESSED_10_MIN" -> "10 分钟压缩版";
            default -> "先补基础";
        };
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String clamp(String value, int maxChars) {
        String text = safe(value, "").replaceAll("\\s+", " ");
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars - 1) + "…";
    }

    public record PreviewExplanations(
        String recommendedEntryReason,
        String learnerCurrentState,
        List<String> learnerEvidence,
        String strategyCode,
        String strategyLabel,
        String strategyExplanation,
        List<LearningPlanPreviewResponse.AlternativeStrategyResponse> alternatives
    ) {
    }
}
