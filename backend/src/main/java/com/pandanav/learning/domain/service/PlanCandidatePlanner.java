package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.enums.CurrentBlockType;
import com.pandanav.learning.domain.enums.FoundationStatus;
import com.pandanav.learning.domain.model.ActionTemplate;
import com.pandanav.learning.domain.model.EntryCandidate;
import com.pandanav.learning.domain.model.IntensityCandidate;
import com.pandanav.learning.domain.model.LearnerState;
import com.pandanav.learning.domain.model.LearningPlanContextNode;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.PlanCandidateSet;
import com.pandanav.learning.domain.model.StrategyCandidate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class PlanCandidatePlanner {

    public PlanCandidateSet plan(LearningPlanPlanningContext context, LearnerState learnerState) {
        List<EntryCandidate> entries = buildEntryCandidates(context, learnerState);
        List<StrategyCandidate> strategies = buildStrategyCandidates(learnerState, context.requestedStrategy());
        List<IntensityCandidate> intensities = buildIntensityCandidates(context);
        List<ActionTemplate> actions = buildActionTemplates(entries.isEmpty() ? "当前关键概念" : entries.get(0).conceptName());
        return new PlanCandidateSet(entries, strategies, intensities, actions);
    }

    private List<EntryCandidate> buildEntryCandidates(LearningPlanPlanningContext context, LearnerState learnerState) {
        List<LearningPlanContextNode> nodes = context.nodes() == null ? List.of() : context.nodes();
        if (nodes.isEmpty()) {
            return List.of(
                new EntryCandidate("bootstrap-1", "当前章节起步节点", "当前可用上下文较少，先从章节起步节点建立稳定节奏。", 12, "MEDIUM"),
                new EntryCandidate("bootstrap-2", "关键概念回顾", "先做一轮概念回顾，确保后续训练不因前置缺口卡住。", 10, "LOW")
            );
        }
        List<EntryRank> ranked = new ArrayList<>();
        for (LearningPlanContextNode node : nodes) {
            int score = 0;
            int mastery = node.mastery() == null ? 55 : node.mastery();
            int attempts = node.attemptCount() == null ? 0 : node.attemptCount();
            score += Math.max(0, 70 - mastery);
            score += Math.min(6, attempts * 2);
            score += node.weakReasons() == null ? 0 : Math.min(6, node.weakReasons().size() * 2);
            if (node.orderNo() != null && node.orderNo() <= 2) {
                score += learnerState.foundationStatus() == FoundationStatus.WEAK ? 6 : 2;
            }
            if (node.prerequisiteNodeIds() != null && !node.prerequisiteNodeIds().isEmpty()) {
                score += learnerState.currentBlockType() == CurrentBlockType.FOUNDATION_GAP ? 3 : 1;
            }
            ranked.add(new EntryRank(node, score));
        }

        return ranked.stream()
            .sorted(Comparator.comparingInt(EntryRank::score).reversed())
            .limit(5)
            .map(item -> toEntryCandidate(item.node(), learnerState))
            .limit(Math.max(2, Math.min(5, ranked.size())))
            .toList();
    }

    private EntryCandidate toEntryCandidate(LearningPlanContextNode node, LearnerState learnerState) {
        int mastery = node.mastery() == null ? 55 : node.mastery();
        int estimatedMinutes = switch (learnerState.evidenceLevel()) {
            case HIGH -> mastery < 50 ? 14 : 10;
            case MEDIUM -> mastery < 50 ? 16 : 12;
            case LOW -> mastery < 50 ? 18 : 14;
        };
        String reason = buildEntryReason(node, learnerState);
        String priority = mastery < 50 ? "HIGH" : (mastery < 70 ? "MEDIUM" : "LOW");
        return new EntryCandidate(node.planNodeId(), node.nodeName(), reason, estimatedMinutes, priority);
    }

    private String buildEntryReason(LearningPlanContextNode node, LearnerState learnerState) {
        if (node.prerequisiteNodeIds() != null && !node.prerequisiteNodeIds().isEmpty()) {
            return "该节点连接后续依赖链，先补齐可降低整段路径反复回退风险。";
        }
        if (learnerState.currentBlockType() == CurrentBlockType.APPLICATION_GAP) {
            return "该节点可直接承接练习反馈，适合先做应用层收敛。";
        }
        return "该节点当前收益最高，先处理可快速稳住后续学习节奏。";
    }

    private List<StrategyCandidate> buildStrategyCandidates(LearnerState learnerState, String requestedStrategy) {
        List<StrategyCandidate> result = new ArrayList<>();
        result.add(new StrategyCandidate(
            "FOUNDATION_FIRST",
            "先补基础",
            "先把前置依赖打通，再推进后续内容，降低反复卡住风险。",
            "短期推进速度略慢，但稳定性更高。"
        ));
        result.add(new StrategyCandidate(
            "PRACTICE_FIRST",
            "先练后学",
            "先通过小练习暴露关键盲点，再按盲点回补解释。",
            "反馈更直接，但基础薄弱时挫败感会升高。"
        ));
        result.add(new StrategyCandidate(
            "FAST_TRACK",
            "快速推进",
            "压缩解释时长，优先推进到目标训练环节。",
            "推进快，但在依赖点更容易回退。"
        ));
        if (requestedStrategy != null && !requestedStrategy.isBlank()) {
            String normalized = requestedStrategy.trim().toUpperCase(Locale.ROOT);
            if ("COMPRESSED_10_MIN".equals(normalized)) {
                result.add(new StrategyCandidate(
                    "COMPRESSED_10_MIN",
                    "10分钟压缩版",
                    "最小可执行动作优先，适合时间极少时保持连续性。",
                    "覆盖面更窄，通常需要后续补齐。"
                ));
            }
        }
        if (learnerState.currentBlockType() == CurrentBlockType.EVIDENCE_LOW && result.stream().noneMatch(item -> "COMPRESSED_10_MIN".equals(item.code()))) {
            result.add(new StrategyCandidate(
                "COMPRESSED_10_MIN",
                "10分钟压缩版",
                "证据不足时先做低风险起步，快速拿到第一轮反馈。",
                "信息量偏少，需要通过下一轮数据继续校准。"
            ));
        }
        return result.stream().limit(4).toList();
    }

    private List<IntensityCandidate> buildIntensityCandidates(LearningPlanPlanningContext context) {
        int budget = context.requestedTimeBudgetMinutes() == null ? 0 : context.requestedTimeBudgetMinutes();
        List<IntensityCandidate> result = new ArrayList<>();
        result.add(new IntensityCandidate("LIGHT", "轻量节奏", budget > 0 ? Math.min(12, budget) : 10, "适合时间碎片化或需要降低压力时使用。"));
        result.add(new IntensityCandidate("STANDARD", "标准节奏", budget > 0 ? Math.min(18, budget) : 15, "兼顾理解与训练的默认节奏。"));
        result.add(new IntensityCandidate("INTENSIVE", "强化节奏", budget > 0 ? Math.min(26, Math.max(budget, 16)) : 22, "适合冲刺阶段，单位时间推进更快。"));
        return result;
    }

    private List<ActionTemplate> buildActionTemplates(String conceptName) {
        return List.of(
            new ActionTemplate("STRUCTURE", "建立结构图", "明确 " + conceptName + " 在章节中的依赖位置", "先画出概念关系图并口述主线", "AI 将对关系图做缺口提示", 6),
            new ActionTemplate("UNDERSTANDING", "补齐关键理解", "搞清最容易混淆的连接点", "用一正一反两个例子解释概念边界", "AI 会基于错误标签切换解释角度", 8),
            new ActionTemplate("TRAINING", "做定向训练", "验证当前回补是否生效", "完成 3-5 个贴近当前卡点的短练习", "AI 会给出错因归因和下一题建议", 12),
            new ActionTemplate("REFLECTION", "收口与决策", "判断是否可进入下一节点", "复盘本轮错误模式并给出下一步选择", "AI 将输出推进或回补建议", 5)
        );
    }

    private record EntryRank(LearningPlanContextNode node, int score) {
    }
}
