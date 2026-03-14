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
        List<ActionTemplate> actions = buildActionTemplates(
            context,
            learnerState,
            entries.isEmpty() ? "当前关键概念" : entries.get(0).conceptName()
        );
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

    private List<ActionTemplate> buildActionTemplates(
        LearningPlanPlanningContext context,
        LearnerState learnerState,
        String conceptName
    ) {
        GapProfile profile = resolveGapProfile(context, learnerState);
        return switch (profile) {
            case FOUNDATION_WEAK -> List.of(
                new ActionTemplate("STRUCTURE", "补前置框架", "补齐 " + conceptName + " 的前置依赖", "画出依赖图并标出断点，至少标注 2 个前置关系", "AI 会检查依赖关系并提示遗漏节点", 7),
                new ActionTemplate("UNDERSTANDING", "校准概念边界", "避免基础概念混淆", "使用 1 个正例 + 1 个反例解释核心定义", "AI 会对定义偏差做逐句纠偏", 9),
                new ActionTemplate("TRAINING", "低阶稳态训练", "验证基础补齐是否生效", "完成 3 题基础练习，正确率达到 2/3", "AI 会给出错因和最小修正动作", 10),
                new ActionTemplate("REFLECTION", "确认可推进性", "判断是否进入下一节点", "总结仍不清楚的 1 个点并决定继续补齐或推进", "AI 会给出继续补齐或推进建议", 5)
            );
            case RELATION_WEAK -> List.of(
                new ActionTemplate("STRUCTURE", "串联关系链", "明确 " + conceptName + " 与上下游节点关系", "写出从前置到当前节点的三步关系链", "AI 会标记关系链中的逻辑跳步", 6),
                new ActionTemplate("UNDERSTANDING", "修复连接点", "补齐最容易断链的连接环节", "用自己的话解释两个关键连接点并给例子", "AI 会检查连接是否闭环", 8),
                new ActionTemplate("TRAINING", "迁移验证训练", "验证关系链能否迁移到题目场景", "完成 4 题连接型练习，至少 3 题能说明推理路径", "AI 会检查推理路径是否连贯", 12),
                new ActionTemplate("REFLECTION", "关系回放", "避免再次出现断链错误", "复盘 1 次错误链路并写出替代思路", "AI 会给出下一轮重点关系节点", 5)
            );
            case CODE_MAPPING_WEAK -> List.of(
                new ActionTemplate("STRUCTURE", "代码映射拆解", "把概念映射到代码骨架", "列出 3 个概念到代码位置的映射", "AI 会检查映射是否对应真实执行路径", 6),
                new ActionTemplate("UNDERSTANDING", "语义到实现转换", "建立概念与实现动作的一一对应", "解释每段关键代码在概念层的作用", "AI 会指出语义和实现不一致的位置", 8),
                new ActionTemplate("TRAINING", "定向改错", "降低“会概念不会写代码”问题", "完成 3 个小改错任务并说明修改理由", "AI 会逐条评估改错理由是否充分", 13),
                new ActionTemplate("REFLECTION", "可复用模板沉淀", "沉淀下一轮可复用编码步骤", "输出 1 份最小实现模板并标注易错点", "AI 会给模板补充检查清单", 5)
            );
            case STABILITY_WEAK -> List.of(
                new ActionTemplate("STRUCTURE", "最小可执行起步", "先确保本轮有稳定产出", "定义 10 分钟内可完成的最小动作", "AI 会收敛到低负担起步动作", 5),
                new ActionTemplate("UNDERSTANDING", "高频错因收敛", "先压制反复出现的错因", "针对高频错误标签写 2 条规避规则", "AI 会校验规避规则是否可执行", 7),
                new ActionTemplate("TRAINING", "短回路训练", "快速拿到新反馈并稳定节奏", "完成 2-3 题短练，单题复盘不超过 2 分钟", "AI 会基于表现动态调节难度", 9),
                new ActionTemplate("REFLECTION", "节奏复盘", "决定下一轮是提速还是继续稳态", "记录本轮压力点并选择下一轮节奏", "AI 会给出提速/稳态建议", 4)
            );
        };
    }

    private GapProfile resolveGapProfile(LearningPlanPlanningContext context, LearnerState learnerState) {
        int relationTagCount = countContains(context.recentErrorTags(), "LINK", "DEPENDENCY", "RELATION", "CONFUSION");
        int codeTagCount = countContains(context.recentErrorTags(), "CODE", "IMPLEMENT", "TRANSFER", "PRACTICE");
        int avgScore = context.recentScores() == null || context.recentScores().isEmpty()
            ? 100
            : (int) Math.round(context.recentScores().stream().mapToInt(Integer::intValue).average().orElse(100D));
        if (learnerState.currentBlockType() == CurrentBlockType.FOUNDATION_GAP) {
            return GapProfile.FOUNDATION_WEAK;
        }
        if (relationTagCount >= 2 || learnerState.currentBlockType() == CurrentBlockType.CONCEPT_LINK_GAP) {
            return GapProfile.RELATION_WEAK;
        }
        if (codeTagCount >= 2 || learnerState.currentBlockType() == CurrentBlockType.APPLICATION_GAP) {
            return GapProfile.CODE_MAPPING_WEAK;
        }
        if (avgScore < 60 || learnerState.currentBlockType() == CurrentBlockType.MIXED || learnerState.currentBlockType() == CurrentBlockType.EVIDENCE_LOW) {
            return GapProfile.STABILITY_WEAK;
        }
        return GapProfile.RELATION_WEAK;
    }

    private int countContains(List<String> values, String... targets) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String upper = value.toUpperCase(Locale.ROOT);
            for (String token : targets) {
                if (upper.contains(token)) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    private enum GapProfile {
        FOUNDATION_WEAK,
        RELATION_WEAK,
        CODE_MAPPING_WEAK,
        STABILITY_WEAK
    }

    private record EntryRank(LearningPlanContextNode node, int score) {
    }
}
