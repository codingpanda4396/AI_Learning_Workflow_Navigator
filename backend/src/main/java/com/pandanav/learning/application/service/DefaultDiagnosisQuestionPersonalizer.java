package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.model.DiagnosisQuestion;
import com.pandanav.learning.domain.model.PlanningContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultDiagnosisQuestionPersonalizer implements DiagnosisQuestionPersonalizer {

    @Override
    public List<DiagnosisQuestion> personalize(List<DiagnosisQuestion> baseQuestions, PlanningContext context) {
        if (baseQuestions == null || baseQuestions.isEmpty() || context == null) {
            return baseQuestions == null ? List.of() : baseQuestions;
        }
        String topic = resolveTopic(context);
        if (topic.isBlank()) {
            return baseQuestions;
        }
        return baseQuestions.stream()
            .map(question -> new DiagnosisQuestion(
                question.questionId(),
                question.dimension(),
                question.type(),
                question.required(),
                question.options(),
                personalizeCopy(question.title(), topic),
                personalizeCopy(question.description(), topic),
                question.placeholder(),
                question.submitHint(),
                question.sectionLabel(),
                question.signalTargets(),
                question.optionSignalMapping()
            ))
            .toList();
    }

    private String personalizeCopy(String copy, String topic) {
        if (copy == null || copy.isBlank()) {
            return copy == null ? "" : copy;
        }
        String candidate = copy.trim();
        if (candidate.contains("{topic}")) {
            return candidate.replace("{topic}", topic);
        }
        if (candidate.contains("这部分内容")) {
            return candidate.replace("这部分内容", "「%s」相关内容".formatted(topic));
        }
        if (candidate.contains("相关")) {
            return candidate;
        }
        return candidate + "（聚焦「%s」）".formatted(topic);
    }

    private String resolveTopic(PlanningContext context) {
        if (context.topicName() != null && !context.topicName().isBlank()) {
            return context.topicName().trim();
        }
        if (context.chapterName() != null && !context.chapterName().isBlank()) {
            return context.chapterName().trim();
        }
        if (context.learningGoal() != null && !context.learningGoal().isBlank()) {
            return context.learningGoal().trim();
        }
        return "";
    }
}
