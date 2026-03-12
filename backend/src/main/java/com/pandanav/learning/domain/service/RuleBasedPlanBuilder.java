package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.model.LearningPlanContextNode;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.LearningPlanPreview;
import com.pandanav.learning.domain.model.LearningPlanSummary;
import com.pandanav.learning.domain.model.PlanPathNode;
import com.pandanav.learning.domain.model.PlanReason;
import com.pandanav.learning.domain.model.PlanTaskPreview;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RuleBasedPlanBuilder {

    public LearningPlanPreview build(LearningPlanPlanningContext context) {
        List<LearningPlanContextNode> nodes = context.nodes();
        int startIndex = pickStartIndex(nodes, context.adjustments().preferPrerequisite());
        int endExclusive = Math.min(nodes.size(), startIndex + resolveNodeWindow(context.adjustments().intensity()));
        List<LearningPlanContextNode> selected = nodes.subList(startIndex, endExclusive);
        LearningPlanContextNode startNode = selected.get(0);

        int totalMinutes = selected.stream().mapToInt(node -> estimateNodeMinutes(node, context)).sum();
        String headline = startNode.prerequisiteNodeIds().isEmpty()
            ? "结合你当前的掌握度，建议先从 " + startNode.nodeName() + " 建立稳定起点，再推进目标路径。"
            : "结合你当前的基础，建议先补齐 " + startNode.nodeName() + " 这一连接环节，再进入后续目标节点。";

        return new LearningPlanPreview(
            new LearningPlanSummary(
                headline,
                startNode.planNodeId(),
                startNode.nodeName(),
                context.adjustments().intensity(),
                totalMinutes,
                selected.size(),
                4
            ),
            buildReasons(context, startNode, selected),
            buildFocuses(context, selected),
            buildPathPreview(context, selected, startNode.planNodeId()),
            buildTaskPreview(startNode.nodeName(), context.adjustments().intensity()),
            context.adjustments()
        );
    }

    private int pickStartIndex(List<LearningPlanContextNode> nodes, boolean preferPrerequisite) {
        for (int i = 0; i < nodes.size(); i++) {
            LearningPlanContextNode node = nodes.get(i);
            boolean weak = node.mastery() < 65 || node.attemptCount() >= 2 || !node.weakReasons().isEmpty();
            if (!weak) {
                continue;
            }
            if (preferPrerequisite && !node.prerequisiteNodeIds().isEmpty() && i > 0) {
                return i - 1;
            }
            return i;
        }
        return 0;
    }

    private int resolveNodeWindow(String intensity) {
        return switch (intensity) {
            case "LIGHT" -> 2;
            case "INTENSIVE" -> 4;
            default -> 3;
        };
    }

    private int estimateNodeMinutes(LearningPlanContextNode node, LearningPlanPlanningContext context) {
        int base = switch (context.adjustments().intensity()) {
            case "LIGHT" -> 14;
            case "INTENSIVE" -> 22;
            default -> 18;
        };
        if (node.mastery() < 50) {
            base += 5;
        }
        if (!node.weakReasons().isEmpty()) {
            base += 3;
        }
        return base;
    }

    private List<PlanReason> buildReasons(
        LearningPlanPlanningContext context,
        LearningPlanContextNode startNode,
        List<LearningPlanContextNode> selected
    ) {
        List<PlanReason> reasons = new ArrayList<>();
        reasons.add(new PlanReason(
            "START_POINT",
            "建议从 " + startNode.nodeName() + " 开始",
            "你在该节点的当前掌握度约为 " + startNode.mastery()
                + "，且它与后续目标节点衔接最紧，先从这里补齐更能避免后面反复回退。"
        ));
        if (selected.size() > 1) {
            LearningPlanContextNode second = selected.get(1);
            reasons.add(new PlanReason(
                "RISK",
                "暂不建议直接跳到 " + second.nodeName(),
                "从你最近的薄弱表现看，如果跳过前置连接环节，进入 " + second.nodeName() + " 时更容易在理解和训练阶段同时卡住。"
            ));
        }
        if (!context.weakPointLabels().isEmpty()) {
            reasons.add(new PlanReason(
                "EVIDENCE",
                "本轮规划参考了你的历史学习证据",
                "系统结合了你的弱项标签、最近得分和节点掌握度，因此这份方案不是通用模板，而是围绕你当前最可能掉链子的环节收口。"
            ));
        }
        return reasons;
    }

    private List<String> buildFocuses(LearningPlanPlanningContext context, List<LearningPlanContextNode> selected) {
        List<String> focuses = new ArrayList<>();
        focuses.add("先稳住 " + selected.get(0).nodeName() + " 的理解链路");
        if (selected.size() > 1) {
            focuses.add("再把 " + selected.get(1).nodeName() + " 和前置节点串起来");
        }
        if (!context.recentErrorTags().isEmpty()) {
            focuses.add("针对近期高频错误标签做一轮定向训练");
        }
        return focuses.stream().limit(3).toList();
    }

    private List<PlanPathNode> buildPathPreview(
        LearningPlanPlanningContext context,
        List<LearningPlanContextNode> selected,
        String startNodeId
    ) {
        return selected.stream()
            .map(node -> new PlanPathNode(
                node.planNodeId(),
                node.nodeName(),
                node.difficulty(),
                node.mastery(),
                node.mastery() >= 80 ? "READY" : "LEARNING",
                node.planNodeId().equals(startNodeId),
                estimateNodeMinutes(node, context),
                node.prerequisiteNodeIds().isEmpty() ? "当前主攻" : "前置核心"
            ))
            .toList();
    }

    private List<PlanTaskPreview> buildTaskPreview(String nodeName, String intensity) {
        int multiplier = switch (intensity) {
            case "LIGHT" -> 0;
            case "INTENSIVE" -> 2;
            default -> 1;
        };
        return List.of(
            new PlanTaskPreview("STRUCTURE", "建立整体框架", "明确 " + nodeName + " 在整条路径中的位置", "阅读结构化引导并画出本轮学习框架", "AI Tutor 会先帮你把关键概念关系压缩成一条可执行主线", 6 + multiplier),
            new PlanTaskPreview("UNDERSTANDING", "补齐关键理解", "搞清最容易断开的概念连接", "跟着例子定位自己最容易混淆的环节", "AI 会根据你的薄弱标签改写解释角度，减少空泛讲解", 8 + multiplier),
            new PlanTaskPreview("TRAINING", "做针对性训练", "验证这次补强是否真的生效", "完成少量但更贴近你当前卡点的训练题", "AI 会把错因回收到反馈里，避免只给分不解释", 12 + multiplier),
            new PlanTaskPreview("REFLECTION", "收口并决定下一步", "判断本轮是否可以继续推进", "复盘错误模式并确认是否进入下个节点", "AI 会基于本轮表现给出下一步推进或回补建议", 5 + multiplier)
        );
    }
}
