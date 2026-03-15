package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.enums.DiagnosisDimension;
import com.pandanav.learning.domain.model.DiagnosisQuestion;
import com.pandanav.learning.domain.model.DiagnosisQuestionCopy;
import com.pandanav.learning.domain.model.DiagnosisSignal;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.service.DiagnosisQuestionCopyFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class DiagnosisQuestionAssembler {

    private final DiagnosisQuestionCopyFactory diagnosisQuestionCopyFactory;

    public DiagnosisQuestionAssembler(DiagnosisQuestionCopyFactory diagnosisQuestionCopyFactory) {
        this.diagnosisQuestionCopyFactory = diagnosisQuestionCopyFactory;
    }

    public List<DiagnosisQuestion> assemble(LearningSession session, List<DiagnosisQuestion> baseQuestions) {
        List<DiagnosisQuestion> questions = new ArrayList<>(baseQuestions == null ? List.of() : baseQuestions);
        if (!shouldInsertPainPointQuestion(session)) {
            return questions;
        }

        DiagnosisQuestion painPointQuestion = createQuestion(session, "q_difficulty_pain_point", DiagnosisDimension.DIFFICULTY_PAIN_POINT, "single_choice");
        int replaceIndex = indexOfDimension(questions, DiagnosisDimension.LEARNING_PREFERENCE);
        if (replaceIndex >= 0) {
            questions.set(replaceIndex, painPointQuestion);
            return questions;
        }
        int goalStyleIndex = indexOfDimension(questions, DiagnosisDimension.GOAL_STYLE);
        if (goalStyleIndex >= 0 && goalStyleIndex + 1 <= questions.size()) {
            questions.add(goalStyleIndex + 1, painPointQuestion);
        } else {
            questions.add(painPointQuestion);
        }
        return questions;
    }

    private DiagnosisQuestion createQuestion(
        LearningSession session,
        String questionId,
        DiagnosisDimension dimension,
        String type
    ) {
        DiagnosisQuestionCopy copy = diagnosisQuestionCopyFactory.build(session, dimension, type);
        return new DiagnosisQuestion(
            questionId,
            dimension,
            type,
            true,
            com.pandanav.learning.api.contract.ContractCatalog.diagnosisQuestionOptions(dimension),
            copy.title(),
            copy.description(),
            copy.placeholder(),
            copy.submitHint(),
            copy.sectionLabel(),
            signalTargets(dimension),
            optionSignalMapping(dimension)
        );
    }

    private List<String> signalTargets(DiagnosisDimension dimension) {
        if (dimension != DiagnosisDimension.DIFFICULTY_PAIN_POINT) {
            return List.of();
        }
        return List.of("difficulty_pain_point", "support_priority");
    }

    private Map<String, List<DiagnosisSignal>> optionSignalMapping(DiagnosisDimension dimension) {
        if (dimension != DiagnosisDimension.DIFFICULTY_PAIN_POINT) {
            return Map.of();
        }
        return Map.of(
            "CONCEPT_UNDERSTANDING", List.of(
                new DiagnosisSignal("difficulty_pain_point", "CONCEPT_UNDERSTANDING", 0.9, 0.95, "pain_point"),
                new DiagnosisSignal("support_priority", "UNDERSTANDING", 0.7, 0.88, "pain_point")
            ),
            "TRANSFER_APPLICATION", List.of(
                new DiagnosisSignal("difficulty_pain_point", "TRANSFER_APPLICATION", 0.9, 0.95, "pain_point"),
                new DiagnosisSignal("support_priority", "TRAINING_VARIANTS", 0.7, 0.88, "pain_point")
            ),
            "IMPLEMENTATION", List.of(
                new DiagnosisSignal("difficulty_pain_point", "IMPLEMENTATION", 0.9, 0.95, "pain_point"),
                new DiagnosisSignal("support_priority", "GUIDED_PRACTICE", 0.7, 0.88, "pain_point")
            ),
            "LONG_TERM_MEMORY", List.of(
                new DiagnosisSignal("difficulty_pain_point", "LONG_TERM_MEMORY", 0.9, 0.95, "pain_point"),
                new DiagnosisSignal("support_priority", "SPACED_REVIEW", 0.7, 0.88, "pain_point")
            )
        );
    }

    private boolean shouldInsertPainPointQuestion(LearningSession session) {
        String goal = session == null || session.getGoalText() == null ? "" : session.getGoalText().trim().toLowerCase(Locale.ROOT);
        if (goal.isBlank()) {
            return false;
        }
        return goal.contains("exam")
            || goal.contains("interview")
            || goal.contains("考试")
            || goal.contains("面试");
    }

    private int indexOfDimension(List<DiagnosisQuestion> questions, DiagnosisDimension dimension) {
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i).dimension() == dimension) {
                return i;
            }
        }
        return -1;
    }
}
