package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.model.DiagnosisQuestion;
import com.pandanav.learning.domain.model.DiagnosisQuestionDraft;
import com.pandanav.learning.domain.model.DiagnosisLearnerProfileSnapshot;
import com.pandanav.learning.domain.model.DiagnosisStrategyDecision;
import com.pandanav.learning.domain.model.PlanningContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapts selected question copy by topic and context; does not change which questions are asked.
 * q_topic_focus keeps principle/code options from the factory; only title is personalized.
 */
@Component
public class DefaultDiagnosisQuestionCopyAdapter implements DiagnosisQuestionCopyAdapter {

    public DefaultDiagnosisQuestionCopyAdapter() {
    }

    @Override
    public List<DiagnosisQuestion> adapt(
        List<DiagnosisQuestionDraft> selectedDrafts,
        PlanningContext planningContext,
        DiagnosisLearnerProfileSnapshot profileSnapshot,
        DiagnosisStrategyDecision strategyDecision
    ) {
        if (selectedDrafts == null || selectedDrafts.isEmpty()) {
            return List.of();
        }
        String topic = resolveTopic(planningContext);
        String goal = planningContext != null ? planningContext.learningGoal() : null;
        return selectedDrafts.stream()
            .map(DiagnosisQuestionDraft::question)
            .map(q -> adaptQuestion(q, topic, goal))
            .toList();
    }

    private DiagnosisQuestion adaptQuestion(DiagnosisQuestion q, String topic, String goal) {
        if (topic == null || topic.isBlank()) {
            return q;
        }
        if ("q_topic_focus".equals(q.questionId())) {
            return new DiagnosisQuestion(
                q.questionId(),
                q.dimension(),
                q.type(),
                q.required(),
                q.options(),
                personalizeCopy(q.title(), topic),
                personalizeCopy(q.description(), topic),
                q.placeholder(),
                q.submitHint(),
                q.sectionLabel(),
                q.signalTargets(),
                q.optionSignalMapping()
            );
        }
        return new DiagnosisQuestion(
            q.questionId(),
            q.dimension(),
            q.type(),
            q.required(),
            q.options(),
            personalizeCopy(q.title(), topic),
            personalizeCopy(q.description(), topic),
            q.placeholder(),
            q.submitHint(),
            q.sectionLabel(),
            q.signalTargets(),
            q.optionSignalMapping()
        );
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
        if (context == null) return "";
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
