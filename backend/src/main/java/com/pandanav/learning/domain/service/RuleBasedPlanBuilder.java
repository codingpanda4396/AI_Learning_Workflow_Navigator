package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.model.LearningPlanContextNode;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.LearningPlanPreview;
import com.pandanav.learning.domain.model.LearningPlanSummary;
import com.pandanav.learning.domain.model.PlanAlternative;
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
        List<PlanTaskPreview> taskPreview = buildTaskPreview(
            startNode.nodeName(),
            context.adjustments().intensity(),
            context.requestedTimeBudgetMinutes()
        );
        PlanTaskPreview currentTask = taskPreview.get(0);

        int totalMinutes = selected.stream().mapToInt(node -> estimateNodeMinutes(node, context)).sum();
        String headline = startNode.prerequisiteNodeIds().isEmpty()
            ? "建议先从 " + startNode.nodeName() + " 建立稳定起点，再推进目标路径。"
            : "建议先补齐 " + startNode.nodeName() + " 这段前置依赖，再进入后续目标节点。";

        return new LearningPlanPreview(
            new LearningPlanSummary(
                headline,
                startNode.planNodeId(),
                startNode.nodeName(),
                context.adjustments().intensity(),
                totalMinutes,
                selected.size(),
                4,
                startNode.prerequisiteNodeIds().isEmpty()
                    ? "先把当前最关键的薄弱点补稳，再继续往后推进。"
                    : "先补前置依赖，再推进后续学习会更顺。",
                startNode.mastery() != null && startNode.mastery() < 50
                    ? "当前节点掌握度偏低，现在不补，后续训练会持续受阻。"
                    : "当前节点正处在最适合补齐且能立即带动后续推进的位置。",
                resolveConfidence(context, startNode),
                startNode.nodeName(),
                currentTask.title(),
                currentTask.estimatedMinutes(),
                resolvePriority(startNode),
                buildAlternatives(startNode),
                buildBenefits(selected, startNode),
                buildNextUnlocks(selected),
                selected.size() > 1 ? "完成后进入 " + selected.get(1).nodeName() : "完成后进入定向训练验证",
                "FALLBACK",
                true,
                List.of("RULE_BASED_DECISION")
            ),
            buildReasons(context, startNode, selected),
            buildFocuses(context, selected),
            buildPathPreview(context, selected, startNode.planNodeId()),
            taskPreview,
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
            "WEAKNESS_MATCH",
            "先补 " + startNode.nodeName(),
            "当前推荐节点的掌握度较低，同时又直接连接后续目标，先补这里能最快减少后续反复回退。"
        ));
        if (!startNode.prerequisiteNodeIds().isEmpty()) {
            reasons.add(new PlanReason(
                "DEPENDENCY",
                "前置依赖要先打通",
                "当前推荐节点和后续目标之间存在依赖关系，先补这里能减少后面整段路径的理解阻塞。"
            ));
        }
        reasons.add(new PlanReason(
            "EFFICIENCY",
            "这一步的投入产出比最高",
            "系统结合最近得分、错误标签和节点掌握度后判断，先处理这一步最能同时提升理解效率和后续推进速度。"
        ));
        if (selected.size() > 1) {
            LearningPlanContextNode second = selected.get(1);
            reasons.add(new PlanReason(
                "RISK_CONTROL",
                "现在不建议直接跳到 " + second.nodeName(),
                "如果跳过当前节点，进入 " + second.nodeName() + " 时更容易在理解和训练阶段同时卡住。"
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

    private List<PlanTaskPreview> buildTaskPreview(String nodeName, String intensity, Integer timeBudgetMinutes) {
        int multiplier = switch (intensity) {
            case "LIGHT" -> 0;
            case "INTENSIVE" -> 2;
            default -> 1;
        };
        int cap = timeBudgetMinutes != null && timeBudgetMinutes > 0 ? timeBudgetMinutes : Integer.MAX_VALUE;
        return List.of(
            new PlanTaskPreview("STRUCTURE", "建立整体框架", "明确 " + nodeName + " 在整条路径中的位置", "阅读结构化引导并画出本轮学习框架", "AI Tutor 会先把关键概念关系压缩成一条可执行主线", Math.min(6 + multiplier, cap)),
            new PlanTaskPreview("UNDERSTANDING", "补齐关键理解", "搞清最容易断开的概念连接", "跟着例子定位自己最容易混淆的环节", "AI 会根据你的薄弱标签改写解释角度，减少空泛讲解", Math.min(8 + multiplier, cap)),
            new PlanTaskPreview("TRAINING", "做针对性训练", "验证这次补强是否真的生效", "完成少量但更贴近当前卡点的训练题", "AI 会把错因回收到反馈里，避免只给分不解释", Math.min(12 + multiplier, cap)),
            new PlanTaskPreview("REFLECTION", "收口并决定下一步", "判断本轮是否可以继续推进", "复盘错误模式并确认是否进入下一个节点", "AI 会基于本轮表现给出下一步推进或回补建议", Math.min(5 + multiplier, cap))
        );
    }

    private String resolveConfidence(LearningPlanPlanningContext context, LearningPlanContextNode startNode) {
        if (!context.weakPointLabels().isEmpty() && !context.recentScores().isEmpty() && startNode.attemptCount() >= 1) {
            return "HIGH";
        }
        if (!context.weakPointLabels().isEmpty() || !context.recentScores().isEmpty()) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String resolvePriority(LearningPlanContextNode startNode) {
        return startNode.mastery() != null && startNode.mastery() < 50 ? "HIGH" : "MEDIUM";
    }

    private List<PlanAlternative> buildAlternatives(LearningPlanContextNode startNode) {
        return List.of(
            new PlanAlternative("FAST_TRACK", "快速推进", "直接压缩前置解释，尽快进入关键训练。", "推进更快，但更容易在依赖点反复卡住。"),
            new PlanAlternative("FOUNDATION_FIRST", "先补基础", "先把 " + startNode.nodeName() + " 相关前置理解补齐，再进入后续节点。", "更稳，但短期成就感会慢一点。"),
            new PlanAlternative("PRACTICE_FIRST", "先做题带学", "先用练习暴露问题，再回补解释。", "反馈更直接，但概念空洞时会增加挫败感。"),
            new PlanAlternative("COMPRESSED_10_MIN", "10分钟压缩版", "把当前步骤压缩成最小可执行动作，适合时间很少时继续推进。", "覆盖面更窄，后续通常还要补课。")
        );
    }

    private List<String> buildBenefits(List<LearningPlanContextNode> selected, LearningPlanContextNode startNode) {
        List<String> benefits = new ArrayList<>();
        benefits.add("先稳住 " + startNode.nodeName() + "，可以减少后续重复回退。");
        if (selected.size() > 1) {
            benefits.add("完成后进入 " + selected.get(1).nodeName() + " 会更顺畅。");
        }
        benefits.add("当前推荐兼顾薄弱点修补和整体推进效率。");
        return benefits.stream().limit(3).toList();
    }

    private List<String> buildNextUnlocks(List<LearningPlanContextNode> selected) {
        if (selected.size() <= 1) {
            return List.of("进入下一轮针对性训练", "确认是否可以直接推进后续节点");
        }
        List<String> unlocks = new ArrayList<>();
        for (int i = 1; i < selected.size(); i++) {
            unlocks.add("解锁 " + selected.get(i).nodeName() + " 的连续学习");
        }
        unlocks.add("更容易识别后续训练中的真实薄弱点");
        return unlocks.stream().limit(3).toList();
    }
}
