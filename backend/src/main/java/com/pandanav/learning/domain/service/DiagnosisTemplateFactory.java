package com.pandanav.learning.domain.service;

import com.pandanav.learning.api.contract.ContractCatalog;
import com.pandanav.learning.domain.enums.DiagnosisDimension;
import com.pandanav.learning.domain.model.DiagnosisQuestion;
import com.pandanav.learning.domain.model.DiagnosisQuestionCopy;
import com.pandanav.learning.domain.model.DiagnosisSignal;
import com.pandanav.learning.domain.model.LearningSession;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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
            copy.sectionLabel(),
            signalTargets(dimension),
            optionSignalMapping(dimension)
        );
    }

    private List<String> signalTargets(DiagnosisDimension dimension) {
        return switch (dimension) {
            case FOUNDATION -> List.of("foundation_level", "review_depth");
            case EXPERIENCE -> List.of("practice_experience", "transfer_experience");
            case GOAL_STYLE -> List.of("goal_orientation", "assessment_pressure");
            case TIME_BUDGET -> List.of("time_budget", "learning_intensity");
            case LEARNING_PREFERENCE -> List.of("learning_preference", "feedback_style");
            case DIFFICULTY_PAIN_POINT -> List.of("difficulty_pain_point", "support_priority");
        };
    }

    private Map<String, List<DiagnosisSignal>> optionSignalMapping(DiagnosisDimension dimension) {
        return switch (dimension) {
            case FOUNDATION -> Map.of(
                "BEGINNER", List.of(
                    signal("foundation_level", "BEGINNER", -1.0, 0.95, "self_assessment"),
                    signal("review_depth", "HIGH", 0.8, 0.80, "self_assessment")
                ),
                "BASIC", List.of(
                    signal("foundation_level", "BASIC", -0.4, 0.90, "self_assessment"),
                    signal("review_depth", "MEDIUM", 0.5, 0.75, "self_assessment")
                ),
                "PROFICIENT", List.of(
                    signal("foundation_level", "PROFICIENT", 0.6, 0.90, "self_assessment"),
                    signal("review_depth", "LOW", -0.3, 0.70, "self_assessment")
                ),
                "ADVANCED", List.of(
                    signal("foundation_level", "ADVANCED", 1.0, 0.95, "self_assessment"),
                    signal("review_depth", "LOW", -0.6, 0.70, "self_assessment")
                )
            );
            case EXPERIENCE -> Map.of(
                "COURSEWORK", List.of(signal("practice_experience", "COURSEWORK", 0.3, 0.80, "history")),
                "ASSIGNMENTS", List.of(signal("practice_experience", "ASSIGNMENTS", 0.5, 0.82, "history")),
                "PROJECTS", List.of(
                    signal("practice_experience", "PROJECTS", 0.8, 0.88, "history"),
                    signal("transfer_experience", "HIGH", 0.6, 0.82, "history")
                ),
                "EXAM_PREP", List.of(signal("assessment_pressure", "HIGH", 0.7, 0.85, "history")),
                "NO_EXPERIENCE", List.of(signal("practice_experience", "NONE", -0.8, 0.92, "history"))
            );
            case GOAL_STYLE -> Map.of(
                "COURSE", List.of(signal("goal_orientation", "COURSE", 0.5, 0.90, "goal")),
                "EXAM", List.of(
                    signal("goal_orientation", "EXAM", 0.8, 0.92, "goal"),
                    signal("assessment_pressure", "HIGH", 0.6, 0.85, "goal")
                ),
                "INTERVIEW", List.of(
                    signal("goal_orientation", "INTERVIEW", 0.8, 0.92, "goal"),
                    signal("transfer_experience", "HIGH", 0.5, 0.80, "goal")
                ),
                "PROJECT", List.of(signal("goal_orientation", "PROJECT", 0.7, 0.90, "goal"))
            );
            case TIME_BUDGET -> Map.of(
                "LIGHT", List.of(
                    signal("time_budget", "LIGHT", -0.8, 0.95, "constraint"),
                    signal("learning_intensity", "LIGHT", -0.6, 0.88, "constraint")
                ),
                "STANDARD", List.of(
                    signal("time_budget", "STANDARD", 0.2, 0.90, "constraint"),
                    signal("learning_intensity", "STANDARD", 0.2, 0.86, "constraint")
                ),
                "INTENSIVE", List.of(
                    signal("time_budget", "INTENSIVE", 0.7, 0.92, "constraint"),
                    signal("learning_intensity", "INTENSIVE", 0.7, 0.88, "constraint")
                ),
                "IMMERSIVE", List.of(
                    signal("time_budget", "IMMERSIVE", 1.0, 0.95, "constraint"),
                    signal("learning_intensity", "IMMERSIVE", 1.0, 0.90, "constraint")
                )
            );
            case LEARNING_PREFERENCE -> Map.of(
                "CONCEPT_FIRST", List.of(signal("learning_preference", "CONCEPT_FIRST", 0.7, 0.90, "preference")),
                "EXAMPLE_FIRST", List.of(signal("learning_preference", "EXAMPLE_FIRST", 0.7, 0.90, "preference")),
                "PRACTICE_FIRST", List.of(
                    signal("learning_preference", "PRACTICE_FIRST", 0.8, 0.92, "preference"),
                    signal("feedback_style", "IMMEDIATE", 0.5, 0.80, "preference")
                ),
                "PROJECT_DRIVEN", List.of(
                    signal("learning_preference", "PROJECT_DRIVEN", 0.8, 0.92, "preference"),
                    signal("feedback_style", "MILESTONE", 0.4, 0.78, "preference")
                )
            );
            case DIFFICULTY_PAIN_POINT -> Map.of(
                "CONCEPT_UNDERSTANDING", List.of(
                    signal("difficulty_pain_point", "CONCEPT_UNDERSTANDING", 0.9, 0.95, "pain_point"),
                    signal("support_priority", "UNDERSTANDING", 0.7, 0.88, "pain_point")
                ),
                "TRANSFER_APPLICATION", List.of(
                    signal("difficulty_pain_point", "TRANSFER_APPLICATION", 0.9, 0.95, "pain_point"),
                    signal("support_priority", "TRAINING_VARIANTS", 0.7, 0.88, "pain_point")
                ),
                "IMPLEMENTATION", List.of(
                    signal("difficulty_pain_point", "IMPLEMENTATION", 0.9, 0.95, "pain_point"),
                    signal("support_priority", "GUIDED_PRACTICE", 0.7, 0.88, "pain_point")
                ),
                "LONG_TERM_MEMORY", List.of(
                    signal("difficulty_pain_point", "LONG_TERM_MEMORY", 0.9, 0.95, "pain_point"),
                    signal("support_priority", "SPACED_REVIEW", 0.7, 0.88, "pain_point")
                )
            );
        };
    }

    private DiagnosisSignal signal(String featureKey, String featureValue, double scoreDelta, double confidence, String evidence) {
        return new DiagnosisSignal(featureKey, featureValue, scoreDelta, confidence, evidence);
    }
}
