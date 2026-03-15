package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.model.DiagnosisQuestion;
import com.pandanav.learning.domain.model.DiagnosisSignal;
import com.pandanav.learning.domain.model.LearnerFeatureSignal;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class LearnerFeatureExtractor {

    public List<LearnerFeatureSignal> extract(
        Long learningSessionId,
        Long userId,
        List<DiagnosisQuestion> questions,
        List<DiagnosisAnswerNormalizer.NormalizedDiagnosisAnswer> normalizedAnswers
    ) {
        Map<String, DiagnosisQuestion> questionById = new LinkedHashMap<>();
        for (DiagnosisQuestion question : questions) {
            questionById.put(question.questionId(), question);
        }

        List<LearnerFeatureSignal> signals = new ArrayList<>();
        for (DiagnosisAnswerNormalizer.NormalizedDiagnosisAnswer answer : normalizedAnswers) {
            DiagnosisQuestion question = questionById.get(answer.questionId());
            if (question == null) {
                continue;
            }
            for (String optionCode : answer.selectedOptionCodes()) {
                List<DiagnosisSignal> mappedSignals = question.optionSignalMapping().getOrDefault(optionCode, List.of());
                for (DiagnosisSignal mappedSignal : mappedSignals) {
                    signals.add(toFeatureSignal(answer, question, learningSessionId, userId, optionCode, mappedSignal));
                }
            }
            if ("TEXT".equals(answer.answerType()) && answer.textAnswer() != null && !answer.textAnswer().isBlank()) {
                for (String target : question.signalTargets()) {
                    signals.add(toTextSignal(answer, question, learningSessionId, userId, target));
                }
            }
        }
        return signals;
    }

    private LearnerFeatureSignal toFeatureSignal(
        DiagnosisAnswerNormalizer.NormalizedDiagnosisAnswer answer,
        DiagnosisQuestion question,
        Long learningSessionId,
        Long userId,
        String optionCode,
        DiagnosisSignal mappedSignal
    ) {
        LearnerFeatureSignal signal = new LearnerFeatureSignal();
        signal.setDiagnosisSessionId(answer.diagnosisSessionId());
        signal.setLearningSessionId(learningSessionId);
        signal.setUserId(userId);
        signal.setQuestionId(question.questionId());
        signal.setFeatureKey(mappedSignal.featureKey());
        signal.setFeatureValue(mappedSignal.featureValue());
        signal.setScoreDelta(mappedSignal.scoreDelta());
        signal.setConfidence(mappedSignal.confidence());
        signal.setSource("RULE");
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("dimension", question.dimension().name());
        evidence.put("questionTitle", safe(question.title()));
        evidence.put("selectedOptionCode", optionCode);
        evidence.put("reason", safe(mappedSignal.evidence()));
        signal.setEvidence(evidence);
        return signal;
    }

    private LearnerFeatureSignal toTextSignal(
        DiagnosisAnswerNormalizer.NormalizedDiagnosisAnswer answer,
        DiagnosisQuestion question,
        Long learningSessionId,
        Long userId,
        String signalTarget
    ) {
        LearnerFeatureSignal signal = new LearnerFeatureSignal();
        signal.setDiagnosisSessionId(answer.diagnosisSessionId());
        signal.setLearningSessionId(learningSessionId);
        signal.setUserId(userId);
        signal.setQuestionId(question.questionId());
        signal.setFeatureKey(signalTarget);
        signal.setFeatureValue("TEXT_PROVIDED");
        signal.setScoreDelta(0.2);
        signal.setConfidence(0.55);
        signal.setSource("RULE_TEXT");
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("dimension", question.dimension().name());
        evidence.put("questionTitle", safe(question.title()));
        evidence.put("text", safe(answer.textAnswer()));
        signal.setEvidence(evidence);
        return signal;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
