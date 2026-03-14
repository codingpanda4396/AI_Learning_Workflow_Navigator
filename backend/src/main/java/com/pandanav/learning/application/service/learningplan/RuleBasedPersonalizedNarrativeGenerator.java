package com.pandanav.learning.application.service.learningplan;

import com.pandanav.learning.domain.enums.CurrentBlockType;
import com.pandanav.learning.domain.enums.GoalOrientation;
import com.pandanav.learning.domain.model.LearnerStateSnapshot;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.LearningPlanPreview;
import com.pandanav.learning.domain.model.PersonalizedNarrative;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RuleBasedPersonalizedNarrativeGenerator implements PersonalizedNarrativeGenerator {

    @Override
    public PersonalizedNarrative generate(
        LearningPlanPlanningContext context,
        LearnerStateSnapshot learnerStateSnapshot,
        DecisionPlan decisionPlan,
        LearningPlanPreview preview
    ) {
        List<String> whatISaw = new ArrayList<>();
        whatISaw.add(resolveGoalObservation(learnerStateSnapshot.goalOrientation()));
        whatISaw.add(resolveEvidenceObservation(learnerStateSnapshot));
        whatISaw.add("当前推荐节点是 " + safe(preview.summary().recommendedStartNodeName()) + "，它直接连接后续路径。");
        if (context.weakPointLabels() != null && !context.weakPointLabels().isEmpty()) {
            whatISaw.add("你最近的薄弱点集中在 " + String.join("、", context.weakPointLabels().stream().limit(2).toList()) + "。");
        }

        String learnerState = learnerStateSnapshot.primaryBlockDescription();
        String whyFits = "这次计划先用 " + safe(preview.summary().recommendedPace()) + " 节奏处理 "
            + safe(preview.summary().recommendedStartNodeName()) + "，目的是先解决最影响后续推进的一段阻塞。";
        String riskIfSkip = decisionPlan.riskAssessment() != null && !decisionPlan.riskAssessment().isBlank()
            ? decisionPlan.riskAssessment()
            : "如果跳过当前步骤，后续训练更可能出现理解断层和重复返工。";
        String boundary = resolveBoundary(learnerStateSnapshot.currentBlockType());
        String adaptationHint = "完成本轮后，系统会根据你的新得分和错因标签，动态决定是继续推进到下一节点还是先做一次回补。";

        return new PersonalizedNarrative(
            learnerState,
            whatISaw.stream().limit(4).toList(),
            whyFits,
            riskIfSkip,
            boundary,
            adaptationHint
        );
    }

    private String resolveGoalObservation(GoalOrientation goalOrientation) {
        return switch (goalOrientation) {
            case EXAM_PREP -> "你的目标更偏向应试推进，系统会优先给出可执行且可验证的步骤。";
            case QUICK_START -> "你的目标更偏向快速上手，系统会优先保留高杠杆动作。";
            case REVIEW_FIX -> "你的目标更偏向查漏补缺，系统会先收敛最可能复发的问题。";
            case UNDERSTAND_PRINCIPLE -> "你的目标更偏向理解原理，系统会强调关键概念之间的连接。";
        };
    }

    private String resolveEvidenceObservation(LearnerStateSnapshot learnerStateSnapshot) {
        return "当前证据强度为 " + learnerStateSnapshot.evidenceLevel()
            + "，" + learnerStateSnapshot.confidenceReasonSummary();
    }

    private String resolveBoundary(CurrentBlockType currentBlockType) {
        return switch (currentBlockType) {
            case FOUNDATION_GAP -> "本轮不追求覆盖更多新内容，先追求把前置基础稳定到可继续推进。";
            case CONCEPT_LINK_GAP -> "本轮不追求题量堆叠，先追求把关键概念连接讲清并验证。";
            case APPLICATION_GAP -> "本轮不追求理论扩展，先追求把已学概念稳定应用到训练中。";
            case MIXED -> "本轮不追求全面展开，先追求用最小步骤同时降低基础和应用风险。";
            case EVIDENCE_LOW -> "本轮不追求复杂个性化策略，先追求低风险起步并补齐行为证据。";
        };
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "当前关键节点" : value.trim();
    }
}
