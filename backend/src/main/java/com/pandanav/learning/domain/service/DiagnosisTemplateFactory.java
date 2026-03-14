package com.pandanav.learning.domain.service;

import com.pandanav.learning.api.contract.ContractCatalog;
import com.pandanav.learning.domain.enums.DiagnosisDimension;
import com.pandanav.learning.domain.model.DiagnosisQuestion;
import com.pandanav.learning.domain.model.DiagnosisQuestionCopy;
import com.pandanav.learning.domain.model.LearningSession;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DiagnosisTemplateFactory {

    private final DiagnosisQuestionCopyFactory diagnosisQuestionCopyFactory;

    public DiagnosisTemplateFactory(DiagnosisQuestionCopyFactory diagnosisQuestionCopyFactory) {
        this.diagnosisQuestionCopyFactory = diagnosisQuestionCopyFactory;
    }

    public List<DiagnosisQuestion> buildQuestions(LearningSession session) {
        return List.of(
            createQuestion(session, "q_foundation", DiagnosisDimension.FOUNDATION, "single_choice"),
            createQuestion(session, "q_experience", DiagnosisDimension.EXPERIENCE, "multiple_choice"),
            createQuestion(session, "q_goal_style", DiagnosisDimension.GOAL_STYLE, "single_choice"),
            createQuestion(session, "q_time_budget", DiagnosisDimension.TIME_BUDGET, "single_choice"),
            createQuestion(session, "q_learning_preference", DiagnosisDimension.LEARNING_PREFERENCE, "single_choice")
        );
    }

    private DiagnosisQuestion createQuestion(LearningSession session, String questionId, DiagnosisDimension dimension, String type) {
        DiagnosisQuestionCopy copy = diagnosisQuestionCopyFactory.build(session, dimension, type);
        return new DiagnosisQuestion(
            questionId,
            dimension,
            type,
            true,
            ContractCatalog.diagnosisQuestionOptions(dimension),
            copy.title(),
            copy.description(),
            copy.placeholder(),
            copy.submitHint(),
            copy.sectionLabel()
        );
    }
}
