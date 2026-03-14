package com.pandanav.learning.application.service.learningplan;

import com.pandanav.learning.api.dto.plan.PlanPriorityNodeResponse;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.LearningPlanPreview;
import com.pandanav.learning.domain.model.LearningPlanSummary;
import com.pandanav.learning.domain.model.PlanPathNode;
import com.pandanav.learning.domain.model.PlanReason;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class LearningPlanExplanationAssembler {

    public PlanExplanation assemble(LearningPlanPreview preview, LearningPlanPlanningContext context) {
        LearningPlanSummary summary = preview == null ? null : preview.summary();
        List<PlanReason> reasons = preview == null || preview.reasons() == null ? List.of() : preview.reasons();
        List<PlanPathNode> pathNodes = preview == null || preview.pathPreview() == null ? List.of() : preview.pathPreview();

        String startNodeName = summary == null || summary.recommendedStartNodeName() == null || summary.recommendedStartNodeName().isBlank()
            ? "当前推荐起点"
            : summary.recommendedStartNodeName();
        List<String> keyWeaknesses = collectKeyWeaknesses(context, pathNodes);
        String whyStartHere = buildWhyStartHere(startNodeName, keyWeaknesses, reasons);
        List<PlanPriorityNodeResponse> priorityNodes = buildPriorityNodes(pathNodes, startNodeName, keyWeaknesses);

        return new PlanExplanation(whyStartHere, keyWeaknesses, priorityNodes);
    }

    private List<String> collectKeyWeaknesses(LearningPlanPlanningContext context, List<PlanPathNode> pathNodes) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        if (context != null) {
            addValues(ordered, context.weakPointLabels(), 4);
            addValues(ordered, context.recentErrorTags(), 4);
            if (context.nodes() != null) {
                context.nodes().forEach(node -> addValues(ordered, node.weakReasons(), 4));
            }
        }
        if (ordered.isEmpty()) {
            for (PlanPathNode node : pathNodes) {
                if (node != null && node.reasonTag() != null && !node.reasonTag().isBlank()) {
                    ordered.add(node.reasonTag().trim());
                }
                if (ordered.size() >= 4) {
                    break;
                }
            }
        }
        if (ordered.isEmpty()) {
            ordered.add("基础薄弱点仍需在学习过程中持续识别");
        }
        return ordered.stream().limit(4).toList();
    }

    private String buildWhyStartHere(String startNodeName, List<String> keyWeaknesses, List<PlanReason> reasons) {
        String weaknessText = keyWeaknesses.isEmpty() ? "关键薄弱点待补齐" : keyWeaknesses.get(0);
        String reasonText = reasons.stream()
            .map(PlanReason::description)
            .filter(item -> item != null && !item.isBlank())
            .findFirst()
            .orElse("");
        if (reasonText.isBlank()) {
            return "系统建议从「" + startNodeName + "」开始，因为当前最需要优先补强的是「" + weaknessText + "」。";
        }
        return "系统建议从「" + startNodeName + "」开始，原因是「" + weaknessText + "」会影响后续推进；" + reasonText;
    }

    private List<PlanPriorityNodeResponse> buildPriorityNodes(
        List<PlanPathNode> pathNodes,
        String startNodeName,
        List<String> keyWeaknesses
    ) {
        List<PlanPriorityNodeResponse> nodes = new ArrayList<>();
        for (PlanPathNode node : pathNodes) {
            if (node == null) {
                continue;
            }
            if (nodes.size() >= 3) {
                break;
            }
            String reason = buildPriorityReason(node, keyWeaknesses);
            nodes.add(new PlanPriorityNodeResponse(
                fallbackText(node.nodeId(), "unknown-node"),
                fallbackText(node.nodeName(), "未命名节点"),
                reason
            ));
        }
        if (nodes.isEmpty()) {
            nodes.add(new PlanPriorityNodeResponse(
                "start-node",
                startNodeName,
                "这是当前最能稳定推进学习路径的起点。"
            ));
        }
        return nodes;
    }

    private String buildPriorityReason(PlanPathNode node, List<String> keyWeaknesses) {
        if (Boolean.TRUE.equals(node.isRecommendedStart())) {
            return "这是当前最影响后续学习推进的起点。";
        }
        String tag = fallbackText(node.reasonTag(), "");
        if (!tag.isBlank()) {
            return "该节点与「" + tag + "」直接相关，适合作为优先补强项。";
        }
        if (node.mastery() != null && node.mastery() < 50) {
            return "该节点当前掌握度偏低，需要优先巩固。";
        }
        if (!keyWeaknesses.isEmpty()) {
            return "该节点与当前薄弱点「" + keyWeaknesses.get(0) + "」关联度较高。";
        }
        return "该节点位于当前路径前段，适合优先推进。";
    }

    private void addValues(LinkedHashSet<String> collector, List<String> values, int limit) {
        if (values == null || values.isEmpty()) {
            return;
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            collector.add(value.trim());
            if (collector.size() >= limit) {
                break;
            }
        }
    }

    private String fallbackText(String text, String fallback) {
        return text == null || text.isBlank() ? fallback : text.trim();
    }

    public record PlanExplanation(
        String whyStartHere,
        List<String> keyWeaknesses,
        List<PlanPriorityNodeResponse> priorityNodes
    ) {
        public PlanExplanation {
            whyStartHere = (whyStartHere == null || whyStartHere.isBlank())
                ? "系统将从当前最稳妥的起点开始，逐步补齐关键薄弱点。"
                : whyStartHere.trim();
            keyWeaknesses = keyWeaknesses == null || keyWeaknesses.isEmpty()
                ? List.of("关键薄弱点待进一步识别")
                : keyWeaknesses;
            priorityNodes = priorityNodes == null || priorityNodes.isEmpty()
                ? List.of(new PlanPriorityNodeResponse("start-node", "当前推荐起点", "这是最稳妥的起始节点。"))
                : priorityNodes;
        }
    }
}
