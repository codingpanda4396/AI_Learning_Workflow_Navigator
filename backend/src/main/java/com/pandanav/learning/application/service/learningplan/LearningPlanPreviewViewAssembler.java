package com.pandanav.learning.application.service.learningplan;

import com.pandanav.learning.domain.enums.DecisionFallbackLevel;
import com.pandanav.learning.domain.model.ActionTemplate;
import com.pandanav.learning.domain.model.AlternativeExplanation;
import com.pandanav.learning.domain.model.EntryCandidate;
import com.pandanav.learning.domain.model.IntensityCandidate;
import com.pandanav.learning.domain.model.LearnerState;
import com.pandanav.learning.domain.model.LearningPlanContextNode;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.LearningPlanPreview;
import com.pandanav.learning.domain.model.LearningPlanSummary;
import com.pandanav.learning.domain.model.LlmPlanDecisionResult;
import com.pandanav.learning.domain.model.PlanAdjustments;
import com.pandanav.learning.domain.model.PlanAlternative;
import com.pandanav.learning.domain.model.PlanCandidateSet;
import com.pandanav.learning.domain.model.PlanPathNode;
import com.pandanav.learning.domain.model.PlanReason;
import com.pandanav.learning.domain.model.PlanTaskPreview;
import com.pandanav.learning.domain.model.StrategyCandidate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Component
public class LearningPlanPreviewViewAssembler {

    public LearningPlanPreview assemble(
        LearningPlanPlanningContext context,
        LearnerState learnerState,
        PlanCandidateSet candidateSet,
        LlmPlanDecisionResult decision,
        DecisionFallbackLevel fallbackLevel,
        List<String> fallbackReasons
    ) {
        EntryCandidate selectedEntry = findEntry(candidateSet.entries(), decision.selectedConceptId());
        StrategyCandidate selectedStrategy = findStrategy(candidateSet.strategies(), decision.selectedStrategyCode());
        IntensityCandidate selectedIntensity = findIntensity(candidateSet.intensities(), decision.selectedIntensityCode());
        List<PlanTaskPreview> taskPreview = buildTaskPreview(selectedEntry, candidateSet.actionTemplates(), selectedIntensity);
        List<PlanPathNode> pathPreview = buildPathPreview(context, selectedEntry, selectedIntensity);
        List<PlanAlternative> alternatives = buildAlternatives(candidateSet.strategies(), selectedStrategy, decision.alternativeExplanations());
        List<PlanReason> reasons = buildReasons(decision, selectedEntry, selectedStrategy);
        List<String> focuses = buildFocuses(context, selectedEntry, selectedStrategy);
        List<String> nextUnlocks = resolveNextUnlocks(context, selectedEntry);
        List<String> normalizedFallbackReasons = normalizeFallbackReasons(fallbackLevel, fallbackReasons);
        boolean fallbackApplied = fallbackLevel != DecisionFallbackLevel.NONE;
        Integer estimatedMinutes = taskPreview.stream()
            .map(PlanTaskPreview::estimatedMinutes)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .sum();
        if (estimatedMinutes <= 0) {
            estimatedMinutes = selectedIntensity.estimatedMinutes();
        }
        LearningPlanSummary summary = new LearningPlanSummary(
            "先从「" + selectedEntry.conceptName() + "」建立稳定起步",
            selectedEntry.conceptId(),
            selectedEntry.conceptName(),
            selectedIntensity.code(),
            estimatedMinutes,
            pathPreview.size(),
            taskPreview.size(),
            "采用「" + selectedStrategy.label() + "」降低回退风险并保持节奏。",
            safeText(decision.heroReason(), "先做稳健起步，快速拿到新一轮学习证据。"),
            fallbackApplied ? "LOW" : "MEDIUM",
            selectedEntry.conceptName(),
            taskPreview.get(0).title(),
            taskPreview.get(0).estimatedMinutes(),
            safeText(selectedEntry.priority(), "MEDIUM"),
            alternatives,
            decision.evidenceBullets() == null ? List.of() : decision.evidenceBullets().stream().limit(3).toList(),
            nextUnlocks,
            taskPreview.size() > 1 ? taskPreview.get(1).title() : taskPreview.get(0).title(),
            fallbackApplied ? "FALLBACK" : "LLM",
            fallbackApplied,
            normalizedFallbackReasons
        );
        return new LearningPlanPreview(
            summary,
            reasons,
            focuses,
            pathPreview,
            taskPreview,
            resolveAdjustments(selectedStrategy, selectedIntensity)
        );
    }

    private EntryCandidate findEntry(List<EntryCandidate> entries, String conceptId) {
        if (entries == null || entries.isEmpty()) {
            return new EntryCandidate("bootstrap-1", "当前章节起步节点", "上下文不足，先从章节起步节点开始。", 12, "MEDIUM");
        }
        String expected = normalize(conceptId);
        return entries.stream()
            .filter(item -> normalize(item.conceptId()).equals(expected))
            .findFirst()
            .orElse(entries.get(0));
    }

    private StrategyCandidate findStrategy(List<StrategyCandidate> strategies, String strategyCode) {
        if (strategies == null || strategies.isEmpty()) {
            return new StrategyCandidate("FOUNDATION_FIRST", "先补基础", "默认策略。", "短期推进会慢一些。");
        }
        String expected = normalize(strategyCode);
        return strategies.stream()
            .filter(item -> normalize(item.code()).equals(expected))
            .findFirst()
            .orElse(strategies.get(0));
    }

    private IntensityCandidate findIntensity(List<IntensityCandidate> intensities, String intensityCode) {
        if (intensities == null || intensities.isEmpty()) {
            return new IntensityCandidate("STANDARD", "标准节奏", 15, "默认强度。");
        }
        String expected = normalize(intensityCode);
        return intensities.stream()
            .filter(item -> normalize(item.code()).equals(expected))
            .findFirst()
            .orElse(intensities.get(0));
    }

    private List<PlanTaskPreview> buildTaskPreview(
        EntryCandidate selectedEntry,
        List<ActionTemplate> templates,
        IntensityCandidate selectedIntensity
    ) {
        if (templates == null || templates.isEmpty()) {
            return List.of(
                new PlanTaskPreview("STRUCTURE", "建立结构图", "先搭起概念骨架", "画出概念关系并口述主线", "AI 检查关系图并提示缺口", 6),
                new PlanTaskPreview("UNDERSTANDING", "补齐关键理解", "收敛最易混淆点", "用正反例解释概念边界", "AI 切换解释角度", 8),
                new PlanTaskPreview("TRAINING", "做定向训练", "验证回补是否有效", "完成 3-5 个短练习", "AI 对错因做归因", 10),
                new PlanTaskPreview("REFLECTION", "收口复盘", "决定下一轮推进节奏", "总结错因并写下一步", "AI 输出下一步建议", 5)
            );
        }
        List<PlanTaskPreview> result = new ArrayList<>();
        for (ActionTemplate item : templates) {
            Integer estimated = item.estimatedMinutes() == null ? selectedIntensity.estimatedMinutes() : item.estimatedMinutes();
            result.add(new PlanTaskPreview(
                safeText(item.stage(), "STRUCTURE"),
                safeText(item.title(), "学习动作"),
                safeText(item.goal(), "围绕 " + selectedEntry.conceptName() + " 完成当前学习动作"),
                safeText(item.learnerAction(), "先做一轮可执行动作"),
                safeText(item.aiSupport(), "AI 给出下一步建议"),
                Math.max(3, estimated)
            ));
            if (result.size() >= 4) {
                break;
            }
        }
        while (result.size() < 4) {
            result.add(new PlanTaskPreview(
                switch (result.size()) {
                    case 0 -> "STRUCTURE";
                    case 1 -> "UNDERSTANDING";
                    case 2 -> "TRAINING";
                    default -> "REFLECTION";
                },
                switch (result.size()) {
                    case 0 -> "建立结构图";
                    case 1 -> "补齐关键理解";
                    case 2 -> "做定向训练";
                    default -> "收口复盘";
                },
                "围绕 " + selectedEntry.conceptName() + " 完成关键动作",
                "先执行最小可行动作并记录卡点",
                "AI 将根据反馈给出调优建议",
                Math.max(4, selectedIntensity.estimatedMinutes() / 3)
            ));
        }
        return result;
    }

    private List<PlanPathNode> buildPathPreview(
        LearningPlanPlanningContext context,
        EntryCandidate selectedEntry,
        IntensityCandidate selectedIntensity
    ) {
        if (context == null || context.nodes() == null || context.nodes().isEmpty()) {
            return List.of(new PlanPathNode(
                selectedEntry.conceptId(),
                selectedEntry.conceptName(),
                2,
                50,
                "LEARNING",
                true,
                selectedIntensity.estimatedMinutes(),
                "RECOMMENDED_START"
            ));
        }
        List<LearningPlanContextNode> sortedNodes = context.nodes().stream()
            .sorted(Comparator.comparing(LearningPlanContextNode::orderNo, Comparator.nullsLast(Integer::compareTo)))
            .toList();
        List<PlanPathNode> result = new ArrayList<>();
        for (LearningPlanContextNode node : sortedNodes) {
            boolean selected = normalize(node.planNodeId()).equals(normalize(selectedEntry.conceptId()));
            result.add(new PlanPathNode(
                node.planNodeId(),
                node.nodeName(),
                resolveDifficulty(node.mastery()),
                node.mastery(),
                resolvePathStatus(node.mastery()),
                selected,
                selected ? selectedIntensity.estimatedMinutes() : Math.max(6, selectedIntensity.estimatedMinutes() - 2),
                selected ? "RECOMMENDED_START" : "FOLLOW_UP"
            ));
        }
        return result;
    }

    private List<PlanAlternative> buildAlternatives(
        List<StrategyCandidate> strategies,
        StrategyCandidate selectedStrategy,
        List<AlternativeExplanation> llmAlternatives
    ) {
        List<PlanAlternative> result = new ArrayList<>();
        Set<String> selectedCodes = Set.of(normalize(selectedStrategy.code()));
        if (llmAlternatives != null) {
            for (AlternativeExplanation item : llmAlternatives) {
                if (item == null || selectedCodes.contains(normalize(item.strategyCode()))) {
                    continue;
                }
                result.add(new PlanAlternative(
                    safeText(item.strategyCode(), "UNKNOWN"),
                    safeText(item.label(), "备选策略"),
                    safeText(item.reason(), "当前不是最优先选项。"),
                    safeText(item.tradeoff(), "存在额外切换成本。")
                ));
            }
        }
        if (strategies != null) {
            for (StrategyCandidate item : strategies) {
                if (selectedCodes.contains(normalize(item.code()))) {
                    continue;
                }
                boolean exists = result.stream().anyMatch(alt -> normalize(alt.strategy()).equals(normalize(item.code())));
                if (exists) {
                    continue;
                }
                result.add(new PlanAlternative(
                    item.code(),
                    item.label(),
                    safeText(item.description(), "当前不是最优先选项。"),
                    safeText(item.tradeoff(), "存在额外切换成本。")
                ));
            }
        }
        return result.stream().limit(3).toList();
    }

    private List<PlanReason> buildReasons(
        LlmPlanDecisionResult decision,
        EntryCandidate selectedEntry,
        StrategyCandidate selectedStrategy
    ) {
        List<PlanReason> reasons = new ArrayList<>();
        reasons.add(new PlanReason(
            "WEAKNESS_MATCH",
            "优先处理当前收益最高节点",
            safeText(selectedEntry.reason(), "该节点当前收益最高。")
        ));
        reasons.add(new PlanReason(
            "STRATEGY_MATCH",
            "策略与当前状态匹配",
            safeText(decision.currentStateSummary(), "当前学习状态需要稳健起步。")
        ));
        reasons.add(new PlanReason(
            "RISK_CONTROL",
            "风险控制优先",
            safeText(decision.heroReason(), "先稳住关键节点可降低后续反复回退风险。")
        ));
        reasons.add(new PlanReason(
            "TRADEOFF",
            "策略权衡说明",
            safeText(selectedStrategy.tradeoff(), "该策略在稳定性与速度之间做了平衡。")
        ));
        return reasons;
    }

    private List<String> buildFocuses(
        LearningPlanPlanningContext context,
        EntryCandidate selectedEntry,
        StrategyCandidate selectedStrategy
    ) {
        List<String> focuses = new ArrayList<>();
        focuses.add(selectedEntry.conceptName());
        focuses.add("策略：" + selectedStrategy.label());
        if (context != null && context.weakPointLabels() != null) {
            focuses.addAll(context.weakPointLabels().stream()
                .filter(item -> item != null && !item.isBlank())
                .limit(2)
                .toList());
        }
        return focuses.stream().distinct().limit(4).toList();
    }

    private List<String> resolveNextUnlocks(LearningPlanPlanningContext context, EntryCandidate selectedEntry) {
        if (context == null || context.nodes() == null || context.nodes().isEmpty()) {
            return List.of();
        }
        LearningPlanContextNode selectedNode = context.nodes().stream()
            .filter(item -> normalize(item.planNodeId()).equals(normalize(selectedEntry.conceptId())))
            .findFirst()
            .orElse(null);
        int selectedOrder = selectedNode == null || selectedNode.orderNo() == null ? Integer.MIN_VALUE : selectedNode.orderNo();
        return context.nodes().stream()
            .filter(item -> item.orderNo() != null && item.orderNo() > selectedOrder)
            .sorted(Comparator.comparing(LearningPlanContextNode::orderNo))
            .map(LearningPlanContextNode::nodeName)
            .filter(item -> item != null && !item.isBlank())
            .limit(2)
            .toList();
    }

    private List<String> normalizeFallbackReasons(DecisionFallbackLevel fallbackLevel, List<String> fallbackReasons) {
        if (fallbackLevel == null || fallbackLevel == DecisionFallbackLevel.NONE) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        result.add("fallback_level:" + fallbackLevel.name());
        if (fallbackReasons != null) {
            result.addAll(fallbackReasons.stream()
                .filter(item -> item != null && !item.isBlank())
                .toList());
        }
        return result;
    }

    private PlanAdjustments resolveAdjustments(StrategyCandidate selectedStrategy, IntensityCandidate selectedIntensity) {
        String strategyCode = normalize(selectedStrategy.code());
        String learningMode = switch (strategyCode) {
            case "PRACTICE_FIRST" -> "PRACTICE_DRIVEN";
            case "FAST_TRACK", "COMPRESSED_10_MIN" -> "MIXED";
            default -> "LEARN_THEN_PRACTICE";
        };
        boolean preferPrerequisite = !"FAST_TRACK".equals(strategyCode);
        return new PlanAdjustments(selectedIntensity.code(), learningMode, preferPrerequisite).normalized();
    }

    private int resolveDifficulty(Integer mastery) {
        if (mastery == null) {
            return 2;
        }
        if (mastery < 50) {
            return 1;
        }
        if (mastery >= 80) {
            return 4;
        }
        return 2;
    }

    private String resolvePathStatus(Integer mastery) {
        if (mastery == null) {
            return "NEW";
        }
        if (mastery < 50) {
            return "WEAK";
        }
        if (mastery < 80) {
            return "PARTIAL";
        }
        return "STABLE";
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
