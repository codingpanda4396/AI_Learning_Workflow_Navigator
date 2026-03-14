package com.pandanav.learning.application.service.learningplan;

import com.pandanav.learning.domain.enums.LearnerSignalTier;
import com.pandanav.learning.domain.model.LearnerEvidenceSummary;
import com.pandanav.learning.domain.model.LearnerSignalSnapshot;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class LearnerEvidenceAggregator {

    public LearnerEvidenceSummary aggregate(
        LearningPlanPlanningContext context,
        LearnerSignalSnapshot snapshot,
        String recommendedNodeName
    ) {
        List<LearnerEvidenceSummary.LearnerEvidenceSignal> signals = List.of(
            toSignal("concept_understanding", snapshot.conceptUnderstanding(), context.weakPointLabels(), context.recentScores()),
            toSignal("relationship_understanding", snapshot.relationshipUnderstanding(), context.recentErrorTags(), context.weakPointLabels()),
            toSignal("code_mapping", snapshot.codeMapping(), context.recentErrorTags(), context.recentScores()),
            toSignal("recent_stability", snapshot.recentStability(), context.recentScores(), context.recentErrorTags())
        );

        List<String> topEvidence = buildTopEvidence(context);
        String safeNodeName = recommendedNodeName == null || recommendedNodeName.isBlank() ? "当前关键知识点" : recommendedNodeName.trim();
        String whyThisStep = "系统优先推荐「" + safeNodeName + "」，因为这一步最能减少当前路径中的连续卡点。";
        String skipRisk = buildSkipRisk(snapshot, safeNodeName);
        String expectedGain = "完成这一步后，你会更容易进入后续训练，并且错因定位会更聚焦。";
        String confidenceHint = buildConfidenceHint(snapshot.confidence(), topEvidence.size());

        return new LearnerEvidenceSummary(
            signals,
            signalTierText(snapshot.confidence()),
            resolveTrend(context),
            topEvidence,
            whyThisStep,
            skipRisk,
            expectedGain,
            confidenceHint
        );
    }

    private LearnerEvidenceSummary.LearnerEvidenceSignal toSignal(
        String key,
        LearnerSignalTier signalTier,
        List<?> evidence1,
        List<?> evidence2
    ) {
        List<String> evidence = new ArrayList<>();
        appendEvidence(evidence, evidence1);
        appendEvidence(evidence, evidence2);
        while (evidence.size() < 2) {
            evidence.add("当前证据正在持续收集中。");
        }
        return new LearnerEvidenceSummary.LearnerEvidenceSignal(
            key,
            signalTierText(signalTier),
            signalTier == LearnerSignalTier.STABLE ? 0.82D : signalTier == LearnerSignalTier.WEAK ? 0.68D : 0.46D,
            evidence.stream().limit(3).toList(),
            signalTier == LearnerSignalTier.WEAK ? "declining" : "stable"
        );
    }

    private void appendEvidence(List<String> target, List<?> source) {
        if (source == null) {
            return;
        }
        for (Object item : source) {
            if (item == null) {
                continue;
            }
            String text = String.valueOf(item).trim();
            if (text.isBlank()) {
                continue;
            }
            target.add(text);
            if (target.size() >= 3) {
                return;
            }
        }
    }

    private List<String> buildTopEvidence(LearningPlanPlanningContext context) {
        List<String> result = new ArrayList<>();
        if (context.weakPointLabels() != null) {
            for (String item : context.weakPointLabels()) {
                if (item == null || item.isBlank()) {
                    continue;
                }
                result.add("薄弱点集中在「" + item.trim() + "」");
                if (result.size() >= 2) {
                    break;
                }
            }
        }
        if (context.recentErrorTags() != null) {
            for (String item : context.recentErrorTags()) {
                if (item == null || item.isBlank()) {
                    continue;
                }
                result.add("近期错误标签出现「" + item.trim() + "」");
                if (result.size() >= 3) {
                    break;
                }
            }
        }
        if (result.isEmpty() && context.recentScores() != null && !context.recentScores().isEmpty()) {
            int avg = (int) Math.round(context.recentScores().stream().mapToInt(Integer::intValue).average().orElse(0D));
            result.add("近期得分均值约 " + avg + " 分");
        }
        if (result.isEmpty()) {
            result.add("当前可用历史证据较少，系统采用低风险起步策略");
        }
        return result.stream().limit(3).toList();
    }

    private String resolveTrend(LearningPlanPlanningContext context) {
        if (context.recentScores() == null || context.recentScores().size() < 2) {
            return "flat";
        }
        int first = context.recentScores().get(0);
        int last = context.recentScores().get(context.recentScores().size() - 1);
        if (last - first >= 8) {
            return "up";
        }
        if (first - last >= 8) {
            return "down";
        }
        return "flat";
    }

    private String buildSkipRisk(LearnerSignalSnapshot snapshot, String recommendedNodeName) {
        if (snapshot.relationshipUnderstanding() == LearnerSignalTier.WEAK || snapshot.conceptUnderstanding() == LearnerSignalTier.WEAK) {
            return "如果跳过「" + recommendedNodeName + "」，后续节点的概念连接更容易断链并反复返工。";
        }
        if (snapshot.codeMapping() == LearnerSignalTier.WEAK) {
            return "如果跳过这一步，进入训练时更容易出现“会概念但落不到代码”的卡顿。";
        }
        return "如果跳过这一步，后续训练阶段的稳定性会下降。";
    }

    private String buildConfidenceHint(LearnerSignalTier tier, int evidenceCount) {
        if (tier == LearnerSignalTier.STABLE) {
            return "当前推荐基于近期学习证据，可信度较高。";
        }
        if (tier == LearnerSignalTier.WEAK) {
            return "当前证据显示你在关键连接处仍有波动，建议先完成主任务再判断是否提速。";
        }
        return evidenceCount >= 2
            ? "当前证据仍在补齐中，系统会根据你完成第一步后的表现快速校准。"
            : "当前证据有限，系统先给出稳健起步动作。";
    }

    private String signalTierText(LearnerSignalTier tier) {
        if (tier == null) {
            return LearnerSignalTier.UNKNOWN.name().toLowerCase(Locale.ROOT);
        }
        return tier.name().toLowerCase(Locale.ROOT);
    }
}
