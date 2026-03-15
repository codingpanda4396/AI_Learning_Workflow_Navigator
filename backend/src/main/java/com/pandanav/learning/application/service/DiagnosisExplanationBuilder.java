package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.diagnosis.DiagnosisExplanationDto;
import com.pandanav.learning.domain.model.DiagnosisLearnerProfileSnapshot;
import com.pandanav.learning.domain.model.DiagnosisStrategyDecision;
import com.pandanav.learning.domain.model.DiagnosisQuestion;
import com.pandanav.learning.domain.model.PlanningContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DiagnosisExplanationBuilder {

    public DiagnosisExplanationDto build(
        PlanningContext context,
        DiagnosisLearnerProfileSnapshot profileSnapshot,
        DiagnosisStrategyDecision strategyDecision,
        List<DiagnosisQuestion> selectedQuestions
    ) {
        String topic = resolveTopic(context);
        String partA = buildWhatSystemSaw(profileSnapshot, topic);
        String partB = buildWhyTheseQuestions(strategyDecision, topic);
        String partC = buildHowAnswersAffectPlanning(strategyDecision, profileSnapshot);

        String whyTheseQuestions = partA + " " + partB;
        List<String> whatWillBeInferred = inferFromStrategy(strategyDecision);
        return new DiagnosisExplanationDto(whyTheseQuestions, whatWillBeInferred, partC);
    }

    private String buildWhatSystemSaw(DiagnosisLearnerProfileSnapshot profile, String topic) {
        List<String> parts = new ArrayList<>();
        if (profile.evidence() != null && !profile.evidence().isEmpty()) {
            parts.add("根据你当前填写的目标与主题，系统会据此确认学习起点。");
        } else {
            parts.add("当前学习数据较少，系统会通过以下问题先确认你的起点。");
        }
        if ("HIGH".equals(profile.goalClarity())) {
            parts.add("你的目标比较明确，我们会重点确认前置基础与投入边界是否匹配。");
        } else if ("LOW".equals(profile.goalClarity())) {
            parts.add("系统会先了解你的大致目标与基础，再推荐合适路径。");
        }
        if (profile.hasContradictionRisk()) {
            parts.add("检测到可能与历史表现不一致的情况，本次会优先核实关键维度。");
        }
        return String.join(" ", parts);
    }

    private String buildWhyTheseQuestions(DiagnosisStrategyDecision strategy, String topic) {
        if (strategy.personalizationReasons() != null && !strategy.personalizationReasons().isEmpty()) {
            return strategy.personalizationReasons().get(0);
        }
        if (topic != null && !topic.isBlank()) {
            return "本次围绕「" + topic + "」重点确认前置基础、时间投入与学习目标。";
        }
        return "本次优先确认前置基础与学习目标，再决定后续规划。";
    }

    private String buildHowAnswersAffectPlanning(DiagnosisStrategyDecision strategy, DiagnosisLearnerProfileSnapshot profile) {
        List<String> lines = new ArrayList<>();
        lines.add("若基础较稳，后续会直接进入核心知识点；若时间受限，规划会采用轻量推进；若目标与基础不匹配，会优先补前置再进主线。");
        return String.join(" ", lines);
    }

    private List<String> inferFromStrategy(DiagnosisStrategyDecision strategy) {
        List<String> out = new ArrayList<>();
        if (strategy.priorityDimensions() != null) {
            for (String dim : strategy.priorityDimensions()) {
                switch (dim) {
                    case "FOUNDATION" -> out.add("当前学习起点");
                    case "TIME_BUDGET" -> out.add("更适合的学习节奏");
                    case "GOAL_STYLE" -> out.add("任务组织方式与目标匹配");
                    case "LEARNING_PREFERENCE" -> out.add("学习偏好与反馈方式");
                    case "EXPERIENCE" -> out.add("过往经验与迁移能力");
                    default -> out.add(dim);
                }
            }
        }
        if (out.isEmpty()) {
            out.add("当前学习起点");
            out.add("更适合的学习节奏");
            out.add("任务组织方式");
        }
        return out;
    }

    private String resolveTopic(PlanningContext context) {
        if (context == null) {
            return "";
        }
        if (context.topicName() != null && !context.topicName().isBlank()) {
            return context.topicName().trim();
        }
        if (context.chapterName() != null && !context.chapterName().isBlank()) {
            return context.chapterName().trim();
        }
        return context.learningGoal() == null ? "" : context.learningGoal().trim();
    }
}
