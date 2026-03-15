package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.enums.DiagnosisDimension;
import com.pandanav.learning.domain.model.DiagnosisQuestion;
import com.pandanav.learning.domain.model.DiagnosisQuestionCandidate;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.PlanningContext;
import com.pandanav.learning.domain.service.DiagnosisTemplateFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builds a candidate pool of diagnosis questions with selection metadata.
 * Delegates question structure to DiagnosisTemplateFactory.
 */
@Component
public class DiagnosisQuestionCandidateFactory {

    private final DiagnosisTemplateFactory diagnosisTemplateFactory;

    public DiagnosisQuestionCandidateFactory(DiagnosisTemplateFactory diagnosisTemplateFactory) {
        this.diagnosisTemplateFactory = diagnosisTemplateFactory;
    }

    public List<DiagnosisQuestionCandidate> buildCandidates(LearningSession session, PlanningContext planningContext) {
        List<DiagnosisQuestion> baseQuestions = diagnosisTemplateFactory.buildQuestions(session);
        return baseQuestions.stream()
            .map(q -> toCandidate(q))
            .toList();
    }

    private DiagnosisQuestionCandidate toCandidate(DiagnosisQuestion q) {
        String intentCode = intentCodeFor(q.dimension());
        int priorityBaseScore = baseScoreFor(q.dimension());
        List<String> applicableStages = List.of("BEGINNER", "BASIC", "ADVANCED_HINT");
        List<String> triggerSignals = triggerSignalsFor(q.dimension());
        List<String> suppressSignals = suppressSignalsFor(q.dimension());
        return new DiagnosisQuestionCandidate(
            q.questionId(),
            q.dimension(),
            intentCode,
            priorityBaseScore,
            applicableStages,
            triggerSignals,
            suppressSignals,
            q
        );
    }

    private static String intentCodeFor(DiagnosisDimension d) {
        return switch (d) {
            case FOUNDATION -> "FOUNDATION_CHECK";
            case EXPERIENCE -> "EXPERIENCE_CHECK";
            case GOAL_STYLE -> "GOAL_STYLE_CHECK";
            case TIME_BUDGET -> "TIME_BUDGET_CHECK";
            case LEARNING_PREFERENCE -> "LEARNING_PREFERENCE_CHECK";
            case DIFFICULTY_PAIN_POINT -> "PAIN_POINT_CHECK";
        };
    }

    private static int baseScoreFor(DiagnosisDimension d) {
        return switch (d) {
            case FOUNDATION -> 10;
            case GOAL_STYLE -> 9;
            case TIME_BUDGET -> 9;
            case EXPERIENCE -> 7;
            case LEARNING_PREFERENCE -> 6;
            case DIFFICULTY_PAIN_POINT -> 8;
        };
    }

    private static List<String> triggerSignalsFor(DiagnosisDimension d) {
        return switch (d) {
            case FOUNDATION -> List.of("NO_FOUNDATION", "FOUNDATION_GAP_RISK");
            case GOAL_STYLE -> List.of("GOAL_UNCLEAR", "GOAL_AMBITION_MISMATCH");
            case TIME_BUDGET -> List.of("TIME_LIMITED", "INTENSITY_MISMATCH");
            case EXPERIENCE -> List.of("NO_EXPERIENCE", "OVERCONFIDENT");
            case LEARNING_PREFERENCE -> List.of("PREFERENCE_UNKNOWN");
            case DIFFICULTY_PAIN_POINT -> List.of("PAIN_POINT_UNKNOWN");
        };
    }

    private static List<String> suppressSignalsFor(DiagnosisDimension d) {
        return switch (d) {
            case FOUNDATION -> List.of();
            case GOAL_STYLE -> List.of();
            case TIME_BUDGET -> List.of();
            case EXPERIENCE -> List.of("TIME_LIMITED");
            case LEARNING_PREFERENCE -> List.of("TIME_LIMITED");
            case DIFFICULTY_PAIN_POINT -> List.of("TIME_LIMITED");
        };
    }
}
