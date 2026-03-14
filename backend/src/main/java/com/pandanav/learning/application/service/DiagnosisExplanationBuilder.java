package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.diagnosis.DiagnosisExplanationDto;
import com.pandanav.learning.domain.model.PlanningContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DiagnosisExplanationBuilder {

    public DiagnosisExplanationDto build(PlanningContext context) {
        String topic = resolveTopic(context);
        String why = topic.isBlank()
            ? "系统需要了解你的知识基础、过往经验、学习目标和时间节奏，从而制定更合适的学习路径。"
            : "系统需要了解你在「%s」上的知识基础、过往经验、学习目标和时间节奏，从而制定更合适的学习路径。".formatted(topic);
        return new DiagnosisExplanationDto(
            why,
            List.of("当前学习起点", "更适合的学习节奏", "任务组织方式"),
            "这些信息将用于生成个性化学习路径，并决定任务难度和学习节奏。"
        );
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
